CREATE TABLE dataset(
  namespace       varchar NOT NULL,
  id              varchar NOT NULL,
  config          jsonb NOT NULL,
  PRIMARY KEY(namespace, id)
);
