-- lottery locker schema (supabase project "lot", eu-central-1)
-- clients only read through RLS and write through the locker_* functions.

create extension if not exists pgcrypto with schema extensions;

create table public.locker_items (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('mode', 'ticket', 'outfit')),
  name text not null check (char_length(name) between 1 and 40),
  author text not null check (char_length(author) between 1 and 24),
  data jsonb not null check (pg_column_size(data) < 6000),
  owner_hash text not null,
  downloads integer not null default 0,
  likes integer not null default 0,
  reports integer not null default 0,
  created_at timestamptz not null default now()
);

create index locker_items_kind_created on public.locker_items (kind, created_at desc);
create index locker_items_owner on public.locker_items (owner_hash);

alter table public.locker_items enable row level security;
create policy "browse locker" on public.locker_items
  for select to anon, authenticated using (reports < 3);

revoke all on public.locker_items from anon, authenticated;
grant select (id, kind, name, author, data, downloads, likes, reports, created_at)
  on public.locker_items to anon, authenticated;

-- publish: owner proves ownership later with the same secret (only its sha256 is stored)
create or replace function public.locker_publish(p_kind text, p_name text, p_author text, p_data jsonb, p_secret text)
returns uuid language plpgsql security definer set search_path = public, extensions as $$
declare
  h text := encode(extensions.digest(p_secret, 'sha256'), 'hex');
  new_id uuid;
begin
  if char_length(coalesce(p_secret, '')) < 16 then raise exception 'bad secret'; end if;
  if (select count(*) from locker_items where owner_hash = h and created_at > now() - interval '1 day') >= 20 then
    raise exception 'slow down, max 20 uploads a day';
  end if;
  insert into locker_items (kind, name, author, data, owner_hash)
  values (p_kind, trim(p_name), trim(p_author), p_data, h)
  returning id into new_id;
  return new_id;
end $$;

create or replace function public.locker_delete(p_id uuid, p_secret text)
returns boolean language plpgsql security definer set search_path = public, extensions as $$
begin
  delete from locker_items where id = p_id and owner_hash = encode(extensions.digest(p_secret, 'sha256'), 'hex');
  return found;
end $$;

create or replace function public.locker_mine(p_secret text)
returns setof uuid language sql security definer set search_path = public, extensions stable as $$
  select id from locker_items where owner_hash = encode(extensions.digest(p_secret, 'sha256'), 'hex');
$$;

create or replace function public.locker_bump(p_id uuid, p_what text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if p_what = 'download' then update locker_items set downloads = downloads + 1 where id = p_id;
  elsif p_what = 'like' then update locker_items set likes = likes + 1 where id = p_id;
  elsif p_what = 'report' then update locker_items set reports = reports + 1 where id = p_id;
  end if;
end $$;

revoke all on function public.locker_publish(text, text, text, jsonb, text) from public;
revoke all on function public.locker_delete(uuid, text) from public;
revoke all on function public.locker_mine(text) from public;
revoke all on function public.locker_bump(uuid, text) from public;
grant execute on function public.locker_publish(text, text, text, jsonb, text) to anon, authenticated;
grant execute on function public.locker_delete(uuid, text) to anon, authenticated;
grant execute on function public.locker_mine(text) to anon, authenticated;
grant execute on function public.locker_bump(uuid, text) to anon, authenticated;
