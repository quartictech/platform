
create table build(
  id uuid not null unique,
  customer_id bigint not null,
  trigger_details jsonb not null,
  build_number bigint not null,
  time timestamp not null
);

create table phase(
  id uuid not null unique,
  build_id uuid not null,
  name varchar not null,
  time timestamp not null
);

create table event(
  id uuid not null unique,
  phase_id uuid not null,
  type varchar not null,
  message jsonb,
  time timestamp not null
);



