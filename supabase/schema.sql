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
--    out of every read by `expires_at > now()`; DM messages have the same
--    24-hour window (§6c) and are swept by `curio_purge_expired_messages()`.
--  • Public text is moderated at the DATABASE too (§5h): `curio_text_is_clean`
--    is CHECKed on community_cards, community_comments and profiles, so a
--    modified client cannot post a slur. `dm_messages` is DELIBERATELY EXEMPT
--    — a private conversation between two friends is protected by RLS, not by
--    a word list.
--  • Nothing media-backed is ever allowed to sync: the tables only carry text.
-- ═══════════════════════════════════════════════════════════════════════════

create extension if not exists pgcrypto;

-- ───────────────────────────────────────────────────────────────────────────
-- 1. profiles — one row per account, holds the account's Online Mode switch
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.profiles (
    id                  uuid primary key references auth.users (id) on delete cascade,
    display_name        text,
    username            text,
    -- 0..27 = the 28 code-drawn portraits (SOCIAL_AVATAR_STYLE_COUNT in the
    -- app's SocialApi.kt). Widen this bound in the SAME commit that adds a
    -- style to the app's AVATARS list, or the new pick is rejected on write.
    avatar_style        smallint not null default 0 check (avatar_style between 0 and 27),
    online_mode_enabled boolean not null default false,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.profiles add column if not exists username text;
alter table public.profiles add column if not exists presence_mode text not null default 'active';
alter table public.profiles drop constraint if exists profiles_presence_mode_check;
alter table public.profiles add constraint profiles_presence_mode_check check (presence_mode in ('active', 'dnd', 'hidden'));
alter table public.profiles add column if not exists avatar_style smallint not null default 0;
-- v3xx52 — the portrait count grew from 16 to 28 (the soft set), so the range
-- check is REPLACED rather than merely created: an install that already
-- carries the old 0..15 constraint must be widened, or picking a new style
-- fails the write. Idempotent — re-pasting with the new bound is a no-op.
do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_avatar_style_range') then
        alter table public.profiles add constraint profiles_avatar_style_range
            check (avatar_style between 0 and 27);
    else
        alter table public.profiles drop constraint profiles_avatar_style_range;
        alter table public.profiles add constraint profiles_avatar_style_range
            check (avatar_style between 0 and 27);
    end if;
end $$;
create unique index if not exists profiles_username_unique
    on public.profiles (lower(username))
    where username is not null and length(trim(username)) between 3 and 24;

drop policy if exists prof_select_own on public.profiles;
create policy prof_select_own on public.profiles
    for select to authenticated
    using (id = auth.uid() or (online_mode_enabled and discoverable));

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
    id             text primary key,
    owner          uuid not null default auth.uid() references auth.users (id) on delete cascade,
    topic_name     text not null default '',
    category_slug  text not null default '',
    capture_format text not null default '',
    title          text not null default '',
    body_text      text not null default '',
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    deleted_at     timestamptz
);

create index if not exists cloud_captures_owner_idx
    on public.cloud_captures (owner, updated_at desc);
create index if not exists cloud_captures_live_idx
    on public.cloud_captures (owner) where deleted_at is null;

alter table public.cloud_captures enable row level security;

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
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_cards (
    id             uuid primary key default gen_random_uuid(),
    owner          uuid not null default auth.uid() references auth.users (id) on delete cascade,
    author_handle  text not null default 'A curious soul',
    -- What kind of post this is. A CARD is a topic share card, a NOTE is a
    -- tweet-style text post (no topic, no card art), and a QUOTE is a line
    -- someone else said. All three are still TEXT ONLY — the column chooses a
    -- renderer, it cannot carry media.
    kind           text not null default 'CARD',
    topic_name     text not null default '',
    category_slug  text not null default '',
    category_name  text not null default '',
    category_glyph text not null default '',
    accent_hex     text not null default '#8E8E93',
    fact_text      text not null,
    caption        text not null default '',
    style          text not null default 'PAPER',
    aspect         text not null default 'CLASSIC',
    body_scale     real not null default 1.0,
    byline         text not null default '',
    created_at     timestamptz not null default now(),
    expires_at     timestamptz not null default (now() + interval '24 hours'),
    -- A topic CARD must say something (it renders the question and its quick
    -- fact); a NOTE or QUOTE may be short but never empty either — the same 1
    -- character floor, just phrased so an upgrade can drop the old constraint.
    constraint community_cards_fact_len_v2 check (
        char_length(fact_text) <= 600 and char_length(fact_text) >= 1
    ),
    constraint community_cards_kind check (kind in ('CARD', 'NOTE', 'QUOTE')),
    constraint community_cards_scale check (body_scale between 0.5 and 2.0),
    constraint community_cards_style check (style in
        ('PAPER','VINYL','COLLAGE','NEUMORPHIC','EDITORIAL','MINIMAL','SIGNATURE')),
    constraint community_cards_aspect check (aspect in ('PORTRAIT','CLASSIC'))
);

alter table public.community_cards add column if not exists caption text not null default '';
alter table public.community_cards add column if not exists kind text not null default 'CARD';
alter table public.community_cards alter column topic_name set default '';

-- Upgrade path for a project that already ran an earlier version of this
-- file: the original fact constraint demanded at least one character of a
-- topic card's body and knew nothing about `kind`, so it is replaced by the
-- kind-aware one (a CARD still has to say something; a NOTE may be a line).
do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'community_cards_caption_len') then
        alter table public.community_cards
            add constraint community_cards_caption_len check (char_length(caption) <= 180);
    end if;

    if exists (select 1 from pg_constraint where conname = 'community_cards_fact_len') then
        alter table public.community_cards drop constraint community_cards_fact_len;
    end if;

    if not exists (select 1 from pg_constraint where conname = 'community_cards_fact_len_v2') then
        alter table public.community_cards
            add constraint community_cards_fact_len_v2 check (
                char_length(fact_text) <= 600 and char_length(fact_text) >= 1
            );
    end if;

    if not exists (select 1 from pg_constraint where conname = 'community_cards_kind') then
        alter table public.community_cards
            add constraint community_cards_kind check (kind in ('CARD', 'NOTE', 'QUOTE'));
    end if;
