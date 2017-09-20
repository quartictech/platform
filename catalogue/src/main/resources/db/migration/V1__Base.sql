CREATE TABLE dataset(
  namespace       varchar NOT NULL,
  id              varchar NOT NULL,
  config          jsonb,
  time            timestamp NOT NULL
);
