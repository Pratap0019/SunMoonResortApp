-- SunMoon Resort - Supabase relational schema (long-term path)
-- Run this in Supabase SQL Editor.

-- 1) Bookings core table
create table if not exists public.bookings (
  booking_id text primary key,
  room_number integer not null,
  guest_name text not null,
  contact_number text not null,
  days_stayed integer not null check (days_stayed > 0),
  check_in_date text not null,
  check_out_date text not null,
  status text not null check (status in ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
  total_amount numeric(12,2) not null check (total_amount >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_bookings_room_number on public.bookings(room_number);
create index if not exists idx_bookings_contact_number on public.bookings(contact_number);
create index if not exists idx_bookings_check_in_date on public.bookings(check_in_date);

-- 2) Breakdown lines (ordered)
create table if not exists public.booking_breakdown_items (
  id bigserial primary key,
  booking_id text not null references public.bookings(booking_id) on delete cascade,
  line_order integer not null,
  item_name text not null,
  calculation text not null default '',
  amount numeric(12,2) not null check (amount >= 0)
);

create unique index if not exists uq_breakdown_booking_line
  on public.booking_breakdown_items(booking_id, line_order);

create index if not exists idx_breakdown_booking_id
  on public.booking_breakdown_items(booking_id);

-- 3) Optional trigger to maintain updated_at
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_bookings_updated_at on public.bookings;
create trigger trg_bookings_updated_at
before update on public.bookings
for each row
execute function public.set_updated_at();

-- 4) RLS (single-tenant demo policy)
alter table public.bookings enable row level security;
alter table public.booking_breakdown_items enable row level security;

-- WARNING: This allows any client with anon key to read/write all rows.
-- Tighten these policies before production multi-user rollout.
drop policy if exists bookings_all_anon on public.bookings;
create policy bookings_all_anon
on public.bookings
for all
to anon
using (true)
with check (true);

drop policy if exists breakdown_all_anon on public.booking_breakdown_items;
create policy breakdown_all_anon
on public.booking_breakdown_items
for all
to anon
using (true)
with check (true);

