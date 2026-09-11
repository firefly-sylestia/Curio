-- ═══════════════════════════════════════════════════════════════════════════
-- Curio — online schema (account, text-only capture sync, 24-hour community)
--
-- HOW TO APPLY: Supabase dashboard → SQL Editor → New query → paste this whole
-- file → Run. The script is IDEMPOTENT (create … if not exists / drop policy
-- if exists first), so re-running it after an edit is safe.
--
-- SECURITY MODEL (read before changing anything here):
--  • Row Level Security is ON for every table. The Android app ships only the
--    public URL + publishable/anon key, so RLS *is* the security boundary —
--    never disable it, and never add a policy for the `anon` role.
--  • Community reads and writes additionally require the AUTHOR/READER to have
--    Online Mode enabled in their own profile row.
--  • Community cards expire 24 hours after they are posted and are filtered
--    out of every read by `expires_at > now()`.
--  • Nothing media-backed is ever allowed to sync: the tables only carry text.
-- ═══════════════════════════════════════════════════════════════════════════

create extension if not exists pgcrypto;

-- ───────────────────────────────────────────────────────────────────────────
-- 1. profiles — one row per account, holds the account's Online Mode switch
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.profiles (
    id                  uuid primary key references auth.users (id) on delete cascade,
    display_name        text,
    online_mode_enabled boolean not null default false,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

alter table public.profiles enable row level security;

-- A user sees only their own profile (never anyone else's), and may create
-- and update it. No delete policy: the row follows the auth user.
drop policy if exists prof_select_own on public.profiles;
create policy prof_select_own on public.profiles
    for select to authenticated
    using (id = auth.uid());

drop policy if exists prof_insert_own on public.profiles;
create policy prof_insert_own on public.profiles
    for insert to authenticated
    with check (id = auth.uid());

drop policy if exists prof_update_own on public.profiles;
create policy prof_update_own on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 2. cloud_captures — text-only mirror of Room captures (owner-private)
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.cloud_captures (
    id             text primary key,           -- the stable Room capture id
    owner          uuid not null default auth.uid() references auth.users (id) on delete cascade,
    topic_name     text not null default '',
    category_slug  text not null default '',
    capture_format text not null default '',
    title          text not null default '',
    body_text      text not null default '',   -- serialized TEXT content only
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    deleted_at     timestamptz
);

create index if not exists cloud_captures_owner_idx
    on public.cloud_captures (owner, updated_at desc);
create index if not exists cloud_captures_live_idx
    on public.cloud_captures (owner) where deleted_at is null;

alter table public.cloud_captures enable row level security;

-- Strictly owner-scoped: a capture is readable and writable only by its owner.
drop policy if exists cc_select_own on public.cloud_captures;
create policy cc_select_own on public.cloud_captures
    for select to authenticated
    using (owner = auth.uid());

drop policy if exists cc_insert_own on public.cloud_captures;
create policy cc_insert_own on public.cloud_captures
    for insert to authenticated
    with check (owner = auth.uid());

drop policy if exists cc_update_own on public.cloud_captures;
create policy cc_update_own on public.cloud_captures
    for update to authenticated
    using (owner = auth.uid())
    with check (owner = auth.uid());

drop policy if exists cc_delete_own on public.cloud_captures;
create policy cc_delete_own on public.cloud_captures
    for delete to authenticated
    using (owner = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 3. community_cards — TEMPORARY text share cards (24 hours)
--    No media columns exist by design: a card is text + topic + style data,
--    which is exactly what the Android share-card renderer needs.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_cards (
    id             uuid primary key default gen_random_uuid(),
    -- `default auth.uid()` means the app never sends an identity column: it
    -- cannot even attempt to post as somebody else (the policy would refuse,
    -- and there is nothing to spoof).
    owner          uuid not null default auth.uid() references auth.users (id) on delete cascade,
    author_handle  text not null default 'A curious soul',
    topic_name     text not null,
    category_slug  text not null default '',
    category_name  text not null default '',
    category_glyph text not null default '',
    accent_hex     text not null default '#8E8E93',
    fact_text      text not null,
    caption        text not null default '',   -- the poster's own line above the card
    style          text not null default 'PAPER',
    aspect         text not null default 'CLASSIC',
    body_scale     real not null default 1.0,
    byline         text not null default '',
    created_at     timestamptz not null default now(),
    expires_at     timestamptz not null default (now() + interval '24 hours'),
    constraint community_cards_fact_len check (char_length(fact_text) between 1 and 600),
    constraint community_cards_scale check (body_scale between 0.5 and 2.0),
    constraint community_cards_style check (style in
        ('PAPER','VINYL','COLLAGE','NEUMORPHIC','EDITORIAL','MINIMAL','SIGNATURE')),
    constraint community_cards_aspect check (aspect in ('PORTRAIT','CLASSIC'))
);

-- EVOLUTION: `create table if not exists` above does nothing to a table that
-- already exists, so every column added later lands here too — re-pasting the
-- file upgrades an older install instead of silently skipping the column.
alter table public.community_cards add column if not exists caption text not null default '';

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'community_cards_caption_len') then
        alter table public.community_cards
            add constraint community_cards_caption_len check (char_length(caption) <= 180);
    end if;
end $$;

-- Reads are always "live cards, newest first" — the partial index keeps that
-- cheap and keeps expired rows out of the hot path until the sweep runs.
create index if not exists community_cards_live_idx
    on public.community_cards (created_at desc)
    where expires_at > now();
create index if not exists community_cards_owner_idx
    on public.community_cards (owner, created_at desc);
create index if not exists community_cards_expiry_idx
    on public.community_cards (expires_at);

alter table public.community_cards enable row level security;

-- READ: any signed-in user whose own Online Mode is on may read LIVE cards
-- whose author also has Online Mode on. An expired card is invisible even
-- before the purge runs.
drop policy if exists comm_select_live on public.community_cards;
create policy comm_select_live on public.community_cards
    for select to authenticated
    using (
        expires_at > now()
        and exists (
            select 1 from public.profiles p
            where p.id = community_cards.owner and p.online_mode_enabled
        )
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

-- CREATE: you may only post as yourself, only while Online Mode is on, and
-- only with a 24-hour lifetime (a longer expiry is refused server-side).
drop policy if exists comm_insert_own on public.community_cards;
create policy comm_insert_own on public.community_cards
    for insert to authenticated
    with check (
        owner = auth.uid()
        and expires_at <= now() + interval '25 hours'
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

-- DELETE: authors may pull their own card early. No update policy at all —
-- a posted card is immutable, which also means nobody can extend its life.
drop policy if exists comm_delete_own on public.community_cards;
create policy comm_delete_own on public.community_cards
    for delete to authenticated
    using (owner = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 4. community_reactions — one like per user per card
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_reactions (
    card_id    uuid not null references public.community_cards (id) on delete cascade,
    user_id    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (card_id, user_id)
);

create index if not exists community_reactions_card_idx
    on public.community_reactions (card_id);

alter table public.community_reactions enable row level security;

-- A reaction is visible only while its card is visible (same rule).
drop policy if exists reac_select_visible on public.community_reactions;
create policy reac_select_visible on public.community_reactions
    for select to authenticated
    using (
        exists (
            select 1 from public.community_cards c
            where c.id = community_reactions.card_id
              and c.expires_at > now()
              and exists (
                  select 1 from public.profiles p
                  where p.id = c.owner and p.online_mode_enabled
              )
        )
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

drop policy if exists reac_insert_own on public.community_reactions;
create policy reac_insert_own on public.community_reactions
    for insert to authenticated
    with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and exists (
            select 1 from public.community_cards c
            where c.id = community_reactions.card_id and c.expires_at > now()
        )
    );

drop policy if exists reac_delete_own on public.community_reactions;
create policy reac_delete_own on public.community_reactions
    for delete to authenticated
    using (user_id = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 4b. community_comments — the replies under a card
--     Comments die with the card (cascade), so they can never outlive the
--     24-hour promise. Text only, exactly like the cards themselves.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_comments (
    id            uuid primary key default gen_random_uuid(),
    card_id       uuid not null references public.community_cards (id) on delete cascade,
    author        uuid not null default auth.uid() references auth.users (id) on delete cascade,
    author_handle text not null default 'A curious soul',
    body          text not null,
    created_at    timestamptz not null default now(),
    constraint community_comments_body_len check (char_length(body) between 1 and 400)
);

create index if not exists community_comments_card_idx
    on public.community_comments (card_id, created_at);

alter table public.community_comments enable row level security;

-- Readable only while the card is live AND both sides have Online Mode on.
drop policy if exists cmt_select_visible on public.community_comments;
create policy cmt_select_visible on public.community_comments
    for select to authenticated
    using (
        exists (
            select 1 from public.community_cards c
            where c.id = community_comments.card_id
              and c.expires_at > now()
              and exists (
                  select 1 from public.profiles p
                  where p.id = c.owner and p.online_mode_enabled
              )
        )
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

drop policy if exists cmt_insert_own on public.community_comments;
create policy cmt_insert_own on public.community_comments
    for insert to authenticated
    with check (
        author = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and exists (
            select 1 from public.community_cards c
            where c.id = community_comments.card_id and c.expires_at > now()
        )
    );

-- Only the comment's own author may remove it.
drop policy if exists cmt_delete_own on public.community_comments;
create policy cmt_delete_own on public.community_comments
    for delete to authenticated
    using (author = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5. community_reports — moderation queue (write-only for users)
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_reports (
    id         uuid primary key default gen_random_uuid(),
    card_id    uuid not null references public.community_cards (id) on delete cascade,
    reporter   uuid not null default auth.uid() references auth.users (id) on delete cascade,
    reason     text not null default 'other',
    note       text,
    created_at timestamptz not null default now(),
    constraint community_reports_once unique (card_id, reporter)
);

create index if not exists community_reports_card_idx
    on public.community_reports (card_id);

alter table public.community_reports enable row level security;

-- Reporters may see the reports they filed; the moderation team reads the
-- table through the service role (which bypasses RLS), never the app.
drop policy if exists rep_insert_own on public.community_reports;
create policy rep_insert_own on public.community_reports
    for insert to authenticated
    with check (reporter = auth.uid());

drop policy if exists rep_select_own on public.community_reports;
create policy rep_select_own on public.community_reports
    for select to authenticated
    using (reporter = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5b. profile discoverability — the social layer needs to find people
--
--     Friend requests are impossible without a way to look someone up, so
--     `profiles` gains a `discoverable` flag and a second READ policy for
--     the PUBLIC identity of accounts that opted in. Policies are OR-ed, so
--     the self-read above still works exactly as before.
--
--     What becomes readable: id, display_name, online_mode_enabled and the
--     timestamps — profiles holds no private data (captures, cards, replies
--     and messages all live in their own RLS-protected tables). An account
--     that keeps Online Mode OFF is not discoverable at all, which is the
--     consent boundary: turning the switch off hides you from search.
-- ───────────────────────────────────────────────────────────────────────────
alter table public.profiles add column if not exists discoverable boolean not null default true;

drop policy if exists prof_select_discoverable on public.profiles;
create policy prof_select_discoverable on public.profiles
    for select to authenticated
    using (discoverable and online_mode_enabled);

-- ───────────────────────────────────────────────────────────────────────────
-- 5c. friend_requests — one request per pair, either direction
--     A request carries its own status, so "accepted" IS the friendship and
--     no second table can drift out of sync with it.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.friend_requests (
    id           uuid primary key default gen_random_uuid(),
    requester    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    addressee    uuid not null references auth.users (id) on delete cascade,
    status       text not null default 'pending'
                 check (status in ('pending', 'accepted', 'declined')),
    created_at   timestamptz not null default now(),
    responded_at timestamptz,
    constraint friend_requests_not_self check (requester <> addressee)
);

-- The unordered pair, so A→B and B→A can never both sit there as separate
-- pending requests. PARTIAL (declined rows are excluded) so a declined
-- request does not permanently block a later one, and unfriending leaves a
-- clean slate because the row is deleted.
alter table public.friend_requests add column if not exists pair_key text
    generated always as (
        least(requester::text, addressee::text) || ':' || greatest(requester::text, addressee::text)
    ) stored;

create unique index if not exists friend_requests_pair_key
    on public.friend_requests (pair_key) where status <> 'declined';
create index if not exists friend_requests_incoming_idx
    on public.friend_requests (addressee, created_at desc);
create index if not exists friend_requests_outgoing_idx
    on public.friend_requests (requester, created_at desc);

-- The two sides of a request are IMMUTABLE. RLS alone cannot express that:
-- an UPDATE policy's USING sees the old row and its WITH CHECK sees the new
-- one, so neither can compare them — without this trigger an addressee could
-- accept a request and swap the requester for a third party in the same
-- statement, manufacturing a friendship that person never agreed to.
create or replace function public.curio_pin_request_parties()
returns trigger
language plpgsql
as $$
begin
    if new.requester <> old.requester or new.addressee <> old.addressee then
        raise exception 'curio: a friend request cannot change who it is between';
    end if;
    if old.status <> 'pending' then
        raise exception 'curio: this friend request was already answered';
    end if;
    new.responded_at := coalesce(new.responded_at, now());
    return new;
end $$;

drop trigger if exists friend_requests_pin_parties on public.friend_requests;
create trigger friend_requests_pin_parties
    before update on public.friend_requests
    for each row execute function public.curio_pin_request_parties();

alter table public.friend_requests enable row level security;

-- READ: only the two sides of the request can see it.
drop policy if exists fr_select_own on public.friend_requests;
create policy fr_select_own on public.friend_requests
    for select to authenticated
    using (requester = auth.uid() or addressee = auth.uid());

-- CREATE: you may only ask as yourself, only at someone who can actually be
-- reached (they have Online Mode on), and only while your own Online Mode is
-- on. Asking yourself is refused by the constraint above.
drop policy if exists fr_insert_own on public.friend_requests;
create policy fr_insert_own on public.friend_requests
    for insert to authenticated
    with check (
        requester = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and exists (
            select 1 from public.profiles them
            where them.id = friend_requests.addressee
              and them.online_mode_enabled
              and them.discoverable
        )
    );

-- ANSWER: only the ADDRESSEE may move a pending request, and only to
-- accepted / declined — an accepted row IS the friendship, so this is the
-- single place one can come into being. The trigger above pins the pair and
-- stops an answered request from being answered again.
drop policy if exists fr_respond_addressee on public.friend_requests;
create policy fr_respond_addressee on public.friend_requests
    for update to authenticated
    using (addressee = auth.uid() and status = 'pending')
    with check (addressee = auth.uid() and status in ('accepted', 'declined'));

-- REMOVE: either side may delete — the requester cancels a pending ask, and
-- either friend unfriends. The row goes, so the pair is free again.
drop policy if exists fr_delete_own on public.friend_requests;
create policy fr_delete_own on public.friend_requests
    for delete to authenticated
    using (requester = auth.uid() or addressee = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5c(ii). curio_are_friends — the ONE definition of "these two are friends"
--     Defined AFTER the table above: a SQL-language function is parsed and
--     validated at CREATE time, so it cannot be declared before the relation
--     it reads exists.
--
--     SECURITY DEFINER so the answer is a fact about the pair, not a view
--     through the caller's own RLS (which would only ever see the caller's
--     side of a request). It returns a boolean about two specific ids and
--     nothing else, and the app's role is granted EXECUTE on it in §7.
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.curio_are_friends(a uuid, b uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.friend_requests f
        where f.status = 'accepted'
          and ((f.requester = a and f.addressee = b)
            or (f.requester = b and f.addressee = a))
    );
$$;

-- ───────────────────────────────────────────────────────────────────────────
-- 5d. dm_messages — direct messages, text only, friends only
--     No media column exists by design (same rule as the cards), and the
--     INSERT policy requires an ACCEPTED request between the two people, so
--     nobody can message a stranger. Reads stay open to both participants
--     even after an unfriend: you keep the conversation you were part of.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.dm_messages (
    id         uuid primary key default gen_random_uuid(),
    sender     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recipient  uuid not null references auth.users (id) on delete cascade,
    body       text not null,
    created_at timestamptz not null default now(),
    read_at    timestamptz,
    constraint dm_messages_not_self check (sender <> recipient),
    constraint dm_messages_body_len check (char_length(body) between 1 and 2000)
);

create index if not exists dm_messages_thread_idx
    on public.dm_messages (sender, recipient, created_at desc);
create index if not exists dm_messages_inbox_idx
    on public.dm_messages (recipient, created_at desc);

-- Same immutability rule as a friend request: a receipt is the only update a
-- message ever takes, so the participants must not be re-pointable.
create or replace function public.curio_pin_message_parties()
returns trigger
language plpgsql
as $$
begin
    if new.sender <> old.sender or new.recipient <> old.recipient then
        raise exception 'curio: a message cannot change who it is between';
    end if;
    return new;
end $$;

drop trigger if exists dm_messages_pin_parties on public.dm_messages;
create trigger dm_messages_pin_parties
    before update on public.dm_messages
    for each row execute function public.curio_pin_message_parties();

alter table public.dm_messages enable row level security;

-- READ: only the two people in the conversation.
drop policy if exists dm_select_participants on public.dm_messages;
create policy dm_select_participants on public.dm_messages
    for select to authenticated
    using (sender = auth.uid() or recipient = auth.uid());

-- CREATE: you send as yourself, to someone with Online Mode on, while your
-- own Online Mode is on, and ONLY to someone who accepted your request.
drop policy if exists dm_insert_friends on public.dm_messages;
create policy dm_insert_friends on public.dm_messages
    for insert to authenticated
    with check (
        sender = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and exists (
            select 1 from public.profiles them
            where them.id = dm_messages.recipient and them.online_mode_enabled
        )
        and public.curio_are_friends(auth.uid(), dm_messages.recipient)
    );

-- READ RECEIPT: the RECIPIENT may stamp read_at on their own inbox
-- (the participants themselves are pinned by the trigger above).
drop policy if exists dm_update_receipt on public.dm_messages;
create policy dm_update_receipt on public.dm_messages
    for update to authenticated
    using (recipient = auth.uid())
    with check (recipient = auth.uid());

-- DELETE: only your own message.
drop policy if exists dm_delete_own on public.dm_messages;
create policy dm_delete_own on public.dm_messages
    for delete to authenticated
    using (sender = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 6. Expiry sweep — housekeeping for the 24-hour cards
--    Reads already filter on `expires_at`, so this only reclaims space.
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.curio_purge_expired_cards()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    removed integer;
begin
    delete from public.community_cards where expires_at <= now();
    get diagnostics removed = row_count;
    return removed;
end;
$$;

revoke all on function public.curio_purge_expired_cards() from public, anon, authenticated;

-- Optional: hourly automatic cleanup. Requires the `pg_cron` extension
-- (Dashboard → Database → Extensions). Uncomment and run once:
--
-- select cron.schedule(
--     'curio-purge-expired-cards',
--     '7 * * * *',
--     $$select public.curio_purge_expired_cards();$$
-- );
--
-- Without pg_cron, run the function manually whenever you like:
--   select public.curio_purge_expired_cards();

-- ───────────────────────────────────────────────────────────────────────────
-- 7. Grants — RLS still gates every row; these only let the signed-in role
--    reach the tables at all. `anon` gets nothing anywhere.
-- ───────────────────────────────────────────────────────────────────────────
grant usage on schema public to authenticated;
grant select, insert, update, delete on public.profiles to authenticated;
grant select, insert, update, delete on public.cloud_captures to authenticated;
grant select, insert, delete on public.community_cards to authenticated;
grant select, insert, delete on public.community_reactions to authenticated;
grant select, insert, delete on public.community_comments to authenticated;
grant select, insert on public.community_reports to authenticated;
grant select, insert, update, delete on public.friend_requests to authenticated;
grant select, insert, update, delete on public.dm_messages to authenticated;

-- The friendship test is callable by any signed-in user (it only ever
-- answers about two ids); the trigger helpers are NOT — they run as the
-- table owner when the trigger fires.
revoke all on function public.curio_are_friends(uuid, uuid) from public, anon;
grant execute on function public.curio_are_friends(uuid, uuid) to authenticated;
revoke all on function public.curio_pin_request_parties() from public, anon, authenticated;
revoke all on function public.curio_pin_message_parties() from public, anon, authenticated;

revoke all on public.profiles from anon;
revoke all on public.cloud_captures from anon;
revoke all on public.community_cards from anon;
revoke all on public.community_reactions from anon;
revoke all on public.community_comments from anon;
revoke all on public.community_reports from anon;
revoke all on public.friend_requests from anon;
revoke all on public.dm_messages from anon;

-- ───────────────────────────────────────────────────────────────────────────
-- 8. Self-check — prints PASS/FAIL per expectation so a bad paste is obvious
-- ───────────────────────────────────────────────────────────────────────────
do $$
declare
    rls_off text;
    anon_open text;
    missing   text;
begin
    select string_agg(c.relname, ', ')
      into rls_off
      from pg_class c
      join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = 'public'
       and c.relname in ('profiles','cloud_captures','community_cards',
                         'community_reactions','community_comments',
                         'community_reports','friend_requests','dm_messages')
       and c.relrowsecurity = false;
    if rls_off is null then
        raise notice 'PASS  RLS enabled on every Curio table';
    else
        raise warning 'FAIL  RLS is OFF on: %', rls_off;
    end if;

    select string_agg(tablename, ', ')
      into anon_open
      from pg_policies
     where schemaname = 'public'
       and roles::text like '%anon%'
       and tablename in ('profiles','cloud_captures','community_cards',
                         'community_reactions','community_comments',
                         'community_reports','friend_requests','dm_messages');
    if anon_open is null then
        raise notice 'PASS  no anon policies on Curio tables';
    else
        raise warning 'FAIL  anon policies exist on: %', anon_open;
    end if;

    select string_agg(t, ', ')
      into missing
      from unnest(array['profiles','cloud_captures','community_cards',
                        'community_reactions','community_comments',
                        'community_reports','friend_requests','dm_messages']) as t
     where not exists (
        select 1 from pg_class c
          join pg_namespace n on n.oid = c.relnamespace
         where n.nspname = 'public' and c.relname = t
     );
    if missing is null then
        raise notice 'PASS  every Curio table exists';
    else
        raise warning 'FAIL  missing tables: %', missing;
    end if;
end $$;
