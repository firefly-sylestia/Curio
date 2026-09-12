-- Curio DM E2EE requires PostgREST table privileges in addition to RLS.
--
-- The policies in schema.sql protect WHICH rows a signed-in user can touch,
-- but PostgreSQL privileges decide whether the authenticated role can touch
-- these tables at all. Without these grants, publishDmIdentity()/saveDmEnvelope()
-- fail before a message is sent; the Android client used to surface that as
-- the misleading "Secure storage is unavailable" message.

begin;

grant select, insert, update, delete on public.dm_device_keys to authenticated;
grant select, insert, update, delete on public.dm_key_envelopes to authenticated;

-- Keep the RLS guards enabled even if an older project disabled them manually.
alter table public.dm_device_keys enable row level security;
alter table public.dm_key_envelopes enable row level security;

-- Re-assert the exact policies required by the Kotlin DM key flow. These are
-- idempotent, so the migration is safe to paste into an existing Curio project.
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
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists dm_device_keys_delete_own on public.dm_device_keys;
create policy dm_device_keys_delete_own on public.dm_device_keys
  for delete to authenticated using (user_id = auth.uid());

drop policy if exists dm_key_envelopes_recipient on public.dm_key_envelopes;
create policy dm_key_envelopes_recipient on public.dm_key_envelopes
  for select to authenticated using (recipient = auth.uid());

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

commit;
