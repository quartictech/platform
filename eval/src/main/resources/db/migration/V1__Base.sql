create table build(
  id uuid not null unique,
  customer_id bigint not null,
  branch varchar not null,
  build_number bigint not null
);

create table event(
  id uuid not null unique,
  build_id uuid not null,
  payload jsonb,
  time timestamp not null
);

CREATE index payload_idx ON event USING gin ((payload));