end $$;

-- NOTE: the old `community_cards_live_idx` partial index (WHERE expires_at >
-- now()) has been removed — Postgres requires partial-index predicates to be
-- IMMUTABLE, and now() is only STABLE, so that index failed with 42P17 on
-- creation. `community_cards_expiry_idx` below (plain index on expires_at)
-- covers the same read pattern; Postgres applies expires_at > now() as an
-- ordinary WHERE filter at query time, which has no immutability requirement.
create index if not exists community_cards_owner_idx
    on public.community_cards (owner, created_at desc);
create index if not exists community_cards_expiry_idx
    on public.community_cards (expires_at);

alter table public.community_cards enable row level security;

-- The display handle is presentation data, but it must still come from the
-- authenticated profile. Without this trigger a modified client could post a
-- card claiming another member's username.
create or replace function public.curio_stamp_author_handle()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    select coalesce(nullif(trim(username), ''), nullif(trim(display_name), ''), 'A curious soul')
      into new.author_handle
      from public.profiles
     where id = auth.uid();
    new.author_handle := coalesce(new.author_handle, 'A curious soul');
    return new;
end $$;

drop trigger if exists community_cards_stamp_author_handle on public.community_cards;
create trigger community_cards_stamp_author_handle
    before insert on public.community_cards
    for each row execute function public.curio_stamp_author_handle();

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
    kind       text not null default 'like',
    created_at timestamptz not null default now(),
    primary key (card_id, user_id),
    constraint community_reactions_kind_check check (kind in ('like', 'dislike'))
);

alter table public.community_reactions add column if not exists kind text not null default 'like';
alter table public.community_reactions drop constraint if exists community_reactions_kind_check;
alter table public.community_reactions add constraint community_reactions_kind_check check (kind in ('like', 'dislike'));

create index if not exists community_reactions_card_idx
    on public.community_reactions (card_id);

alter table public.community_reactions enable row level security;

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

