-- BiteCheck — Supabase schema
-- Run this once in the Supabase dashboard: SQL Editor -> New query -> paste -> Run.
-- Creates all cloud tables (Phases 3-4) with row-level security so each user
-- can only read/write their own rows.

-- Profiles (Phase 3)
create table if not exists public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  name text,
  gender text,
  dob text,
  height_cm numeric,
  weight_kg numeric,
  goal text,
  activity_level int,
  calorie_target int,
  updated_at timestamptz not null default now()
);

-- Meals (Phase 4/5)
create table if not exists public.meals (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  food text not null,
  quantity numeric,
  unit text,
  meal_type text,
  calories int not null,
  logged_at timestamptz not null default now()
);

-- Water logs (Phase 4)
create table if not exists public.water_logs (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  amount_ml int not null,
  logged_at timestamptz not null default now()
);

-- Weight logs (Phase 4)
create table if not exists public.weight_logs (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  weight_kg numeric not null,
  logged_at timestamptz not null default now()
);

-- Feedback (Phase 4)
create table if not exists public.feedback (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  subject text,
  message text not null,
  created_at timestamptz not null default now()
);

-- Complaints (Phase 4)
create table if not exists public.complaints (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  subject text,
  message text not null,
  created_at timestamptz not null default now()
);

-- Row-level security: each user sees only their own rows.
alter table public.profiles enable row level security;
alter table public.meals enable row level security;
alter table public.water_logs enable row level security;
alter table public.weight_logs enable row level security;
alter table public.feedback enable row level security;
alter table public.complaints enable row level security;

create policy "own profile" on public.profiles
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own meals" on public.meals
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own water" on public.water_logs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own weight" on public.weight_logs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own feedback" on public.feedback
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own complaints" on public.complaints
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
