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
    -- 0..27 = the 28 code-drawn avatars: twenty portraits (0..19) then eight
    -- cozy icons (20..27), matching SOCIAL_AVATAR_STYLE_COUNT in the app's
    -- SocialApi.kt. Widen this bound in the SAME commit that adds a style to
    -- the app's PORTRAITS/ICONS lists, or the new pick is rejected on write.
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
-- v395 — the avatar count is 28 (the twenty redrawn portraits plus the eight
-- cozy icons), so the range check is REPLACED rather than merely created: an
-- install that already carries an older bound (0..15 from the first set, 0..19
-- from the portrait-only set) must be widened, or picking a new style fails the
-- write. Idempotent — re-pasting with the new bound is a no-op.
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

-- Editing a reply: the author alone, body only, never ownership, card or
-- branch. The trigger above already pins the parent on update, and the body
-- length + content CHECKs re-run on the new text automatically. The stamp is
-- server-owned: a body change always sets edited_at, no client say in it.
alter table public.community_comments
    add column if not exists edited_at timestamptz;

create or replace function public.curio_stamp_comment_edit()
returns trigger
language plpgsql
as $$
begin
    if new.body is distinct from old.body and new.edited_at is null then
        new.edited_at := now();
    end if;
    return new;
end $$;
drop trigger if exists community_comments_stamp_edit on public.community_comments;
create trigger community_comments_stamp_edit
    before update on public.community_comments
    for each row execute function public.curio_stamp_comment_edit();