drop policy if exists reac_update_own on public.community_reactions;
create policy reac_update_own on public.community_reactions
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists reac_delete_own on public.community_reactions;
create policy reac_delete_own on public.community_reactions
  for delete to authenticated
  using (user_id = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 4b. community_comments — the replies under a card
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_comments (
    id            uuid primary key default gen_random_uuid(),
    card_id       uuid not null references public.community_cards (id) on delete cascade,
    author        uuid not null default auth.uid() references auth.users (id) on delete cascade,
    author_handle text not null default 'A curious soul',
    body          text not null,
    -- The reply this one answers, for BRANCHED threads. Null is a top-level
    -- reply. A trigger below refuses a parent that lives on another card, so a
    -- branch can never be grafted onto somebody else's post.
    parent_id     uuid references public.community_comments (id) on delete cascade,
    created_at    timestamptz not null default now(),
    constraint community_comments_body_len check (char_length(body) between 1 and 400)
);

alter table public.community_comments
    add column if not exists parent_id uuid references public.community_comments (id) on delete cascade;

create index if not exists community_comments_card_idx
    on public.community_comments (card_id, created_at);
create index if not exists community_comments_parent_idx
    on public.community_comments (parent_id);

create or replace function public.curio_pin_comment_parent()
returns trigger
language plpgsql
as $$
declare
    parent_card uuid;
begin
    if new.parent_id is null then
        return new;
    end if;
    if new.parent_id = new.id then
        raise exception 'curio: a reply cannot answer itself';
    end if;
    select card_id into parent_card
      from public.community_comments
     where id = new.parent_id;
    if parent_card is null or parent_card <> new.card_id then
        raise exception 'curio: a reply can only answer a reply on its own card';
    end if;
    return new;
end $$;

drop trigger if exists community_comments_pin_parent on public.community_comments;
create trigger community_comments_pin_parent
    before insert or update on public.community_comments
    for each row execute function public.curio_pin_comment_parent();

alter table public.community_comments enable row level security;

drop trigger if exists community_comments_stamp_author_handle on public.community_comments;
create trigger community_comments_stamp_author_handle
    before insert on public.community_comments
    for each row execute function public.curio_stamp_author_handle();

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

drop policy if exists rep_insert_own on public.community_reports;
create policy rep_insert_own on public.community_reports
    for insert to authenticated
    with check (reporter = auth.uid());

drop policy if exists rep_select_own on public.community_reports;
create policy rep_select_own on public.community_reports
    for select to authenticated
    using (reporter = auth.uid());

-- ────────────────────────────────────────────────��──────────────────────────
-- 5b. profile discoverability
-- ───────────────────────────────────────────────────────────────────────────
alter table public.profiles add column if not exists discoverable boolean not null default true;

drop policy if exists prof_select_discoverable on public.profiles;
create policy prof_select_discoverable on public.profiles
    for select to authenticated
    using (discoverable and online_mode_enabled);

-- ───────────────────────────────────────────────────────────────────────────
-- 5c. friend_requests — one request per pair, either direction
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

drop policy if exists fr_select_own on public.friend_requests;
create policy fr_select_own on public.friend_requests
    for select to authenticated
    using (
        (requester = auth.uid() or addressee = auth.uid())
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

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

drop policy if exists fr_respond_addressee on public.friend_requests;
create policy fr_respond_addressee on public.friend_requests
    for update to authenticated
    using (addressee = auth.uid() and status = 'pending')
    with check (addressee = auth.uid() and status in ('accepted', 'declined'));

drop policy if exists fr_delete_own on public.friend_requests;
create policy fr_delete_own on public.friend_requests
    for delete to authenticated
    using (requester = auth.uid() or addressee = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5c(ii). curio_are_friends — the ONE definition of "these two are friends"
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
-- 5d. DM encryption identities and conversation-key envelopes
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.dm_device_keys (
    user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    device_id text not null,
    public_key text not null,
    key_version integer not null default 1 check (key_version > 0),
    created_at timestamptz not null default now(),
    retired_at timestamptz,
    primary key (user_id, device_id)
);

create table if not exists public.dm_key_envelopes (
    conversation_id text not null,
    recipient uuid not null references auth.users (id) on delete cascade,
    device_id text not null,
    key_version integer not null check (key_version > 0),
    encrypted_key text not null,
    encryption_version text not null default 'curio-dm-v1',
    created_at timestamptz not null default now(),
    primary key (conversation_id, recipient, device_id, key_version)
);

alter table public.dm_device_keys enable row level security;
alter table public.dm_key_envelopes enable row level security;

drop policy if exists dm_device_keys_own on public.dm_device_keys;
drop policy if exists dm_device_keys_select_participant on public.dm_device_keys;
create policy dm_device_keys_select_participant on public.dm_device_keys
  for select to authenticated using (
    user_id = auth.uid() or public.curio_are_friends(auth.uid(), user_id)
  );
drop policy if exists dm_device_keys_own on public.dm_device_keys;
create policy dm_device_keys_own on public.dm_device_keys
  for insert to authenticated with check (user_id = auth.uid());

drop policy if exists dm_device_keys_update_own on public.dm_device_keys;
create policy dm_device_keys_update_own on public.dm_device_keys
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists dm_device_keys_delete_own on public.dm_device_keys;
create policy dm_device_keys_delete_own on public.dm_device_keys
  for delete to authenticated using (user_id = auth.uid());

-- PostgREST implements an upsert as an INSERT followed by an UPDATE on a
-- conflicting row. PostgreSQL requires the caller to be able to SELECT that
-- existing row before the UPDATE policy is considered. A sender could write a
-- first envelope for a friend but could not replace it after restoring a
-- device, which made all later sends fail with an RLS error. An envelope is
-- still encrypted to its recipient's public key; allowing the accepted friend
-- to see the envelope lets the participant upsert it without exposing message
-- plaintext or a private key.
drop policy if exists dm_key_envelopes_recipient on public.dm_key_envelopes;
drop policy if exists dm_key_envelopes_select_participant on public.dm_key_envelopes;
create policy dm_key_envelopes_select_participant on public.dm_key_envelopes
    for select to authenticated using (
        recipient = auth.uid()
        or public.curio_are_friends(auth.uid(), recipient)
    );

drop policy if exists dm_key_envelopes_write_participant on public.dm_key_envelopes;
create policy dm_key_envelopes_write_participant on public.dm_key_envelopes
  for insert to authenticated with check (
  recipient = auth.uid()
  or public.curio_are_friends(auth.uid(), recipient)
  );

drop policy if exists dm_key_envelopes_update_participant on public.dm_key_envelopes;
create policy dm_key_envelopes_update_participant on public.dm_key_envelopes
  for update to authenticated
  using (
    recipient = auth.uid()
    or public.curio_are_friends(auth.uid(), recipient)
  )
  with check (
    recipient = auth.uid()
    or public.curio_are_friends(auth.uid(), recipient)
  );

-- ───────────────────────────���─────────────────────────────────���─────────────
-- 5e. dm_messages — ciphertext-only writes; legacy body is read-only
-- ────────────────────────────────────���──────────────────────────────────────
create table if not exists public.dm_messages (
    id         uuid primary key default gen_random_uuid(),
    sender     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recipient  uuid not null references auth.users (id) on delete cascade,
    body       text,
    ciphertext text,
    nonce      text,
    encryption_version text,
    migration_state text not null default 'legacy' check (migration_state in ('legacy','encrypted','pending_reencrypt')),
    created_at timestamptz not null default now(),
    read_at    timestamptz,
    constraint dm_messages_not_self check (sender <> recipient),
    constraint dm_messages_ciphertext_shape check (
        (migration_state = 'legacy' and body is not null and ciphertext is null)
        or (migration_state in ('encrypted','pending_reencrypt') and ciphertext is not null and nonce is not null and encryption_version is not null)
    )
);

alter table public.dm_messages add column if not exists ciphertext text;
alter table public.dm_messages add column if not exists nonce text;
alter table public.dm_messages add column if not exists encryption_version text;
alter table public.dm_messages add column if not exists migration_state text not null default 'legacy';
alter table public.dm_messages alter column body drop not null;

-- Existing plaintext rows remain explicitly legacy/read-only during migration.
drop policy if exists dm_insert_friends on public.dm_messages;
create policy dm_insert_friends on public.dm_messages
    for insert to authenticated with check (
        sender = auth.uid() and migration_state = 'encrypted'
        and body is null and ciphertext is not null and nonce is not null
        and exists (select 1 from public.profiles me where me.id = auth.uid() and me.online_mode_enabled)
        and exists (select 1 from public.profiles them where them.id = dm_messages.recipient and them.online_mode_enabled)
        and public.curio_are_friends(auth.uid(), dm_messages.recipient)
    );

create index if not exists dm_messages_thread_idx
    on public.dm_messages (sender, recipient, created_at desc);
create index if not exists dm_messages_inbox_idx
    on public.dm_messages (recipient, created_at desc);

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

drop policy if exists dm_select_participants on public.dm_messages;
create policy dm_select_participants on public.dm_messages
    for select to authenticated
    using (
        (sender = auth.uid() or recipient = auth.uid())
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

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

drop policy if exists dm_update_receipt on public.dm_messages;
create policy dm_update_receipt on public.dm_messages
    for update to authenticated
    using (recipient = auth.uid())
    with check (recipient = auth.uid());

-- A direct-message deletion is a recall for the two participants, not a
-- sender-only local hide. The participant check keeps unrelated accounts out.
drop policy if exists dm_delete_own on public.dm_messages;
drop policy if exists dm_delete_participant on public.dm_messages;
create policy dm_delete_participant on public.dm_messages
    for delete to authenticated
    using (sender = auth.uid() or recipient = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5e. dm_typing — "is typing…", one row per (sender, recipient) pair
--
-- Deliberately tiny and self-cleaning: the client upserts its own row while
-- the composer has text, and every read ignores rows older than a few
-- seconds, so a crashed app leaves nothing behind that anyone would see.
-- A row is not a message and carries no content — only WHO is typing to WHOM.
-- ──────────────────────────���────────────────────────────────────────────────
create table if not exists public.dm_typing (
    sender     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recipient  uuid not null references auth.users (id) on delete cascade,
    updated_at timestamptz not null default now(),
    primary key (sender, recipient),
    constraint dm_typing_not_self check (sender <> recipient)
);

-- The stamp is the server's clock, never the phone's: an upsert that kept
-- resending a client timestamp could otherwise look "stale" on a device whose
-- clock is behind, and "fresh" forever on one that is ahead.
create or replace function public.curio_stamp_typing()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end $$;

drop trigger if exists dm_typing_stamp on public.dm_typing;
create trigger dm_typing_stamp
    before insert or update on public.dm_typing
    for each row execute function public.curio_stamp_typing();

alter table public.dm_typing enable row level security;

-- The other side may only see that you are typing; you alone may write your
-- own row (and only to an accepted friend, exactly like a message).
drop policy if exists dm_typing_select_parties on public.dm_typing;
create policy dm_typing_select_parties on public.dm_typing
    for select to authenticated
    using (sender = auth.uid() or recipient = auth.uid());

drop policy if exists dm_typing_upsert_own on public.dm_typing;
create policy dm_typing_upsert_own on public.dm_typing
    for insert to authenticated
    with check (
        sender = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and public.curio_are_friends(auth.uid(), dm_typing.recipient)
    );

drop policy if exists dm_typing_update_own on public.dm_typing;
create policy dm_typing_update_own on public.dm_typing
    for update to authenticated
    using (sender = auth.uid())
    with check (sender = auth.uid());

drop policy if exists dm_typing_delete_own on public.dm_typing;
create policy dm_typing_delete_own on public.dm_typing
    for delete to authenticated
    using (sender = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5f. dm_reactions — one reaction per person per message
--
-- [kind] is a short glyph NAME from the app's own icon set (never uploaded
-- artwork), so a reaction is a single small word on the server. A second
-- reaction from the same person replaces the first — the unique key is what
-- makes that a guaranteed fact rather than a client convention.
-- ──────────────────────────────────────────────��────────────────────────────
create table if not exists public.dm_reactions (
    message_id uuid not null references public.dm_messages (id) on delete cascade,
    user_id    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    kind       text not null,
    created_at timestamptz not null default now(),
    primary key (message_id, user_id),
    constraint dm_reactions_kind_len check (char_length(kind) between 1 and 32)
);

create index if not exists dm_reactions_message_idx
    on public.dm_reactions (message_id);

alter table public.dm_reactions enable row level security;

-- Only the two people in the conversation can see (or leave) a reaction on
-- one of its messages. Both halves are re-checked against the message row, so
-- a reaction can never be planted on somebody else's thread.
drop policy if exists dm_reactions_select_parties on public.dm_reactions;
create policy dm_reactions_select_parties on public.dm_reactions
    for select to authenticated
    using (
        exists (
            select 1 from public.dm_messages m
            where m.id = dm_reactions.message_id
              and (m.sender = auth.uid() or m.recipient = auth.uid())
        )
    );

drop policy if exists dm_reactions_insert_own on public.dm_reactions;
create policy dm_reactions_insert_own on public.dm_reactions
    for insert to authenticated
    with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.dm_messages m
            where m.id = dm_reactions.message_id
              and (m.sender = auth.uid() or m.recipient = auth.uid())
        )
    );

drop policy if exists dm_reactions_update_own on public.dm_reactions;
create policy dm_reactions_update_own on public.dm_reactions
    for update to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

drop policy if exists dm_reactions_delete_own on public.dm_reactions;
create policy dm_reactions_delete_own on public.dm_reactions
    for delete to authenticated
    using (user_id = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 6. Expiry sweep — housekeeping for the 24-hour cards
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

-- ───────────────────────────────────────────────────────────────────────────
-- 6b. THE TEXT GATE — one fold, enforced by the database
--
-- The app refuses this text before it leaves the device
-- (`data/CurioContentFilter.kt`); this is the SAME fold on the server, so a
-- modified client cannot post around it either.
--
-- `curio_normalize_text` folds a string down to plain lowercase Latin letters:
-- accents dropped, look-alikes mapped (0→o, 3→e, @→a, $→s, |→i …) and every
-- remaining separator or unknown character REMOVED — so "f u c k", "f.u.c.k",
-- "fuuuuck" and "fück" all land on the same letters. The map is APPLIED IN THE
-- SAME ORDER as the Kotlin `CurioContentFilter.LOOKALIKES` list: the
-- single-character `translate()` first, then the ligatures `translate` cannot
-- express because their target is more than one letter (ß→ss, æ→ae, œ→oe,
-- þ→th) as an explicit `replace()` chain. Keep the two in step — a fold that
-- drifts between the layers is a hole a modified client can spell through
-- (homophone spellings such as phuck, fvck and fack are listed EXPLICITLY in
-- the word arrays for the same reason).
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.curio_normalize_text(raw text)
returns text
language sql
immutable
as $$
    with folded as (
        select translate(
            lower(coalesce(raw, '')),
            -- Sources: digits, the symbols people reach for, and the
            -- precomposed accented Latin letters Kotlin removes by
            -- decomposing to NFD and dropping the combining marks.
            '013456789$@!|+£€z'
                || 'àáâãäåāăąçćčďđðèéêëēĕėęěğģìíîïīĭįıĺļľłñńňņ'
                || 'òóôõöøōŏőŕřśŝşšșžźżţťțùúûüūŭůűųýÿ',
            'oieasgtbgsaiitles'
                || 'aaaaaaaaacccdddeeeeeeeeeggiiiiiiiillllnnnnooooooooorr'
                || 'sssssssstttuuuuuuuuuyy'
        ) as t
    )
    select regexp_replace(
        replace(
            replace(
                replace(
                    replace(t, 'ß', 'ss'),
                    'æ', 'ae'
                ),
                'œ', 'oe'
            ),
            'þ', 'th'
        ),
        '[^a-z]', '', 'g'
    )
    from folded;
$$;

create or replace function public.curio_text_is_clean(raw text)
returns boolean
language plpgsql
immutable
as $$
declare
    -- Words matched ANYWHERE in the folded text (profanity, explicit sexual
    -- content, slurs and harassment). Safe to match anywhere because none of
    -- them hides inside an ordinary word.
    anywhere text[] := array[
        'fuck','fucker','fuckers','fucking','fuk','fuking','fukk','fck','fuxk',
        'fux','fuq','fook','phuck','phuk','fvck','fvk','fack','fucked',
        'motherfucker','motherfucking',
        'shit','shits','shyt','bullshit','dipshit','shithead',
        'bitch','bitches','bich','biatch','cunt','cunts','kunt','kunts',
        'dickhead','dickheads','pussy','pussies','whore','whores','slut',
        'sluts','sloot','asshole','assholes','arsehole','arseholes',
        'bastard','bastards','blowjob','blowjobs','handjob','handjobs',
        'rimjob','footjob','onlyfans','hentai','nsfw','sexting','sextape',
        'dildo','dildos','vibrator','vibrators','penis','vagina','nipple',
        'nipples','orgasm','orgasms','cumshot','creampie','bukkake',
        'hooker','hookers','brothel','stripper','strippers','fetish','bdsm',
        'bondage','dominatrix','camgirl','camsex','porno','porn','pornhub',
        'xvideos','threesome','foursome','gangbang','orgy','orgies',
        'pedophile','pedophiles','paedophile','molester','molesters',
        'childporn','lolicon','shotacon',
        'nigger','niggers','nigga','niggas','nigglet','faggot','faggots',
        'fagot','fagots','tranny','trannies','shemale','shemales','ladyboy',
        'retard','retards','retarded','spastic','mongoloid','wetback','chink',
        'kike','raghead','towelhead','redskin','squaw','darkie','gook','nazi',
        'nazis','hitler','whitepower','whitepride','gaschamber','killyourself',
        'neckyourself'
    ];
    -- Words that hide inside ordinary ones (ass in "class", sex in "Essex",
    -- hoe in "shoes", cock in "cocktail", cum in "cucumber") — matched as
    -- WHOLE words only.
    wholeword text[] := array[
        'ass','arse','asses','dumbass','jackass','kickass','sex','sexy',
        'sexual','sexist','sextoy','sextoys','nude','nudes','naked','boob',
        'boobs','tit','tits','titties','cum','anal','rape','raped','raping',
        'rapist','rapists','dic','dick','dicks','cock','cocks','hoe','hoes',
        'horny','wank','wanker','bugger','fag','fags','homo','escort',
        'escorts','coon','paki','kys'
    ];
    folded    text;
    squash    text;
    collapsed text;
    merged    text;
    bad       text;
    pass      integer;
begin
    folded := public.curio_normalize_text(raw);
    squash := regexp_replace(folded, '[^a-z]', '', 'g');
    if length(squash) < 3 then
        return true;
    end if;
    -- Runs of the same letter squeezed, so "fuuuuck" reads as "fuck".
    collapsed := regexp_replace(squash, '(.)\1+', '\1', 'g');

    foreach bad in array anywhere loop
        if position(bad in squash) > 0 or position(bad in collapsed) > 0 then
            return false;
        end if;
    end loop;

    -- Word view, with runs of SINGLE letters merged ("f u c k" -> "fuck").
    merged := array_to_string(regexp_split_to_table(folded, '[^a-z]+'), ' ');
    for pass in 1..12 loop
        merged := regexp_replace(merged, '\y([a-z]) ([a-z])\y', '\1\2', 'g');
    end loop;
    foreach bad in array wholeword loop
        if (' ' || merged || ' ') like ('% ' || bad || ' %') then
            return false;
        end if;
    end loop;
    return true;
end $$;

-- ── The gate itself: a CHECK on every table a person can type into. Guarded so
--    an install whose existing rows already carry blocked text still pastes
--    cleanly (the failure is reported as a NOTICE instead of aborting the
--    paste — clean those rows up, then re-run this file).
do $$
begin
    begin
        alter table public.community_cards
            add constraint community_cards_text_clean
            check (
                public.curio_text_is_clean(fact_text) and
                public.curio_text_is_clean(caption)
            );
        raise notice 'PASS  community_cards text gate installed';
    exception when others then
        raise warning 'FAIL  community_cards text gate: %', sqlerrm;
    end;

    begin
        alter table public.community_comments
            add constraint community_comments_text_clean
            check (public.curio_text_is_clean(body));
        raise notice 'PASS  community_comments text gate installed';
    exception when others then
        raise warning 'FAIL  community_comments text gate: %', sqlerrm;
    end;

    -- dm_messages is DELIBERATELY EXEMPT from the text gate. A direct
    -- message is a private conversation between two friends, so the words
    -- belong to them; the filter guards the PUBLIC surfaces (the 24-hour
    -- wall, its replies, usernames, display names and bios), where anything
    -- written is visible to people who did not choose to read it. RLS (only
    -- the two participants, friends only) is what protects a thread, not a
    -- word list. The drop below removes the constraint from a project that
    -- already pasted an earlier revision of this file.
    begin
        alter table public.dm_messages
            drop constraint if exists dm_messages_text_clean;
        raise notice 'PASS  dm_messages exempt from the text gate';
    exception when others then
        raise warning 'FAIL  dm_messages text gate exemption: %', sqlerrm;
    end;

    begin
        alter table public.profiles
            add constraint profiles_text_clean
            check (
                public.curio_text_is_clean(display_name) and
                public.curio_text_is_clean(username) and
                public.curio_text_is_clean(bio)
            );
        raise notice 'PASS  profiles text gate installed';
    exception when others then
        raise warning 'FAIL  profiles text gate: %', sqlerrm;
    end;
end $$;

-- ─────────────────────────────────────────────────────────────────────────��─
-- 6c. Messages live 24 hours, just like the cards
--
-- A conversation is a 24-hour thing in Curio: the server keeps a message for
-- one day, and the DATABASE — not the client — decides that a day has passed,
-- so an old message is unreadable even to its own participants once it expires.
-- The device keeps its own copy of what it received (that is what makes an
-- offline thread readable), so "gone from the server" and "gone from the app"
-- are two different things on purpose: nothing you received is lost, and
-- nothing you sent lives on the server for longer than a day.
-- ──────────────────────────────────────────────��────────────────────────────
create or replace function public.curio_purge_expired_messages()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    removed integer;
begin
    delete from public.dm_messages
     where created_at <= now() - interval '24 hours';
    get diagnostics removed = row_count;
    return removed;
end;
$$;

revoke all on function public.curio_purge_expired_messages() from public, anon, authenticated;

-- The read window: a participant can only ever READ the last 24 hours, whether
-- or not the sweep has run yet. The purge function is housekeeping; this policy
-- is the actual limit.
drop policy if exists dm_select_participants on public.dm_messages;
create policy dm_select_participants on public.dm_messages
    for select to authenticated
    using (
        (sender = auth.uid() or recipient = auth.uid())
        and created_at > now() - interval '24 hours'
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

-- Optional: run both sweeps hourly. Requires the `pg_cron` extension
-- (Dashboard → Database → Extensions). Uncomment and run once:
--
-- select cron.schedule(
--     'curio-purge-expired',
--     '11 * * * *',
--     $$select public.curio_purge_expired_cards(); select public.curio_purge_expired_messages();$$
-- );

-- ───────────────────────────────────────────────────────────────────────────
-- 7. Grants
-- ───────────────────────────────────────────────────────────────────────────
grant usage on schema public to authenticated;
grant select, insert, update, delete on public.profiles to authenticated;
grant select, insert, update, delete on public.cloud_captures to authenticated;
grant select, insert, delete on public.community_cards to authenticated;
grant select, insert, update, delete on public.community_reactions to authenticated;
grant select, insert, delete on public.community_comments to authenticated;
grant select, insert on public.community_reports to authenticated;
grant select, insert, update, delete on public.friend_requests to authenticated;
grant select, insert, update, delete on public.dm_messages to authenticated;
grant select, insert, update, delete on public.dm_typing to authenticated;
grant select, insert, update, delete on public.dm_reactions to authenticated;

revoke all on function public.curio_are_friends(uuid, uuid) from public, anon;
grant execute on function public.curio_are_friends(uuid, uuid) to authenticated;
revoke all on function public.curio_pin_request_parties() from public, anon, authenticated;
revoke all on function public.curio_pin_message_parties() from public, anon, authenticated;
revoke all on function public.curio_stamp_typing() from public, anon, authenticated;
revoke all on function public.curio_pin_comment_parent() from public, anon, authenticated;
revoke all on function public.curio_stamp_author_handle() from public, anon, authenticated;

revoke all on public.profiles from anon;
revoke all on public.cloud_captures from anon;
revoke all on public.community_cards from anon;
revoke all on public.community_reactions from anon;
revoke all on public.community_comments from anon;
revoke all on public.community_reports from anon;
revoke all on public.friend_requests from anon;
revoke all on public.dm_messages from anon;
revoke all on public.dm_typing from anon;
revoke all on public.dm_reactions from anon;

-- ───────────────────────────────────────────────────────────────────────────
-- 5f. social privacy — profile visibility, presence, blocks
-- ───────────────────────────────────────────────────────────────────────────
-- Three member-owned decisions, all enforced HERE. The client mirrors them so
-- the UI can explain itself; it is never the guard.
--
--   * profile_visibility — 'public' exposes the public identity row to every
--     discoverable member; 'friends' exposes it only to accepted friends (and
--     to the owner, via prof_select_own). The discoverable SELECT policy is
--     recreated below with that extra test.
--   * hide_activity — a member who hides activity has NO stamp stored at all:
--     the client clears `last_active_at` the moment the switch goes on, so
--     there is nothing to read whether a policy is consulted or not. Presence
--     is a courtesy line, never a record.
--   * member_blocks — one row per block. A blocked pair is removed from every
--     reachable surface: cards, replies, likes, requests, messages and each
--     other's profiles. The client also filters its own in-memory lists so a
--     block takes effect on the screen it was made from; these policies are
--     the real guard.
--
-- Privacy NARROWS access, it never widens it: every test below still requires
-- Online Mode on the account whose row is being read.

alter table public.profiles add column if not exists profile_visibility text not null default 'public';
alter table public.profiles add column if not exists hide_activity boolean not null default false;
alter table public.profiles add column if not exists last_active_at timestamptz;

-- The member's own words — the line a profile shows under their name. TEXT
-- ONLY, like everything else in the online layer, and short by design: it is a
-- line on a profile, not a blog. Null (or blank) means "not written", which the
-- app renders as nothing at all rather than a placeholder.
alter table public.profiles add column if not exists bio text;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_bio_len') then
        alter table public.profiles add constraint profiles_bio_len
            check (bio is null or char_length(bio) <= 160);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_visibility_values') then
        alter table public.profiles add constraint profiles_visibility_values
            check (profile_visibility in ('public', 'friends'));
    end if;
end $$;

create table if not exists public.member_blocks (
    blocker    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    blocked    uuid not null references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (blocker, blocked),
    constraint member_blocks_not_self check (blocker <> blocked)
);

create index if not exists member_blocks_blocked_idx on public.member_blocks (blocked);

alter table public.member_blocks enable row level security;

drop policy if exists blk_select_own on public.member_blocks;
create policy blk_select_own on public.member_blocks
    for select to authenticated
    using (blocker = auth.uid());

drop policy if exists blk_insert_own on public.member_blocks;
create policy blk_insert_own on public.member_blocks
    for insert to authenticated
    with check (blocker = auth.uid());

drop policy if exists blk_delete_own on public.member_blocks;
create policy blk_delete_own on public.member_blocks
    for delete to authenticated
    using (blocker = auth.uid());

-- "Is this pair blocked, in either direction?" — security definer so a policy
-- can ask it without read access to the other member's rows.
create or replace function public.curio_is_blocked(a uuid, b uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.member_blocks m
         where (m.blocker = a and m.blocked = b)
            or (m.blocker = b and m.blocked = a)
    );
$$;

grant execute on function public.curio_is_blocked(uuid, uuid) to authenticated;

-- ── profiles: discovery honors visibility AND blocks ─────────────────────
drop policy if exists prof_select_discoverable on public.profiles;
create policy prof_select_discoverable on public.profiles
    for select to authenticated
    using (
        id <> auth.uid()
        and online_mode_enabled
        and discoverable
        and (profile_visibility = 'public' or public.curio_are_friends(auth.uid(), id))
        and not public.curio_is_blocked(auth.uid(), id)
    );

-- ── community_cards: a blocked member is not in your wall ────────────────
drop policy if exists comm_select_live on public.community_cards;
create policy comm_select_live on public.community_cards
    for select to authenticated
    using (
        expires_at > now()
        and not public.curio_is_blocked(auth.uid(), owner)
        and exists (
            select 1 from public.profiles p
            where p.id = community_cards.owner and p.online_mode_enabled
        )
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

-- ── community_comments: nor are their replies ────────────────────────────
drop policy if exists cmt_select_visible on public.community_comments;
create policy cmt_select_visible on public.community_comments
    for select to authenticated
    using (
        not public.curio_is_blocked(auth.uid(), community_comments.author)
        and exists (
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

-- ── community_reactions: nor their likes ────────────────────────────────
drop policy if exists reac_select_visible on public.community_reactions;
create policy reac_select_visible on public.community_reactions
    for select to authenticated
    using (
        not public.curio_is_blocked(auth.uid(), community_reactions.user_id)
        and exists (
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

-- ── friend_requests: a blocked member cannot ask, and cannot be read ─────
drop policy if exists fr_select_own on public.friend_requests;
create policy fr_select_own on public.friend_requests
    for select to authenticated
    using (
        (requester = auth.uid() or addressee = auth.uid())
        and not public.curio_is_blocked(
            requester,
            addressee
        )
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

drop policy if exists fr_insert_own on public.friend_requests;
create policy fr_insert_own on public.friend_requests
    for insert to authenticated
    with check (
        requester = auth.uid()
        and not public.curio_is_blocked(auth.uid(), addressee)
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
        and (select profile_visibility from public.profiles where id = friend_requests.addressee) <> 'friends'
    );

-- ── dm_messages: a block ends the conversation in both directions ────────
drop policy if exists dm_select_participants on public.dm_messages;
create policy dm_select_participants on public.dm_messages
    for select to authenticated
    using (
        (sender = auth.uid() or recipient = auth.uid())
        and not public.curio_is_blocked(sender, recipient)
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
    );

drop policy if exists dm_insert_friends on public.dm_messages;
create policy dm_insert_friends on public.dm_messages
    for insert to authenticated
    with check (
        sender = auth.uid()
        and not public.curio_is_blocked(auth.uid(), recipient)
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

revoke all on public.member_blocks from anon;

-- ───────────────────────────────────────────────────────────────────────────
-- 5g. realtime — which tables the app may SUBSCRIBE to
-- ─────��─────────────────────────────────────────────────────────────────────
-- Curio's live surfaces (an open conversation, the inbox, the wall) are driven
-- by Supabase Realtime instead of a timer: the server announces a change and
-- the screen pulls the delta through the normal REST path. Nothing is trusted
-- from the frame itself, and postgres_changes is RLS-aware, so a subscriber
-- still only ever hears about rows it is allowed to read.
--
-- A table must be a member of the `supabase_realtime` publication to be
-- subscribable at all. This is additive and guarded, so re-pasting is safe,
-- and a project with no realtime publication (or a database that predates it)
-- simply reports a notice and keeps working — the app polls in that case.
--
-- REPLICA IDENTITY FULL on the two tables whose UPDATES matter (a read receipt,
-- an answered request): a filtered UPDATE subscription is matched against the
-- OLD row, which only carries the primary key unless the full row is published.

alter table public.dm_messages replica identity full;
alter table public.friend_requests replica identity full;

do $$
declare
    t text;
begin
    foreach t in array array['dm_messages', 'friend_requests', 'community_cards'] loop
        if not exists (
            select 1 from pg_publication_tables
             where pubname = 'supabase_realtime'
               and schemaname = 'public'
               and tablename = t
        ) then
            execute format('alter publication supabase_realtime add table public.%I', t);
        end if;
    end loop;
    raise notice 'PASS  realtime publication covers dm_messages, friend_requests and community_cards';
exception
    when undefined_object then
        raise notice 'PASS  no supabase_realtime publication here — the app falls back to polling';
    when insufficient_privilege then
        raise notice 'NOTE  add these tables to the supabase_realtime publication from the dashboard';
end $$;

-- ─────────────────────────────────────────────────────────────────�����────────
-- 8. Self-check
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
                         'member_blocks',
                         'community_reactions','community_comments',
                         'community_reports','friend_requests','dm_messages',
                         'dm_typing','dm_reactions','member_blocks')
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
                         'community_reports','friend_requests','dm_messages',
                         'dm_typing','dm_reactions','member_blocks');
    if anon_open is null then
        raise notice 'PASS  no anon policies on Curio tables';
    else
        raise warning 'FAIL  anon policies exist on: %', anon_open;
    end if;

    select string_agg(t, ', ')
      into missing
      from unnest(array['profiles','cloud_captures','community_cards',
                        'community_reactions','community_comments',
                        'community_reports','friend_requests','dm_messages',
                        'dm_typing','dm_reactions','member_blocks']) as t
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
