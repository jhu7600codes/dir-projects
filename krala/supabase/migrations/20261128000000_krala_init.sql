-- krala: cloud sync schema
-- every table is keyed by (user_id, id) and locked down with row level security so a user only
-- ever sees their own rows (auth.uid() = user_id). updated_at is epoch millis set by the client;
-- the newer row wins on both sides (see keep_newest below). deletes are tombstones (deleted = true).

-- ---------------------------------------------------------------- tables

create table if not exists public.conversations (
    user_id         uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    id              text    not null,
    title           text    not null default '',
    title_auto      boolean not null default true,
    pinned          boolean not null default false,
    provider_id     text,
    model           text,
    system_prompt   text,
    created_at      bigint  not null,
    updated_at      bigint  not null,
    last_message_at bigint  not null,
    deleted         boolean not null default false,
    primary key (user_id, id)
);

create table if not exists public.messages (
    user_id         uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    id              text    not null,
    conversation_id text    not null,
    role            text    not null check (role in ('user', 'assistant')),
    content         text    not null default '',
    hidden_prefix   text,
    attachments     text    not null default '[]',
    status          text    not null default 'done',
    error           text,
    provider_name   text,
    model           text,
    created_at      bigint  not null,
    updated_at      bigint  not null,
    deleted         boolean not null default false,
    primary key (user_id, id)
);

create table if not exists public.memory_entries (
    user_id       uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    id            text    not null,
    kind          text    not null default 'chat' check (kind in ('chat', 'archive')),
    chat_id       text,
    chat_name     text    not null default '',
    notes         text    not null default '',
    archived_into text,
    created_at    bigint  not null,
    updated_at    bigint  not null,
    deleted       boolean not null default false,
    primary key (user_id, id)
);

-- api keys are never stored here in plain text. key_cipher is only set when the user turns on
-- "sync keys", and holds the key encrypted on the device with their passphrase (pbkdf2 + aes-gcm).
create table if not exists public.providers (
    user_id         uuid    not null default auth.uid() references auth.users (id) on delete cascade,
    id              text    not null,
    name            text    not null,
    base_url        text    not null,
    models          text    not null default '[]',
    default_model   text,
    context_size    integer not null default 32000,
    priority        integer not null default 0,
    enabled         boolean not null default true,
    preset_id       text,
    supports_vision boolean not null default false,
    key_cipher      text,
    created_at      bigint  not null,
    updated_at      bigint  not null,
    deleted         boolean not null default false,
    primary key (user_id, id)
);

create table if not exists public.user_settings (
    user_id    uuid   not null default auth.uid() primary key references auth.users (id) on delete cascade,
    data       text   not null default '{}',
    updated_at bigint not null
);

create index if not exists conversations_user_updated on public.conversations (user_id, updated_at);
create index if not exists messages_user_updated on public.messages (user_id, updated_at);
create index if not exists messages_conversation on public.messages (user_id, conversation_id);
create index if not exists memory_user_updated on public.memory_entries (user_id, updated_at);
create index if not exists providers_user_updated on public.providers (user_id, updated_at);

-- ---------------------------------------------------------------- last write wins

-- an upsert carrying an older updated_at than the stored row is ignored, so a device that was
-- offline can never overwrite newer edits from another device.
create or replace function public.keep_newest()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.updated_at < old.updated_at then
        return null;
    end if;
    return new;
end;
$$;

do $$
declare t text;
begin
    foreach t in array array['conversations', 'messages', 'memory_entries', 'providers', 'user_settings'] loop
        execute format('drop trigger if exists %I_keep_newest on public.%I', t, t);
        execute format('create trigger %I_keep_newest before update on public.%I for each row execute function public.keep_newest()', t, t);
    end loop;
end;
$$;

-- ---------------------------------------------------------------- row level security

alter table public.conversations  enable row level security;
alter table public.messages       enable row level security;
alter table public.memory_entries enable row level security;
alter table public.providers      enable row level security;
alter table public.user_settings  enable row level security;

do $$
declare t text;
begin
    foreach t in array array['conversations', 'messages', 'memory_entries', 'providers', 'user_settings'] loop
        execute format('drop policy if exists "own rows: select" on public.%I', t);
        execute format('drop policy if exists "own rows: insert" on public.%I', t);
        execute format('drop policy if exists "own rows: update" on public.%I', t);
        execute format('drop policy if exists "own rows: delete" on public.%I', t);
        execute format('create policy "own rows: select" on public.%I for select to authenticated using ((select auth.uid()) = user_id)', t);
        execute format('create policy "own rows: insert" on public.%I for insert to authenticated with check ((select auth.uid()) = user_id)', t);
        execute format('create policy "own rows: update" on public.%I for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)', t);
        execute format('create policy "own rows: delete" on public.%I for delete to authenticated using ((select auth.uid()) = user_id)', t);
    end loop;
end;
$$;

-- ---------------------------------------------------------------- realtime

-- postgres changes respect rls, so each device only hears about its own user's rows
do $$
declare t text;
begin
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        create publication supabase_realtime;
    end if;
    foreach t in array array['conversations', 'messages', 'memory_entries', 'providers', 'user_settings'] loop
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = t
        ) then
            execute format('alter publication supabase_realtime add table public.%I', t);
        end if;
    end loop;
end;
$$;

-- ---------------------------------------------------------------- account deletion

-- handled by the delete-account edge function (supabase/functions/delete-account): it deletes the
-- caller's auth user with the service role, and every table above cascades from auth.users.
