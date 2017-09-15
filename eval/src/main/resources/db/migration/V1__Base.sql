CREATE TABLE build(
  id              uuid NOT NULL UNIQUE,
  customer_id     bigint NOT NULL,
  branch          varchar NOT NULL,
  build_number    bigint NOT NULL
);

CREATE TABLE event(
  id              uuid NOT NULL UNIQUE,
  build_id        uuid NOT NULL,
  payload         jsonb,
  time            timestamp NOT NULL
);

CREATE index payload_idx ON event USING gin ((payload));
