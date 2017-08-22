
create table job(
  id uuid,
  client uuid,
  name varchar,
  create_spec jsonb,
  log text,
  start_time timestamp,
  end_time timestamp,
  reason varchar,
  message varchar,
  exit_code int
);
