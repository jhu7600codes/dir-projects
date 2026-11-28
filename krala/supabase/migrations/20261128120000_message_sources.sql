-- web sources a reply used, one "title<tab>url" per line
alter table public.messages add column if not exists sources text;
