-- SunMoon Resort - Supabase relational schema (long-term path)
-- Run this in Supabase SQL Editor.

-- 1) Bookings core table
create table if not exists public.bookings (
  booking_id text primary key,
  room_number integer not null,
  guest_name text not null,
  contact_number text not null,
  days_stayed integer not null check (days_stayed > 0),
  check_in_date date not null,
  check_out_date date not null,
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

-- 5) Concurrency protection: prevent active-booking overlap on same room.
create extension if not exists btree_gist;

-- Ensure existing installations use DATE typed columns (required for immutable index expression).
alter table public.bookings
  alter column check_in_date type date using check_in_date::date,
  alter column check_out_date type date using check_out_date::date;

alter table public.bookings
  drop constraint if exists bookings_no_overlap_per_room;

alter table public.bookings
  add constraint bookings_no_overlap_per_room
  exclude using gist (
    room_number with =,
    daterange(check_in_date, check_out_date, '[)') with &&
  )
  where (status in ('CONFIRMED', 'CHECKED_IN'));

-- 6) Atomic room confirmation RPC (single transaction).
drop function if exists public.confirm_booking_atomic(
  text, integer, text, text, integer, text, text, text, numeric, jsonb
);

create or replace function public.confirm_booking_atomic(
  booking_id text,
  room_number integer,
  guest_name text,
  contact_number text,
  days_stayed integer,
  check_in_date date,
  check_out_date date,
  status text,
  total_amount numeric,
  breakdown_items jsonb default '[]'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  has_conflict boolean;
  row_item jsonb;
begin
  if status not in ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') then
    return jsonb_build_object('ok', false, 'reason', 'INVALID_STATUS');
  end if;

  perform pg_advisory_xact_lock(room_number);

  select exists (
    select 1
    from public.bookings b
    where b.room_number = confirm_booking_atomic.room_number
      and b.status in ('CONFIRMED', 'CHECKED_IN')
      and daterange(b.check_in_date, b.check_out_date, '[)')
          && daterange(confirm_booking_atomic.check_in_date, confirm_booking_atomic.check_out_date, '[)')
  ) into has_conflict;

  if has_conflict then
    return jsonb_build_object('ok', false, 'reason', 'ROOM_ALREADY_BOOKED');
  end if;

  insert into public.bookings (
    booking_id,
    room_number,
    guest_name,
    contact_number,
    days_stayed,
    check_in_date,
    check_out_date,
    status,
    total_amount
  ) values (
    booking_id,
    room_number,
    guest_name,
    contact_number,
    days_stayed,
    check_in_date,
    check_out_date,
    status,
    total_amount
  );

  for row_item in select * from jsonb_array_elements(coalesce(breakdown_items, '[]'::jsonb)) loop
    insert into public.booking_breakdown_items (
      booking_id,
      line_order,
      item_name,
      calculation,
      amount
    ) values (
      booking_id,
      coalesce((row_item ->> 'line_order')::integer, 0),
      coalesce(row_item ->> 'item_name', ''),
      coalesce(row_item ->> 'calculation', ''),
      coalesce((row_item ->> 'amount')::numeric, 0)
    );
  end loop;

  return jsonb_build_object('ok', true, 'booking_id', booking_id);
exception
  when others then
    return jsonb_build_object('ok', false, 'reason', SQLSTATE, 'message', SQLERRM);
end;
$$;

grant execute on function public.confirm_booking_atomic(
  text, integer, text, text, integer, date, date, text, numeric, jsonb
) to anon;

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

