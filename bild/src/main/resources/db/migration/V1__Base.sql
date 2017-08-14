
create table build(
  id serial,
  customer_id varchar,
  installation_id bigint,
  clone_url varchar,
  ref varchar,
  commit varchar,
  phase varchar,
  start_time timestamp,
  success boolean,
  reason varchar,
  dag jsonb
);

create table job(
  id serial,
  build_id integer,
  pod_name varchar,
  log text
);
