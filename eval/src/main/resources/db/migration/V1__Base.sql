
create table build(
  id uuid not null unique,
  customer_id bigint,
  trigger_details jsonb,
  build_number bigint,
  result jsonb,
  time timestamp
);

create table phase(
  id uuid not null unique,
  name varchar,
  build_id uuid,
  time timestamp
);

create table event(
  id uuid not null unique,
  phase_id uuid,
  type varchar,
  message jsonb,
  time timestamp
);