drop policy if exists cmt_update_own on public.community_comments;
create policy cmt_update_own on public.community_comments
    for update to authenticated
    using (author = auth.uid())
    with check (author = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 4c. community_comment_reactions — one heart per member per REPLY
--
-- WHY its own table: `community_reactions` is keyed by (card_id, user_id), so
-- it holds exactly one row per member per CARD. A heart on a reply cannot live
-- there — a card that has the reader's own like on it has no room for a second
-- row, and every reply under it would share that one reaction. A reply heart is
-- therefore the same shape one level down: keyed by (comment_id, user_id),
-- cascading away with its reply (and so with the reply's 24-hour card), and
-- read through the SAME gate as the reply itself — the card still live, its
-- author in Online Mode, the reader in Online Mode — so a heart can never be
-- counted on a card that has expired.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_comment_reactions (
    comment_id uuid not null references public.community_comments (id) on delete cascade,
    user_id    uuid not null default auth.uid() references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (comment_id, user_id)
);

create index if not exists community_comment_reactions_comment_idx
    on public.community_comment_reactions (comment_id);

create index if not exists community_comment_reactions_user_idx
    on public.community_comment_reactions (user_id);

alter table public.community_comment_reactions enable row level security;

drop policy if exists creac_select_visible on public.community_comment_reactions;
create policy creac_select_visible on public.community_comment_reactions
    for select to authenticated
    using (
        exists (
            select 1 from public.community_comments cm
            join public.community_cards c on c.id = cm.card_id
            where cm.id = community_comment_reactions.comment_id
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

drop policy if exists creac_insert_own on public.community_comment_reactions;
create policy creac_insert_own on public.community_comment_reactions
    for insert to authenticated
    with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.profiles me
            where me.id = auth.uid() and me.online_mode_enabled
        )
        and exists (
            select 1 from public.community_comments cm
            join public.community_cards c on c.id = cm.card_id
            where cm.id = community_comment_reactions.comment_id
              and c.expires_at > now()
        )
    );

drop policy if exists creac_update_own on public.community_comment_reactions;
create policy creac_update_own on public.community_comment_reactions
    for update to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

drop policy if exists creac_delete_own on public.community_comment_reactions;
create policy creac_delete_own on public.community_comment_reactions
    for delete to authenticated
    using (user_id = auth.uid());

-- ───────────────────────────────────────────────────────────────────────────
-- 5. community_reports — moderation queue (write-only for users)
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.community_reports (
    id         uuid primary key default gen_random_uuid(),
    card_id    uuid references public.community_cards (id) on delete cascade,
    reporter   uuid not null default auth.uid() references auth.users (id) on delete cascade,
    reason     text not null default 'other',
    note       text,
    created_at timestamptz not null default now()
);

-- A report names a CARD, a REPLY or a MEMBER — exactly one of the three, which
-- is what makes one moderation queue able to hold every kind of report. The
-- old (card_id, reporter) unique constraint is DROPPED: re-reporting has to be
-- possible (curio_file_report REFRESHES the reporter's own row instead of
-- refusing the second attempt, which used to answer with an error).
alter table public.community_reports alter column card_id drop not null;
alter table public.community_reports add column if not exists comment_id uuid
    references public.community_comments (id) on delete cascade;
alter table public.community_reports add column if not exists target_user uuid
    references auth.users (id) on delete cascade;
alter table public.community_reports add column if not exists status text not null default 'open';
alter table public.community_reports add column if not exists resolution text;
alter table public.community_reports add column if not exists handled_by uuid
    references auth.users (id) on delete set null;
alter table public.community_reports add column if not exists handled_at timestamptz;
alter table public.community_reports add column if not exists updated_at timestamptz not null default now();

alter table public.community_reports drop constraint if exists community_reports_once;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'community_reports_status_values') then
        alter table public.community_reports add constraint community_reports_status_values
            check (status in ('open', 'dismissed', 'resolved'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'community_reports_one_target') then
        alter table public.community_reports add constraint community_reports_one_target
            check (
                (case when card_id is not null then 1 else 0 end) +
                (case when comment_id is not null then 1 else 0 end) +
                (case when target_user is not null then 1 else 0 end) = 1
            );
    end if;
    if not exists (select 1 from pg_constraint where conname = 'community_reports_not_self') then
        alter table public.community_reports add constraint community_reports_not_self
            check (target_user is null or target_user <> reporter);
    end if;
end $$;

-- One row per reporter per target, enforced per target KIND (a partial unique
-- index per column): a second report refreshes that row, so a member can always
-- report again after their first one was dismissed.
create unique index if not exists community_reports_card_once
    on public.community_reports (card_id, reporter) where card_id is not null;
create unique index if not exists community_reports_comment_once
    on public.community_reports (comment_id, reporter) where comment_id is not null;
create unique index if not exists community_reports_user_once
    on public.community_reports (target_user, reporter) where target_user is not null;

create index if not exists community_reports_card_idx
    on public.community_reports (card_id);
create index if not exists community_reports_comment_idx
    on public.community_reports (comment_id);
create index if not exists community_reports_user_idx
    on public.community_reports (target_user);
create index if not exists community_reports_open_idx
    on public.community_reports (status, created_at desc);

alter table public.community_reports enable row level security;

drop policy if exists rep_insert_own on public.community_reports;
create policy rep_insert_own on public.community_reports
    for insert to authenticated
    with check (reporter = auth.uid());

drop policy if exists rep_select_own on public.community_reports;
create policy rep_select_own on public.community_reports
    for select to authenticated
    using (reporter = auth.uid());

-- 5a. The moderation team — roles, one permission per capability, and the
-- OWNER.
--
-- Privilege is database-managed: the client can only ask "may I?" and the
-- answer comes from here (curio_admin_can). Two roles exist. 'owner' bypasses
-- every switch, cannot be removed or demoted by ANYONE (so the community can
-- never lock itself out of its own controls) and is seeded below from the
-- @jugnu profile — a handle, never a UUID baked into the client. 'admin' holds
-- five independent switches, granted one by one from the Moderation screen.
create table if not exists public.community_admins (
    user_id uuid primary key references auth.users (id) on delete cascade,
    added_by uuid references auth.users (id) on delete set null,
    created_at timestamptz not null default now()
);

alter table public.community_admins add column if not exists role text not null default 'admin';
alter table public.community_admins add column if not exists can_delete_posts boolean not null default true;
alter table public.community_admins add column if not exists can_delete_replies boolean not null default true;
alter table public.community_admins add column if not exists can_handle_reports boolean not null default true;
alter table public.community_admins add column if not exists can_manage_admins boolean not null default false;
alter table public.community_admins add column if not exists can_ban_members boolean not null default false;
-- v403 — the feedback forms belong to the owner, and can be handed to an
-- admin: without it a moderator can run the report queue but cannot open the
-- form builder, read a form's answers or publish one.
alter table public.community_admins add column if not exists can_manage_forms boolean not null default false;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'community_admins_role_values') then
        alter table public.community_admins add constraint community_admins_role_values
            check (role in ('owner', 'admin'));
    end if;
end $$;

alter table public.community_admins enable row level security;

-- Any row here counts as "on the team".
create or replace function public.curio_is_community_admin(subject uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (select 1 from public.community_admins where user_id = subject)
$$;

-- …and every ACTION asks for its own permission: one of
-- 'posts' | 'replies' | 'reports' | 'admins' | 'bans' | 'forms'. Anything
-- unrecognised is a NO, and the owner is always a yes.
create or replace function public.curio_admin_can(p_permission text, subject uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce((
        select case
            when a.role = 'owner' then true
            when p_permission = 'posts' then a.can_delete_posts
            when p_permission = 'replies' then a.can_delete_replies
            when p_permission = 'reports' then a.can_handle_reports
            when p_permission = 'admins' then a.can_manage_admins
            when p_permission = 'bans' then a.can_ban_members
            when p_permission = 'forms' then a.can_manage_forms
            else false
        end
        from public.community_admins a
        where a.user_id = subject
    ), false)
$$;

revoke all on function public.curio_is_community_admin(uuid) from public, anon;
grant execute on function public.curio_is_community_admin(uuid) to authenticated;
revoke all on function public.curio_admin_can(text, uuid) from public, anon;
grant execute on function public.curio_admin_can(text, uuid) to authenticated;

drop policy if exists community_admins_select_admin on public.community_admins;
create policy community_admins_select_admin on public.community_admins
    for select to authenticated
    using (public.curio_is_community_admin());

drop policy if exists community_admins_insert_admin on public.community_admins;
create policy community_admins_insert_admin on public.community_admins
    for insert to authenticated
    with check (public.curio_admin_can('admins'));

drop policy if exists community_admins_update_admin on public.community_admins;
create policy community_admins_update_admin on public.community_admins
    for update to authenticated
    using (public.curio_admin_can('admins'))
    with check (public.curio_admin_can('admins'));

drop policy if exists community_admins_delete_admin on public.community_admins;
create policy community_admins_delete_admin on public.community_admins
    for delete to authenticated
    using (public.curio_admin_can('admins'));

drop policy if exists rep_select_admin on public.community_reports;
create policy rep_select_admin on public.community_reports
    for select to authenticated
    using (public.curio_admin_can('reports'));

drop policy if exists cards_delete_admin on public.community_cards;
create policy cards_delete_admin on public.community_cards
    for delete to authenticated
    using (public.curio_admin_can('posts'));

drop policy if exists comments_delete_admin on public.community_comments;
create policy comments_delete_admin on public.community_comments
    for delete to authenticated
    using (public.curio_admin_can('replies'));

-- Reported content has to stay READABLE to the people who review it —
-- including a card whose own 24 hours are already up, which is often exactly
-- why it was reported. These are ADDITIVE select policies (permissive
-- policies OR together), so nothing narrows for a normal member.
drop policy if exists cards_select_admin on public.community_cards;
create policy cards_select_admin on public.community_cards
    for select to authenticated
    using (public.curio_admin_can('reports'));

drop policy if exists comments_select_admin on public.community_comments;
create policy comments_select_admin on public.community_comments
    for select to authenticated
    using (public.curio_admin_can('reports'));

-- Seed the OWNER from the profile that carries the @jugnu handle. Idempotent,
-- and it PROMOTES the row if that account was already on the team.
insert into public.community_admins (
    user_id, role, can_delete_posts, can_delete_replies,
    can_handle_reports, can_manage_admins, can_ban_members
)
select p.id, 'owner', true, true, true, true, true
from public.profiles p
where lower(trim(leading '@' from coalesce(p.username, ''))) = 'jugnu'
on conflict (user_id) do update set
    role = 'owner',
    can_delete_posts = true,
    can_delete_replies = true,
    can_handle_reports = true,
    can_manage_admins = true,
    can_ban_members = true;

-- ───────────────────────────────────────────────────────────────────────────
-- 5b. profile discoverability
-- ───────────────────────────────────────────────────────────────────────────
alter table public.profiles add column if not exists discoverable boolean not null default true;

drop policy if exists prof_select_discoverable on public.profiles;
create policy prof_select_discoverable on public.profiles
    for select to authenticated
    using (discoverable and online_mode_enabled);

-- ───────────────────────────────────────────────────────────────────────────
-- 5c. friend_requests — one request per pair, either direction
-- ──────────���────────────────────────────────────────────────────────────────
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
-- ───────────────────────────────────────────────────────────────────────────
-- 5d. DM encryption — REMOVED
--
-- Messages are plain text between accepted friends again: no device keypairs,
-- no conversation-key envelopes and no per-conversation encryption mode. The
-- three tables the encryption work introduced (`dm_device_keys`,
-- `dm_key_envelopes`, `dm_conversations`), their policies, their triggers and
-- their RPCs are all gone — the tables are DROPPED at the end of this file
-- (§6c Removal), so a project that already shipped them really loses them on
-- the next paste.
--
-- The one helper kept is the canonical conversation id: the reply guard and
-- `curio_delete_dm_conversation` both still name a conversation with it.
-- ───────────────────────────────────────────────────────────────────────────
-- The canonical id of a two-person conversation: both uuid texts, in the
-- order the app sorts them. ONE definition, shared by every code path that
-- names a conversation, so a conversation can never mean two different strings
-- on two different code paths.
create or replace function public.curio_dm_conversation_of(a uuid, b uuid)
returns text
language sql
immutable
as $$
    select case when a::text < b::text
        then a::text || ':' || b::text
        else b::text || ':' || a::text end;
$$;

-- ───────────────────────────────────────────────────────────────────────────
-- 5e. dm_messages — plain text between accepted friends
--
-- The `dm_conversations` mode table that used to live here went with the
-- encryption layer (§6c Removal). Every message carries its words in `body`
-- and nothing else: no ciphertext, no nonce, no per-message mode.
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.dm_messages (
    id         uuid primary key default gen_random_uuid(),
    sender     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recipient  uuid not null references auth.users (id) on delete cascade,
    body       text not null,
    created_at timestamptz not null default now(),
    read_at    timestamptz,
    constraint dm_messages_not_self check (sender <> recipient)
);

-- Threaded replies (v3xx54): a message may answer ONE other message of the
-- same conversation. Null = a normal line. The trigger below enforces the
-- rules the client also follows: the parent must exist, sit in the SAME
-- conversation, be at most ONE level deep, and never be the message itself.
alter table public.dm_messages add column if not exists reply_to uuid references public.dm_messages (id) on delete set null;
create index if not exists dm_messages_reply_to_idx on public.dm_messages (reply_to);

create or replace function public.curio_check_dm_reply()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    parent record;
    convo text := public.curio_dm_conversation_of(new.sender, new.recipient);
    parent_convo text;
begin
    if new.reply_to is null then
        return new;
    end if;
    if new.reply_to = new.id then
        raise exception 'curio: a message cannot reply to itself';
    end if;
    select sender, recipient, reply_to into parent from public.dm_messages where id = new.reply_to;
    if not found then
        raise exception 'curio: the message being replied to no longer exists';
    end if;
    parent_convo := public.curio_dm_conversation_of(parent.sender, parent.recipient);
    if parent_convo is null or parent_convo <> convo then
        raise exception 'curio: a reply must live in the same conversation as its parent';
    end if;
    -- Threads stay one level deep: a reply to a reply quotes the root instead.
    if parent.reply_to is not null then
        raise exception 'curio: replies nest one level deep — reply to the root message';
    end if;
    return new;
end $$;
drop trigger if exists dm_messages_check_reply on public.dm_messages;
create trigger dm_messages_check_reply
    before insert on public.dm_messages
    for each row execute function public.curio_check_dm_reply();
-- The words are the message. No ciphertext, no per-message mode.
--
-- A row left over from the encryption era has no readable words in it at all,
-- so it is deleted here BEFORE `body` becomes NOT NULL and before the column
-- that held it is dropped (§6d). The constraint drops come first because both
-- of them describe shapes that no longer exist.
alter table public.dm_messages drop constraint if exists dm_messages_ciphertext_shape;
alter table public.dm_messages drop constraint if exists dm_messages_migration_state_check;
delete from public.dm_messages where body is null;
alter table public.dm_messages alter column body set not null;

drop policy if exists dm_insert_friends on public.dm_messages;
create policy dm_insert_friends on public.dm_messages
    for insert to authenticated with check (
        sender = auth.uid()
        and body is not null
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

-- Editing a sent message: the SENDER alone, and only the edit stamp and the
-- body may change. Every row is plain text now, so every row you own is
-- editable.
alter table public.dm_messages add column if not exists edited_at timestamptz;

drop policy if exists dm_edit_own on public.dm_messages;
create policy dm_edit_own on public.dm_messages
    for update to authenticated
    using (sender = auth.uid())
    with check (sender = auth.uid());

create or replace function public.curio_guard_dm_message_edit()
returns trigger
language plpgsql
as $$
begin
    if new.sender <> old.sender or new.recipient <> old.recipient then
        raise exception 'curio: a message cannot change its conversation';
    end if;
    if new.body is distinct from old.body and new.edited_at is null then
        new.edited_at := now();
    end if;
    return new;
end $$;
drop trigger if exists dm_messages_guard_edit on public.dm_messages;
create trigger dm_messages_guard_edit
    before update on public.dm_messages
    for each row execute function public.curio_guard_dm_message_edit();

-- ───────────────────────────────────────────────────────────────────────────
-- 5d2. EDITS ARE SERVER FUNCTIONS, not table writes
--
-- Editing a reply or a message used to be a direct table write, and both had
-- a failure mode that reads to a member as "editing is broken":
--
--   * a PUT on a filtered table route runs through PostgREST's upsert path,
--     which answered `column pgrst_body.id does not exist` for every reply
--     edit, and
--   * a PATCH whose row policies do not expose the row answers 204 while
--     changing NOTHING, so the app painted a success over a message that never
--     changed.
--
-- The function is now the authority for both. It checks who is asking, re-runs
-- the public-text rule (replies only — a private conversation is deliberately
-- unfiltered), stamps `edited_at` itself and RAISES a readable reason when it
-- refuses, so a client can never show a success for an edit that did not
-- happen. The row's ID never moves, so reactions and answers pointing at the
-- line stay attached to it.
-- ───────────────────────────────────────────────────────────────────────────

create or replace function public.curio_edit_comment(p_comment_id uuid, p_body text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    clean text := btrim(coalesce(p_body, ''));
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if clean = '' then
        raise exception 'curio: write something first';
    end if;
    if char_length(clean) > 400 then
        raise exception 'curio: keep a reply under 400 characters';
    end if;
    if not public.curio_text_is_clean(clean) then
        raise exception 'curio: that wording is not allowed here';
    end if;
    update public.community_comments
       set body = clean,
           edited_at = now()
     where id = p_comment_id
       and author = actor;
    if not found then
        raise exception 'curio: that reply is not yours to edit';
    end if;
end $$;

create or replace function public.curio_edit_dm_message(p_message_id uuid, p_body text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    clean text := btrim(coalesce(p_body, ''));
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if clean = '' then
        raise exception 'curio: the message is empty';
    end if;
    if char_length(clean) > 2000 then
        raise exception 'curio: keep a message under 2000 characters';
    end if;
    -- A private conversation is NOT content-filtered: the sender test below is
    -- the whole guard, exactly as it is for sending.
    update public.dm_messages
       set body = clean,
           edited_at = now()
     where id = p_message_id
       and sender = actor
       and created_at > now() - interval '24 hours';
    if not found then
        raise exception 'curio: only your own message from the last day can be edited';
    end if;
end $$;

revoke all on function public.curio_edit_comment(uuid, text) from public, anon;
grant execute on function public.curio_edit_comment(uuid, text) to authenticated;
revoke all on function public.curio_edit_dm_message(uuid, text) from public, anon;
grant execute on function public.curio_edit_dm_message(uuid, text) to authenticated;

-- A participant may clear the entire two-person thread. The client scopes the
-- DELETE to both directions, while this policy prevents deleting somebody
-- else's conversation.
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

-- Per-user "delete for me" marker. It hides a thread from one inbox without
-- destroying the other participant's history.
create table if not exists public.dm_conversation_hidden (
    user_id uuid not null references auth.users(id) on delete cascade,
    other_user_id uuid not null references auth.users(id) on delete cascade,
    hidden_at timestamptz not null default now(),
    primary key (user_id, other_user_id),
    check (user_id <> other_user_id)
);
alter table public.dm_conversation_hidden enable row level security;
drop policy if exists dm_hidden_owner on public.dm_conversation_hidden;
create policy dm_hidden_owner on public.dm_conversation_hidden
    for all to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());
create index if not exists dm_hidden_other_idx
    on public.dm_conversation_hidden (other_user_id, user_id);

--------------------------------------------------------------------------------
-- 5h. Delete a conversation — for me, or for both of us
--
-- "Delete for me" is the per-user marker above (`dm_conversation_hidden`): it
-- hides the thread from ONE inbox and destroys nobody else's history. A new
-- message from the other person is newer than the marker, so the thread comes
-- back on its own — hiding a chat can never swallow the reply that follows it.
--
-- "Delete for both of us" has to remove rows that belong to the OTHER person
-- too, which no table grant can express without letting a client delete
-- arbitrary messages. So it is a security-definer function with one narrow
-- contract: the caller must be one of the two parties and the rows it touches
-- are only ever the pair's own messages, hidden markers and conversation row.
-- It answers how many messages went, so the app can say something honest.
--
-- Deliberately NOT gated on the friendship still existing: removing a friend
-- is exactly when someone wants to clear the thread, and refusing then would
-- leave them with a chat they cannot get rid of. The pair must simply have a
-- conversation to delete — the friendship gate belongs on WRITING.
create or replace function public.curio_delete_dm_conversation(other uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    me uuid := auth.uid();
    removed integer;
begin
    if me is null then
        raise exception 'curio: sign in to delete a conversation';
    end if;
    if other is null or other = me then
        raise exception 'curio: that is not a conversation';
    end if;
    if not exists (
        select 1 from public.dm_messages
         where (sender = me and recipient = other)
            or (sender = other and recipient = me)
    ) then
        raise exception 'curio: there is no conversation to delete';
    end if;

    delete from public.dm_messages
     where (sender = me and recipient = other)
        or (sender = other and recipient = me);
    get diagnostics removed = row_count;

    delete from public.dm_conversation_hidden
     where (user_id = me and other_user_id = other)
        or (user_id = other and other_user_id = me);

    return removed;
end $$;

revoke all on function public.curio_delete_dm_conversation(uuid) from public, anon;
grant execute on function public.curio_delete_dm_conversation(uuid) to authenticated;

-- Realtime delivery is required for message, reaction, request, and typing
-- updates. The checks keep this safe to paste repeatedly after all tables exist.
do $$
begin
    if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'dm_messages') then
        alter publication supabase_realtime add table public.dm_messages;
    end if;
    if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'dm_reactions') then
        alter publication supabase_realtime add table public.dm_reactions;
    end if;
    if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'friend_requests') then
        alter publication supabase_realtime add table public.friend_requests;
    end if;
    if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'dm_typing') then
        alter publication supabase_realtime add table public.dm_typing;
    end if;
end $$;

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
-- accents dropped, look-alikes mapped (0→o, 3→e, @���a, $→s, |→i …) and every
-- remaining separator or unknown character becomes a word boundary — so spaced
-- single-letter evasions can still be joined while ordinary words keep their
-- boundaries. The map is APPLIED IN THE
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
        '[^a-z]+', ' ', 'g'
    )
    from folded;
$$;

create or replace function public.curio_text_is_clean(raw text)
returns boolean
language plpgsql
immutable
as $$
declare
    -- Public-safety terms. They are matched as complete normalised words so an
    -- innocent longer word is never refused because it contains a substring.
    anywhere text[] := array[
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
    -- Additional public-safety terms, also matched as complete normalised words.
    wholeword text[] := array[
        'ass','arse','asses','dumbass','jackass','kickass','sex','sexy',
        'sexual','sexist','sextoy','sextoys','nude','nudes','naked','boob',
        'boobs','tit','tits','titties','cum','anal','rape','raped','raping',
        'rapist','rapists','dic','dick','dicks','cock','cocks','hoe','hoes',
        'horny','wank','wanker','bugger','fag','fags','homo','escort',
        'escorts','coon','paki','kys'
    ];
    folded    text;
    merged    text;
    bad       text;
    pass      integer;
begin
    folded := public.curio_normalize_text(raw);
    if length(regexp_replace(folded, '[^a-z]', '', 'g')) < 3 then
        return true;
    end if;

    -- Word view, with runs of SINGLE letters merged ("f u c k" -> "fuck").
    merged := array_to_string(regexp_split_to_table(folded, '[^a-z]+'), ' ');
    for pass in 1..12 loop
        merged := regexp_replace(merged, '\y([a-z]) ([a-z])\y', '\1\2', 'g');
    end loop;
    foreach bad in array (anywhere || wholeword) loop
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
-- 6b. Moderation — bans, the audit trail, and the actions themselves
--
-- A ban HIDES a member (content gone, posting refused); it never touches their
-- account, so nothing has to be undone to let them back in. Every action
-- records WHO did it, TO WHAT and WHY: a removal without a reason is not a
-- moderation action, it is an accident with no way to explain itself later.
-- ───────────────────────────────────────────────────────────────────────────

-- The ban is a column on the member's own profile (readable by them, so the
-- app can say what happened) and the enforcement lives in the policies.
alter table public.profiles add column if not exists banned boolean not null default false;
alter table public.profiles add column if not exists banned_at timestamptz;
alter table public.profiles add column if not exists banned_by uuid
    references auth.users (id) on delete set null;
alter table public.profiles add column if not exists ban_reason text;

-- Security definer: a banned member's content must be filtered by a test that
-- reads the profile even where row policies would hide it.
create or replace function public.curio_member_hidden(subject uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce((select p.banned from public.profiles p where p.id = subject), false)
$$;

revoke all on function public.curio_member_hidden(uuid) from public, anon;
grant execute on function public.curio_member_hidden(uuid) to authenticated;

-- RESTRICTIVE policies AND with the existing permissive ones, so a hidden
-- member's cards and replies disappear without touching the read rules
-- themselves. A moderator reviewing a report still sees the content (that is
-- how they decide whether to lift the ban), and the author cannot post while
-- hidden.
drop policy if exists cards_hide_hidden_author on public.community_cards;
create policy cards_hide_hidden_author on public.community_cards
    as restrictive
    for select to authenticated
    using (
        not public.curio_member_hidden(owner)
        or public.curio_admin_can('reports')
    );

drop policy if exists comments_hide_hidden_author on public.community_comments;
create policy comments_hide_hidden_author on public.community_comments
    as restrictive
    for select to authenticated
    using (
        not public.curio_member_hidden(author)
        or public.curio_admin_can('reports')
    );

drop policy if exists cards_block_hidden_author on public.community_cards;
create policy cards_block_hidden_author on public.community_cards
    as restrictive
    for insert to authenticated
    with check (not public.curio_member_hidden(auth.uid()));

drop policy if exists comments_block_hidden_author on public.community_comments;
create policy comments_block_hidden_author on public.community_comments
    as restrictive
    for insert to authenticated
    with check (not public.curio_member_hidden(auth.uid()));

-- The record of every moderation decision.
create table if not exists public.moderation_actions (
    id           uuid primary key default gen_random_uuid(),
    actor        uuid references auth.users (id) on delete set null,
    action       text not null,
    target_kind  text not null,
    target_id    uuid,
    target_owner uuid,
    reason       text,
    note         text,
    created_at   timestamptz not null default now()
);

alter table public.moderation_actions enable row level security;

drop policy if exists moderation_actions_select_admin on public.moderation_actions;
create policy moderation_actions_select_admin on public.moderation_actions
    for select to authenticated
    using (public.curio_admin_can('reports'));

grant select on public.moderation_actions to authenticated;
revoke all on public.moderation_actions from anon;

/**
 * Files a report against a CARD, a REPLY or a MEMBER.
 *
 * One row per reporter per target: reporting the same thing again REFRESHES
 * that row (reason, note, back to 'open') instead of failing on a unique
 * constraint, which is what made "report again" impossible. Returns the id.
 */
create or replace function public.curio_file_report(
    p_kind text,
    p_target uuid,
    p_reason text,
    p_note text default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_reason text := coalesce(nullif(btrim(coalesce(p_reason, '')), ''), 'other');
    v_note text := nullif(btrim(coalesce(p_note, '')), '');
    v_card uuid := null;
    v_comment uuid := null;
    v_user uuid := null;
    v_report uuid;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if p_target is null then
        raise exception 'curio: nothing to report';
    end if;
    if p_kind = 'card' then
        v_card := p_target;
        if not exists (select 1 from public.community_cards where id = p_target) then
            raise exception 'curio: that post is gone';
        end if;
    elsif p_kind = 'comment' then
        v_comment := p_target;
        if not exists (select 1 from public.community_comments where id = p_target) then
            raise exception 'curio: that reply is gone';
        end if;
    elsif p_kind = 'user' then
        v_user := p_target;
        if p_target = actor then
            raise exception 'curio: you cannot report yourself';
        end if;
        if not exists (select 1 from public.profiles where id = p_target) then
            raise exception 'curio: that member is gone';
        end if;
    else
        raise exception 'curio: unknown report target';
    end if;

    select r.id into v_report
      from public.community_reports r
     where r.reporter = actor
       and r.card_id is not distinct from v_card
       and r.comment_id is not distinct from v_comment
       and r.target_user is not distinct from v_user
     limit 1;

    if v_report is null then
        insert into public.community_reports (card_id, comment_id, target_user, reporter, reason, note)
        values (v_card, v_comment, v_user, actor, left(v_reason, 40), left(v_note, 500))
        returning id into v_report;
    else
        update public.community_reports
           set reason = left(v_reason, 40),
               note = left(v_note, 500),
               status = 'open',
               resolution = null,
               handled_by = null,
               handled_at = null,
               updated_at = now()
         where id = v_report;
    end if;
    return v_report;
end $$;

/** Hides a member and stamps why. Content-only: the account stays usable. */
create or replace function public.curio_moderate_hide_member(
    p_user_id uuid,
    p_hidden boolean,
    p_reason text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_reason text := nullif(btrim(coalesce(p_reason, '')), '');
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('bans') then
        raise exception 'curio: you do not have permission to hide members';
    end if;
    if p_hidden and v_reason is null then
        raise exception 'curio: a reason is required';
    end if;
    update public.profiles
       set banned = p_hidden,
           banned_at = case when p_hidden then now() else null end,
           banned_by = case when p_hidden then actor else null end,
           ban_reason = case when p_hidden then left(v_reason, 300) else null end
     where id = p_user_id;
    if not found then
        raise exception 'curio: that member is gone';
    end if;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason)
    values (actor, case when p_hidden then 'hide_member' else 'unhide_member' end, 'user', p_user_id, p_user_id, v_reason);
end $$;

/** Removes one card, with the reason the author will never be told by accident. */
create or replace function public.curio_moderate_remove_card(
    p_card_id uuid,
    p_reason text,
    p_note text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_reason text := nullif(btrim(coalesce(p_reason, '')), '');
    v_owner uuid;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('posts') then
        raise exception 'curio: you do not have permission to remove posts';
    end if;
    if v_reason is null then
        raise exception 'curio: a reason is required';
    end if;
    select owner into v_owner from public.community_cards where id = p_card_id;
    if v_owner is null then
        raise exception 'curio: that post is already gone';
    end if;
    delete from public.community_cards where id = p_card_id;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason, note)
    values (actor, 'remove_post', 'card', p_card_id, v_owner, left(v_reason, 300), left(nullif(btrim(coalesce(p_note, '')), ''), 500));
end $$;

/** Removes one reply, with the same rule: no reason, no removal. */
create or replace function public.curio_moderate_remove_comment(
    p_comment_id uuid,
    p_reason text,
    p_note text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_reason text := nullif(btrim(coalesce(p_reason, '')), '');
    v_author uuid;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('replies') then
        raise exception 'curio: you do not have permission to remove replies';
    end if;
    if v_reason is null then
        raise exception 'curio: a reason is required';
    end if;
    select author into v_author from public.community_comments where id = p_comment_id;
    if v_author is null then
        raise exception 'curio: that reply is already gone';
    end if;
    delete from public.community_comments where id = p_comment_id;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason, note)
    values (actor, 'remove_reply', 'comment', p_comment_id, v_author, left(v_reason, 300), left(nullif(btrim(coalesce(p_note, '')), ''), 500));
end $$;

/**
 * Works one queue row: dismiss it, remove the content it named, or hide the
 * member it named. The permission test is per ACTION — handling the queue is
 * not the same right as removing what it points at.
 */
create or replace function public.curio_handle_report(
    p_report uuid,
    p_action text,
    p_reason text default null,
    p_note text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_reason text := nullif(btrim(coalesce(p_reason, '')), '');
    v_card uuid;
    v_comment uuid;
    v_user uuid;
    v_author uuid;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('reports') then
        raise exception 'curio: you do not have permission to handle reports';
    end if;
    select card_id, comment_id, target_user into v_card, v_comment, v_user
      from public.community_reports where id = p_report;
    if not found then
        raise exception 'curio: that report is gone';
    end if;
    if p_action = 'dismiss' then
        if v_reason is null then
            v_reason := 'no action needed';
        end if;
        update public.community_reports
           set status = 'dismissed', resolution = left(v_reason, 300),
               handled_by = actor, handled_at = now(), updated_at = now()
         where id = p_report;
        insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason, note)
        values (actor, 'dismiss_report', 'report', p_report, null, left(v_reason, 300), left(nullif(btrim(coalesce(p_note, '')), ''), 500));
    elsif p_action = 'remove_content' then
        if v_card is not null then
            perform public.curio_moderate_remove_card(v_card, coalesce(v_reason, 'removed after a report'), p_note);
        elsif v_comment is not null then
            perform public.curio_moderate_remove_comment(v_comment, coalesce(v_reason, 'removed after a report'), p_note);
        else
            raise exception 'curio: this report names a member, hide them instead';
        end if;
        update public.community_reports
           set status = 'resolved', resolution = left(coalesce(v_reason, 'content removed'), 300),
               handled_by = actor, handled_at = now(), updated_at = now()
         where id = p_report;
    elsif p_action = 'hide_author' then
        if v_card is not null then
            select owner into v_author from public.community_cards where id = v_card;
        elsif v_comment is not null then
            select author into v_author from public.community_comments where id = v_comment;
        else
            v_author := v_user;
        end if;
        if v_author is null then
            raise exception 'curio: the member this names is gone';
        end if;
        perform public.curio_moderate_hide_member(v_author, true, coalesce(v_reason, 'hidden after a report'));
        update public.community_reports
           set status = 'resolved', resolution = left(coalesce(v_reason, 'member hidden'), 300),
               handled_by = actor, handled_at = now(), updated_at = now()
         where id = p_report;
    elsif p_action = 'reopen' then
        update public.community_reports
           set status = 'open', resolution = null, handled_by = null, handled_at = null, updated_at = now()
         where id = p_report;
    else
        raise exception 'curio: unknown moderation action';
    end if;
end $$;

/**
 * Grants or edits one admin. The owner row can never be touched from here, and
 * only someone with the 'admins' permission may call this at all.
 */
-- The eight-argument signature (v403 added the forms switch). The seven-
-- argument one is dropped first: `create or replace` with a different
-- signature only ADDS an overload, which would leave a version of this
-- function behind that silently forgot every form permission.
drop function if exists public.curio_set_community_admin(
    uuid, text, boolean, boolean, boolean, boolean, boolean
);

create or replace function public.curio_set_community_admin(
    p_user_id uuid,
    p_role text,
    p_can_delete_posts boolean,
    p_can_delete_replies boolean,
    p_can_handle_reports boolean,
    p_can_manage_admins boolean,
    p_can_ban_members boolean,
    p_can_manage_forms boolean default false
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_role text := case when p_role = 'owner' then 'owner' else 'admin' end;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('admins') then
        raise exception 'curio: you do not have permission to manage admins';
    end if;
    if exists (select 1 from public.community_admins where user_id = p_user_id and role = 'owner') then
        raise exception 'curio: the owner cannot be changed';
    end if;
    if v_role = 'owner' and exists (select 1 from public.community_admins where role = 'owner') then
        raise exception 'curio: there is already an owner';
    end if;
    insert into public.community_admins (
        user_id, added_by, role, can_delete_posts, can_delete_replies,
        can_handle_reports, can_manage_admins, can_ban_members, can_manage_forms
    )
    values (
        p_user_id, actor, v_role,
        coalesce(p_can_delete_posts, true),
        coalesce(p_can_delete_replies, true),
        coalesce(p_can_handle_reports, true),
        coalesce(p_can_manage_admins, false),
        coalesce(p_can_ban_members, false),
        coalesce(p_can_manage_forms, false)
    )
    on conflict (user_id) do update set
        role = excluded.role,
        can_delete_posts = excluded.can_delete_posts,
        can_delete_replies = excluded.can_delete_replies,
        can_handle_reports = excluded.can_handle_reports,
        can_manage_admins = excluded.can_manage_admins,
        can_ban_members = excluded.can_ban_members,
        can_manage_forms = excluded.can_manage_forms;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner)
    values (actor, 'set_admin', 'user', p_user_id, p_user_id);
end $$;

/** Takes one admin off the team. The owner can never be removed. */
create or replace function public.curio_remove_community_admin(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('admins') then
        raise exception 'curio: you do not have permission to manage admins';
    end if;
    if exists (select 1 from public.community_admins where user_id = p_user_id and role = 'owner') then
        raise exception 'curio: the owner cannot be removed';
    end if;
    delete from public.community_admins where user_id = p_user_id;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner)
    values (actor, 'remove_admin', 'user', p_user_id, p_user_id);
end $$;

/**
 * Deletes every report the team has already decided on.
 *
 * The queue is a work list, not an archive: once a report has been dismissed or
 * resolved, its row is finished business, and the queue keeps nothing but the
 * audit trail in `moderation_actions` (which is where "what happened to this
 * report" is answered from anyway). Open reports are NEVER touched — the action
 * takes them out of the count it deletes, so a sweep can never throw away a
 * report nobody has read yet.
 *
 * Returns how many rows went, so the app can say what it did.
 */
create or replace function public.curio_moderate_delete_reports()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_deleted integer := 0;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('reports') then
        raise exception 'curio: you do not have permission to handle reports';
    end if;
    delete from public.community_reports where status <> 'open';
    get diagnostics v_deleted = row_count;
    if v_deleted > 0 then
        insert into public.moderation_actions (actor, action, target_kind, reason)
        values (actor, 'delete_handled_reports', 'report',
                v_deleted || ' handled reports deleted from the queue');
    end if;
    return v_deleted;
end $$;

revoke all on function public.curio_moderate_delete_reports() from public, anon;
grant execute on function public.curio_moderate_delete_reports() to authenticated;

revoke all on function public.curio_file_report(text, uuid, text, text) from public, anon;
grant execute on function public.curio_file_report(text, uuid, text, text) to authenticated;
revoke all on function public.curio_handle_report(uuid, text, text, text) from public, anon;
grant execute on function public.curio_handle_report(uuid, text, text, text) to authenticated;
revoke all on function public.curio_moderate_remove_card(uuid, text, text) from public, anon;
grant execute on function public.curio_moderate_remove_card(uuid, text, text) to authenticated;
revoke all on function public.curio_moderate_remove_comment(uuid, text, text) from public, anon;
grant execute on function public.curio_moderate_remove_comment(uuid, text, text) to authenticated;
revoke all on function public.curio_moderate_hide_member(uuid, boolean, text) from public, anon;
grant execute on function public.curio_moderate_hide_member(uuid, boolean, text) to authenticated;
revoke all on function public.curio_set_community_admin(uuid, text, boolean, boolean, boolean, boolean, boolean, boolean) from public, anon;
grant execute on function public.curio_set_community_admin(uuid, text, boolean, boolean, boolean, boolean, boolean, boolean) to authenticated;
revoke all on function public.curio_remove_community_admin(uuid) from public, anon;
grant execute on function public.curio_remove_community_admin(uuid) to authenticated;

-- ───────────────────────────────────────────────────────────────────────────
-- 6d. Removal — the DM encryption layer is gone
--
-- DM encryption was withdrawn: every chat is plain text between two accepted
-- friends again. Everything it added is removed here, on the way in, so a
-- project that already shipped the encryption tables really loses them —
-- dropping a table in the middle of the file would have been undone by that
-- table's own `create … if not exists` above it, which is why the removal
-- lives at the END of the script.
--
-- Order matters inside the block: trigger, then table, then function. The
-- `to_regclass` guards keep the whole thing safe on a project that never had
-- these tables (a fresh paste) as well as on one that did.
-- ───────────────────────────────────────────────────────────────────────────
do $$
begin
    if to_regclass('public.dm_key_envelopes') is not null then
        drop trigger if exists dm_key_envelopes_validate on public.dm_key_envelopes;
        drop table public.dm_key_envelopes;
    end if;
    if to_regclass('public.dm_device_keys') is not null then
        drop table public.dm_device_keys;
    end if;
    if to_regclass('public.dm_conversations') is not null then
        drop trigger if exists dm_conversations_pin_parties on public.dm_conversations;
        drop table public.dm_conversations;
    end if;
end $$;

drop function if exists public.curio_dm_conversation_peer(text, uuid);
drop function if exists public.curio_validate_dm_envelope();
drop function if exists public.curio_pin_dm_conversation_parties();
-- The delivery-mode trigger lives on dm_messages, which always exists, so
-- the `if exists` guard is enough; it must go BEFORE its function or the
-- drop answers 2BP01 (cannot drop function ... other objects depend on it).
drop trigger if exists dm_messages_enforce_delivery_mode on public.dm_messages;
drop function if exists public.curio_enforce_dm_delivery_mode();
drop trigger if exists dm_messages_enforce_envelopes on public.dm_messages;
drop function if exists public.curio_enforce_dm_message_envelopes();
drop function if exists public.curio_retire_dm_device(text);
drop function if exists public.curio_publish_dm_device(text, text);
drop function if exists public.curio_dm_missing_envelopes(text, integer);

-- And the columns that only ever held ciphertext (plus the mode that said
-- whether a row was encrypted) are gone. The row cleanup and the constraint
-- drops that have to precede them already ran in §5e, where `body` became NOT
-- NULL — this is the last step, after every reader of those columns is gone.
alter table public.dm_messages drop column if exists ciphertext;
alter table public.dm_messages drop column if exists nonce;
alter table public.dm_messages drop column if exists encryption_version;
alter table public.dm_messages drop column if exists migration_state;

-- ───────────────────────────────────────────────────────────────────────────
-- 6f. FEEDBACK FORMS — the team asks the members, and the members answer
-- ───────────────────────────────────────────────────────────────────────────
-- What this is: one question sheet (up to 5 questions, up to 4 options each),
-- written in the app's moderation room, TESTED as many times as the team
-- likes, then PUBLISHED. A published form is live to every member who has
-- Online Mode on: it shows on Home and lives in Support for later.
--
-- SECURITY / PRIVACY RULES (read before changing anything here):
--  • ANONYMOUS. `feedback_answers` carries NO user id, no device id and no
--    session of any kind — there is deliberately nothing here that could link
--    an answer to an account, and nothing may ever be added. The app keeps a
--    local "already answered" flag per form; the server cannot (and must not)
--    enforce one-answer-per-member, because enforcing it is exactly what would
--    make the answers traceable.
--  • WRITE-ONLY FOR MEMBERS: a member with a signed-in token may INSERT an
--    answer or a skip while the form is LIVE, and may never SELECT either
--    table. Reading is the team's (`curio_admin_can('forms')`).
--  • WHO MAY WRITE A FORM: `curio_admin_can('forms')` — the owner always, and
--    any admin the owner gave the forms switch. The database is the guard; the
--    client's copy of the rule only decides what to draw.
--  • CADENCE: one publication per 14 days. Testing never counts (a test form
--    is `is_test` and touches neither the clock nor the live form). The owner
--    may OVERRIDE the clock, and publishing closes whichever form was live —
--    only the newest form is ever live at once.
--
-- The shape of one row of `questions` (an array, 1..5 of these):
--   {"id":"q1","prompt":"What should Curio do next?","kind":"SINGLE",
--    "options":["Reading","Writing","Social","Something else"]}
-- kind is SINGLE (one option), MULTI (any of them) or TEXT (one short written
-- answer). The same `id` keys the answer:
--   {"id":"q1","option":2} | {"id":"q2","options":[0,3]} | {"id":"q3","text":"…"}
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.feedback_forms (
    id           uuid primary key default gen_random_uuid(),
    title        text not null check (char_length(btrim(title)) between 3 and 120),
    -- The line above the questions ("Two minutes, and it shapes what we build").
    intro        text not null default '' check (char_length(intro) <= 400),
    -- 1..5 question objects, see the shape above.
    questions    jsonb not null,
    -- draft  = being written (only the team sees it)
    -- live   = published (every member sees it)
    -- closed = it was live and is not any more
    status       text not null default 'draft' check (status in ('draft', 'live', 'closed')),
    -- A test never counts toward the 14-day clock and is never the live form.
    is_test      boolean not null default false,
    created_by   uuid references auth.users (id) on delete set null,
    created_at   timestamptz not null default now(),
    published_at timestamptz,
    closed_at    timestamptz,
    check (jsonb_typeof(questions) = 'array'),
    check (jsonb_array_length(questions) between 1 and 5)
);

create index if not exists feedback_forms_live_idx
    on public.feedback_forms (published_at desc)
    where status = 'live';

-- One member's answers to one form. NO identity columns, by design.
create table if not exists public.feedback_answers (
    id         uuid primary key default gen_random_uuid(),
    form_id    uuid not null references public.feedback_forms (id) on delete cascade,
    answers    jsonb not null,
    created_at timestamptz not null default now(),
    check (jsonb_typeof(answers) = 'array'),
    check (jsonb_array_length(answers) between 1 and 5)
);

create index if not exists feedback_answers_form_idx
    on public.feedback_answers (form_id, created_at desc);

-- "Not now" and "never show me this again", counted, not attributed.
create table if not exists public.feedback_skips (
    id         uuid primary key default gen_random_uuid(),
    form_id    uuid not null references public.feedback_forms (id) on delete cascade,
    kind       text not null check (kind in ('skip', 'never')),
    created_at timestamptz not null default now()
);

create index if not exists feedback_skips_form_idx
    on public.feedback_skips (form_id, kind);

alter table public.feedback_forms enable row level security;
alter table public.feedback_answers enable row level security;
alter table public.feedback_skips enable row level security;

-- ── who may read a form ────────────────────────────────────────────────────
-- A PUBLISHED form (live and not a test) is readable by every signed-in
-- member, which is the whole point; a test is readable by the team only, even
-- while it is "live" so they can walk it exactly as a member would, so the
-- walkthrough can never reach the members themselves.
drop policy if exists ff_select_live_or_team on public.feedback_forms;
create policy ff_select_live_or_team on public.feedback_forms
    for select to authenticated
    using (
        (status = 'live' and is_test = false)
        or public.curio_admin_can('forms')
    );

drop policy if exists ff_insert_team on public.feedback_forms;
create policy ff_insert_team on public.feedback_forms
    for insert to authenticated
    with check (public.curio_admin_can('forms'));

drop policy if exists ff_update_team on public.feedback_forms;
create policy ff_update_team on public.feedback_forms
    for update to authenticated
    using (public.curio_admin_can('forms'))
    with check (public.curio_admin_can('forms'));

drop policy if exists ff_delete_team on public.feedback_forms;
create policy ff_delete_team on public.feedback_forms
    for delete to authenticated
    using (public.curio_admin_can('forms'));

-- ── answers: insert while live, read only as the team ──────────────────────
drop policy if exists fa_insert_while_live on public.feedback_answers;
create policy fa_insert_while_live on public.feedback_answers
    for insert to authenticated
    with check (
        exists (
            select 1 from public.feedback_forms f
             where f.id = form_id
               and (
                    (f.status = 'live' and f.is_test = false)
                    or public.curio_admin_can('forms')
               )
        )
    );

drop policy if exists fa_select_team on public.feedback_answers;
create policy fa_select_team on public.feedback_answers
    for select to authenticated
    using (public.curio_admin_can('forms'));

drop policy if exists fs_insert_while_live on public.feedback_skips;
create policy fs_insert_while_live on public.feedback_skips
    for insert to authenticated
    with check (
        exists (
            select 1 from public.feedback_forms f
             where f.id = form_id
               and f.status = 'live'
               and f.is_test = false
        )
    );

drop policy if exists fs_select_team on public.feedback_skips;
create policy fs_select_team on public.feedback_skips
    for select to authenticated
    using (public.curio_admin_can('forms'));

-- ── publishing ────────────────────────────────────────────────────────────
-- The ONE door a form goes live through: it checks the team's forms switch,
-- enforces the 14-day clock (unless the caller OVERRIDES and is allowed to),
-- closes whatever was live, and stamps `published_at`. `p_force` is the
-- owner's override; it is re-checked here rather than trusted from the client.
create or replace function public.curio_publish_feedback_form(
    p_form_id uuid,
    p_force boolean default false
)
returns timestamptz
language plpgsql
security definer
set search_path = public
as $$
declare
    v_test    boolean;
    v_last    timestamptz;
    v_days    int;
    v_at      timestamptz;
begin
    if not public.curio_admin_can('forms') then
        raise exception 'curio: the forms belong to the owner';
    end if;
    select is_test into v_test from public.feedback_forms where id = p_form_id;
    if v_test is null then
        raise exception 'curio: no such form';
    end if;

    if v_test then
        -- A test is published as a test: it never becomes the live form and
        -- never touches the clock, so the team can walk through it as often
        -- as they like.
        update public.feedback_forms
           set status = 'live', published_at = now(), closed_at = null
         where id = p_form_id
        returning published_at into v_at;
        return v_at;
    end if;

    if not p_force then
        select max(published_at) into v_last
          from public.feedback_forms
         where is_test = false and published_at is not null and id <> p_form_id;
        if v_last is not null and v_last > now() - interval '14 days' then
            v_days := ceil(extract(epoch from (now() - v_last)) / 86400.0)::int;
            raise exception 'curio: a form went out % days ago, so the next one can go out in % days',
                v_days, greatest(14 - v_days, 1);
        end if;
    end if;

    -- Only the newest form is ever live: this one replaces the last.
    update public.feedback_forms
       set status = 'closed', closed_at = now()
     where status = 'live' and is_test = false and id <> p_form_id;

    update public.feedback_forms
       set status = 'live', published_at = now(), closed_at = null
     where id = p_form_id
    returning published_at into v_at;
    return v_at;
end $$;

create or replace function public.curio_close_feedback_form(p_form_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.curio_admin_can('forms') then
        raise exception 'curio: the forms belong to the owner';
    end if;
    update public.feedback_forms
       set status = 'closed', closed_at = now()
     where id = p_form_id and status = 'live';
end $$;

-- ── the tally ─────────────────────────────────────────────────────────────
-- One row per question per option, plus one row (`option_index = -1`) for the
-- written answers and for the skips, so the results page needs ONE call and
-- never downloads the answers themselves. Team only.
create or replace function public.curio_feedback_tally(p_form_id uuid)
returns table (
    question_id  text,
    kind         text,
    option_index int,
    total        int
)
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    q      jsonb;
    v_kind text;
    v_id   text;
    i      int;
    opts   int;
    n      int;
begin
    if not public.curio_admin_can('forms') then
        raise exception 'curio: the answers are the team''s';
    end if;

    for q in
        select jsonb_array_elements(questions)
          from public.feedback_forms
         where id = p_form_id
    loop
        v_id := q->>'id';
        v_kind := upper(coalesce(q->>'kind', 'SINGLE'));
        if v_kind = 'TEXT' then
            select count(*) into n
              from public.feedback_answers a
             where a.form_id = p_form_id
               and exists (
                    select 1 from jsonb_array_elements(a.answers) e
                     where e->>'id' = v_id
                       and btrim(coalesce(e->>'text', '')) <> ''
               );
            question_id  := v_id;
            kind         := v_kind;
            option_index := -1;
            total        := n;
            return next;
        else
            opts := coalesce(jsonb_array_length(q->'options'), 0);
            for i in 0 .. (opts - 1) loop
                select count(*) into n
                  from public.feedback_answers a
                 where a.form_id = p_form_id
                   and exists (
                        select 1 from jsonb_array_elements(a.answers) e
                         where e->>'id' = v_id
                           and (
                                (e ? 'option' and coalesce(e->>'option', '') ~ '^[0-9]+$'
                                     and (e->>'option')::int = i)
                             or (e ? 'options' and e->'options' @> jsonb_build_array(i))
                           )
                   );
                question_id  := v_id;
                kind         := v_kind;
                option_index := i;
                total        := n;
                return next;
            end loop;
        end if;
    end loop;

    -- How many members chose not to take it: shown beside the form.
    for v_kind, n in
        select sk.kind, count(*)::int
          from public.feedback_skips sk
         where sk.form_id = p_form_id
         group by sk.kind
    loop
        question_id  := '__' || v_kind;
        kind         := upper(v_kind);
        option_index := -1;
        total        := n;
        return next;
    end loop;
end $$;

revoke all on function public.curio_publish_feedback_form(uuid, boolean) from public, anon;
grant execute on function public.curio_publish_feedback_form(uuid, boolean) to authenticated;
revoke all on function public.curio_close_feedback_form(uuid) from public, anon;
grant execute on function public.curio_close_feedback_form(uuid) to authenticated;
revoke all on function public.curio_feedback_tally(uuid) from public, anon;
grant execute on function public.curio_feedback_tally(uuid) to authenticated;

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

-- ───────────────────────────────────────────────────────────────────────────
-- member_follows (v389) — one row per follow, the Follow button's whole
-- server side. A block outranks a follow (the insert is refused if the target
-- blocks the follower), self-follows are refused, and the ban guard covers
-- the table like every other social one (a read-only or social ban refuses
-- new follows; existing rows just sit there until the ban lapses).
-- ───────────────────────────────────────────────────────────────────────────
create table if not exists public.member_follows (
    follower   uuid not null default auth.uid() references auth.users (id) on delete cascade,
    followed   uuid not null references auth.users (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (follower, followed),
    constraint member_follows_not_self check (follower <> followed)
);

create index if not exists member_follows_followed_idx on public.member_follows (followed);

alter table public.member_follows enable row level security;

drop policy if exists follow_select_all on public.member_follows;
create policy follow_select_all on public.member_follows
    for select to authenticated
    using (true);

drop policy if exists follow_insert_own on public.member_follows;
create policy follow_insert_own on public.member_follows
    for insert to authenticated
    with check (
        follower = auth.uid()
        and follower <> followed
        and not exists (
            select 1 from public.member_blocks b
            where b.blocker = followed and b.blocked = auth.uid()
        )
    );

drop policy if exists follow_delete_own on public.member_follows;
create policy follow_delete_own on public.member_follows
    for delete to authenticated
    using (follower = auth.uid());

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
-- ─────��──��──────────────────────────────────────────────────────────────────
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
-- REPLICA IDENTITY FULL on the tables whose UPDATES and DELETES are filtered
-- (a read receipt, an answered request, a reaction taken back): a filtered
-- subscription is matched against the OLD row, which only carries the primary
-- key unless the full row is published. dm_typing is deliberately NOT changed:
-- the open conversation watches it for INSERT/UPDATE only, and its primary key
-- already carries the `sender` its filter matches on. All four message-side
-- tables (dm_messages, dm_typing, dm_reactions, friend_requests) are added to
-- the publication above.

alter table public.dm_messages replica identity full;
alter table public.friend_requests replica identity full;
alter table public.dm_reactions replica identity full;

do $$
declare
    t text;
begin
    foreach t in array array['dm_messages', 'friend_requests', 'community_cards', 'community_comments'] loop
        if not exists (
            select 1 from pg_publication_tables
             where pubname = 'supabase_realtime'
               and schemaname = 'public'
               and tablename = t
        ) then
            execute format('alter publication supabase_realtime add table public.%I', t);
        end if;
    end loop;
    raise notice 'PASS  realtime publication covers messages, requests, cards and comments';
exception
    when undefined_object then
        raise notice 'PASS  no supabase_realtime publication here — the app falls back to polling';
    when insufficient_privilege then
        raise notice 'NOTE  add these tables to the supabase_realtime publication from the dashboard';
end $$;

-- ───────────────────────────────────────────────────────────────────────────
-- 6e. THE BAN LADDER — four tiers, one clock, and a record that outlives both
--
-- The first ban was one boolean: content hidden and posting refused. Real
-- moderation is proportional, so a tier is now picked per ban:
--
--   ''          not banned.
--   'content'   their content is HIDDEN and they cannot post or reply. The
--               classic ban; friends and messages still work.
--   'read_only' the wall stays up and readable; they cannot post, reply,
--               react, edit their profile — or use friends and messages.
--               The "they can only view" tier.
--   'social'    the wall works exactly as before, but friends, requests and
--               messages are paused. The "banned from the social side" tier.
--   'account'   the whole account is locked: content hidden, no write
--               anywhere in the online layer, and the app shows them the
--               lock instead of pretending nothing happened.
--
-- `banned` stays the boolean the app and the older clients read (it means
-- "hidden", i.e. the content and account tiers), and `banned_until` is the
-- clock: NULL beside a tier = permanent, a stamp in the past = the ban has
-- LAPSED and reads as no ban on its own. Nothing is deleted when it lapses or
-- is lifted — moderation_actions keeps the record, and the ban list still
-- shows the row as history.
--
-- ENFORCEMENT IS A TRIGGER, deliberately. A member's write reaches the
-- database either as a direct REST call (RLS decides) or through a
-- security-definer RPC (RLS is bypassed by construction), and a tier that
-- only holds on one of those paths is decoration. A row trigger fires either
-- way, so `curio_ban_guard` below is the real guard; the restrictive select
-- policies above stay as they are, because hiding content is a READ rule.
-- ───────────────────────────────────────────────────────────────────────────

alter table public.profiles add column if not exists ban_kind text not null default '';
alter table public.profiles add column if not exists banned_until timestamptz;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_ban_kind_values') then
        alter table public.profiles add constraint profiles_ban_kind_values
            check (ban_kind in ('', 'content', 'read_only', 'social', 'account'));
    end if;
end $$;

/**
 * The member's ACTIVE tier, '' when they are not banned.
 *
 * A lapsed timed ban reads as no ban, so it lifts itself the moment its clock
 * runs out — no moderator action, no cron job, and no difference between the
 * app's answer and the database's. A row that predates the ladder (`banned`
 * with no kind) reads as the content tier.
 */
create or replace function public.curio_ban_kind(subject uuid)
returns text
language sql
stable
security definer
set search_path = public
as $$
    select coalesce((
        select case
            when p.banned_until is not null and p.banned_until <= now() then ''
            when p.ban_kind <> '' then p.ban_kind
            when p.banned then 'content'
            else ''
        end
        from public.profiles p
        where p.id = subject
    ), '')
$$;

revoke all on function public.curio_ban_kind(uuid) from public, anon;
grant execute on function public.curio_ban_kind(uuid) to authenticated;

-- "Is this member's content hidden?" — now the LADDER's answer rather than the
-- raw boolean, so a read-only or social ban leaves the wall alone while the
-- content and account tiers still disappear from everyone's feed. Every
-- restrictive policy added in section 6b calls this, unchanged.
create or replace function public.curio_member_hidden(subject uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select public.curio_ban_kind(subject) in ('content', 'account')
$$;

/** Who may still post on the wall: the un-banned and the social tier (the wall
 *  is exactly what a social ban leaves them). */
create or replace function public.curio_can_post(subject uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select public.curio_ban_kind(subject) in ('', 'social')
$$;

revoke all on function public.curio_can_post(uuid) from public, anon;
grant execute on function public.curio_can_post(uuid) to authenticated;

/** Who may still use friends and messages: a CONTENT ban keeps them (it is a
 *  wall punishment); the read-only and social tiers lose them. */
create or replace function public.curio_can_social(subject uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select public.curio_ban_kind(subject) in ('', 'content')
$$;

revoke all on function public.curio_can_social(uuid) from public, anon;
grant execute on function public.curio_can_social(uuid) to authenticated;

/**
 * The guard itself. Attached to every writable table in the online layer.
 *
 * It refuses by TIER, not by table: the account tier refuses everything, the
 * wall tables refuse the read-only and content tiers, and the social tables
 * (friends, requests, messages, typing, blocks, the profile row itself)
 * refuse the read-only and social tiers. Blanket-account bans are checked
 * first so an account ban cannot post into the wall through a table nobody
 * remembered to list.
 */
create or replace function public.curio_ban_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    subject uuid := auth.uid();
    tier    text;
    v_wall  boolean;
begin
    if subject is null then
        return new;
    end if;
    tier := public.curio_ban_kind(subject);
    if tier = '' then
        return new;
    end if;
    if tier = 'account' then
        raise exception 'curio: this account is banned';
    end if;
    -- The profile row is the exception below the account tier: the app writes
    -- presence and `last_active_at` into it in the background, and a ban that
    -- makes a routine heartbeat fail would fill the logs with errors that
    -- have nothing to do with the ban. The tiers below still lose every
    -- WRITE the wall and the social side own.
    if tg_table_name = 'profiles' then
        return new;
    end if;
    v_wall := tg_table_name in ('community_cards', 'community_comments',
                                'community_reactions', 'community_comment_reactions');
    if v_wall and not public.curio_can_post(subject) then
        raise exception 'curio: your account is read-only right now';
    end if;
    if not v_wall and not public.curio_can_social(subject) then
        raise exception 'curio: friends and messages are paused on this account';
    end if;
    return new;
end $$;

do $$
declare
    t text;
begin
    foreach t in array array['community_cards', 'community_comments',
                             'community_reactions', 'community_comment_reactions',
                             'friend_requests', 'dm_messages', 'dm_typing',
                             'dm_reactions', 'member_blocks', 'member_follows', 'profiles']
    loop
        execute format('drop trigger if exists curio_ban_guard_%1$s on public.%1$s', t);
        execute format(
            'create trigger curio_ban_guard_%1$s before insert or update on public.%1$s ' ||
            'for each row execute function public.curio_ban_guard()', t);
    end loop;
end $$;

/**
 * Bans a member at a tier, for a while or for good.
 *
 * `p_hours` null (or 0) is permanent — the tier is what decides how far the
 * ban reaches and the clock is what decides how long it lasts, so the two are
 * deliberately separate arguments. The owned rules are the same as the old
 * hide: a reason is required, the owner can never be banned, and a moderator
 * cannot ban themselves.
 */
create or replace function public.curio_moderate_ban_member(
    p_user_id uuid,
    p_kind text,
    p_reason text default null,
    p_hours integer default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_kind text := lower(btrim(coalesce(p_kind, '')));
    v_reason text := nullif(btrim(coalesce(p_reason, '')), '');
    v_until timestamptz;
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('bans') then
        raise exception 'curio: you do not have permission to ban members';
    end if;
    if v_kind not in ('content', 'read_only', 'social', 'account') then
        raise exception 'curio: unknown ban tier';
    end if;
    if v_reason is null then
        raise exception 'curio: a reason is required';
    end if;
    if p_user_id is null then
        raise exception 'curio: nobody to ban';
    end if;
    if p_user_id = actor then
        raise exception 'curio: you cannot ban yourself';
    end if;
    if exists (select 1 from public.community_admins a
                where a.user_id = p_user_id and a.role = 'owner') then
        raise exception 'curio: the owner cannot be banned';
    end if;
    if p_hours is not null and p_hours > 0 then
        -- Ten years is the ceiling; anything longer is a permanent ban said
        -- the long way, and keeping the stamp honest beats an overflow.
        v_until := now() + make_interval(hours => least(p_hours, 87600));
    end if;
    update public.profiles
       set banned       = v_kind in ('content', 'account'),
           banned_at    = now(),
           banned_by    = actor,
           ban_reason   = left(v_reason, 300),
           ban_kind     = v_kind,
           banned_until = v_until
     where id = p_user_id;
    if not found then
        raise exception 'curio: that member is gone';
    end if;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason)
    values (actor, 'ban_' || v_kind, 'user', p_user_id, p_user_id, left(v_reason, 300));
end $$;

/** Lifts whatever tier is in force. The profile row goes back to clean; the
 *  audit row stays, which is the only place the record needs to live. */
create or replace function public.curio_moderate_lift_ban(
    p_user_id uuid,
    p_note text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    actor uuid := auth.uid();
    v_note text := nullif(btrim(coalesce(p_note, '')), '');
begin
    if actor is null then
        raise exception 'curio: sign in first';
    end if;
    if not public.curio_admin_can('bans') then
        raise exception 'curio: you do not have permission to lift bans';
    end if;
    update public.profiles
       set banned       = false,
           banned_at    = null,
           banned_by    = null,
           ban_reason   = null,
           ban_kind     = '',
           banned_until = null
     where id = p_user_id;
    if not found then
        raise exception 'curio: that member is gone';
    end if;
    insert into public.moderation_actions (actor, action, target_kind, target_id, target_owner, reason)
    values (actor, 'unban', 'user', p_user_id, p_user_id, v_note);
end $$;

/**
 * The old one-boolean hide, kept working for any client that still calls it:
 * it is now the content tier of the ladder, so a build in the wild and a new
 * build write the same state.
 */
create or replace function public.curio_moderate_hide_member(
    p_user_id uuid,
    p_hidden boolean,
    p_reason text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if p_hidden then
        perform public.curio_moderate_ban_member(p_user_id, 'content', p_reason, null);
    else
        perform public.curio_moderate_lift_ban(p_user_id, p_reason);
    end if;
end $$;

/**
 * Every member carrying a ban stamp, live bans first — the moderation page's
 * own list, and the only place a lifted-again member's lapsed ban is still
 * visible as a row (`active` false). Gated by the 'bans' permission, since a
 * ban list is itself sensitive.
 */
create or replace function public.curio_moderate_list_bans()
returns table (
    user_id       uuid,
    display_name  text,
    username      text,
    avatar_style  smallint,
    kind          text,
    reason        text,
    banned_at     timestamptz,
    banned_until  timestamptz,
    banned_by     uuid,
    banned_by_name text,
    active        boolean
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
    if not public.curio_admin_can('bans') then
        raise exception 'curio: you do not have permission to read the ban list';
    end if;
    return query
        select p.id, p.display_name, p.username, p.avatar_style,
               k.tier, p.ban_reason, p.banned_at, p.banned_until, p.banned_by,
               b.display_name,
               k.tier <> ''
          from public.profiles p
          cross join lateral (select public.curio_ban_kind(p.id) as tier) k
          left join public.profiles b on b.id = p.banned_by
         where p.banned or p.ban_kind <> ''
         order by (k.tier <> '') desc, p.banned_at desc nulls last
         limit 200;
end $$;

/**
 * One member's moderation record, newest first: what was done, to what, why,
 * by whom. A member may read their OWN record (a ban they cannot look up is
 * just a mystery), and the team may read anyone's.
 */
create or replace function public.curio_moderate_member_history(p_user_id uuid)
returns table (
    id         uuid,
    action     text,
    reason     text,
    note       text,
    created_at timestamptz,
    actor      uuid,
    actor_name text,
    actor_username text
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
    if auth.uid() is null then
        raise exception 'curio: sign in first';
    end if;
    if p_user_id is null then
        raise exception 'curio: no member';
    end if;
    if p_user_id <> auth.uid() and not public.curio_admin_can('bans') then
        raise exception 'curio: that record is the moderation team''s';
    end if;
    return query
        select m.id, m.action, m.reason, m.note, m.created_at,
               m.actor, a.display_name, a.username
          from public.moderation_actions m
          left join public.profiles a on a.id = m.actor
         where m.target_owner = p_user_id
            or (m.target_kind = 'user' and m.target_id = p_user_id)
         order by m.created_at desc
         limit 60;
end $$;

revoke all on function public.curio_moderate_ban_member(uuid, text, text, integer) from public, anon;
grant execute on function public.curio_moderate_ban_member(uuid, text, text, integer) to authenticated;
revoke all on function public.curio_moderate_lift_ban(uuid, text) from public, anon;
grant execute on function public.curio_moderate_lift_ban(uuid, text) to authenticated;
revoke all on function public.curio_moderate_list_bans() from public, anon;
grant execute on function public.curio_moderate_list_bans() to authenticated;
revoke all on function public.curio_moderate_member_history(uuid) from public, anon;
grant execute on function public.curio_moderate_member_history(uuid) to authenticated;

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
                         'community_comment_reactions',
                         'community_reports','friend_requests','dm_messages',
                         'dm_typing','dm_reactions','dm_conversation_hidden',
                         'feedback_forms','feedback_answers','feedback_skips')
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
                         'community_comment_reactions',
                         'community_reports','friend_requests','dm_messages',
                         'dm_typing','dm_reactions','member_blocks',
                         'dm_conversation_hidden','feedback_forms',
                         'feedback_answers','feedback_skips');
    if anon_open is null then
        raise notice 'PASS  no anon policies on Curio tables';
    else
        raise warning 'FAIL  anon policies exist on: %', anon_open;
    end if;

    select string_agg(t, ', ')
      into missing
      from unnest(array['profiles','cloud_captures','community_cards',
                        'community_reactions','community_comments',
                        'community_comment_reactions',
                        'community_reports','friend_requests','dm_messages',
                        'dm_typing','dm_reactions','member_blocks',
                        'dm_conversation_hidden','feedback_forms',
                        'feedback_answers','feedback_skips']) as t
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

    -- ── the forms are anonymous, and stay that way ────────────────────────
    -- The whole promise of the feedback form is that an answer cannot be
    -- traced to the account that gave it. A future column named for a user
    -- (id, owner, author, device, session…) on either answer table would
    -- break that promise silently, so it breaks the paste instead.
    select string_agg(c.relname || '.' || a.attname, ', ')
      into anon_open
      from pg_attribute a
      join pg_class c on c.oid = a.attrelid
      join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = 'public'
       and c.relname in ('feedback_answers', 'feedback_skips')
       and a.attnum > 0 and not a.attisdropped
       and a.attname ~* '(user|owner|author|device|session|account|profile)';
    if anon_open is null then
        raise notice 'PASS  the form answers carry no identity';
    else
        raise warning 'FAIL  identity columns on form answers: %', anon_open;
    end if;
end $$;
