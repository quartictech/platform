-- We decided to start again for this change to the schema
drop table build;
drop table phase;
drop table event;

create table build(
  id uuid not null unique,
  customer_id bigint not null,
  branch varchar not null,
  build_number bigint not null
);

create table event(
  id uuid not null unique,
  build_id uuid not null,
  phase_id uuid,  -- Can be null
  payload jsonb,
  time timestamp not null
);

create index idxgintype on event USING gin ((payload -> 'type'));
