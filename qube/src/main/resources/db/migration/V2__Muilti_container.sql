
ALTER TABLE job ADD COLUMN containers JSONB;

UPDATE job
SET containers = json_build_object(
  'default', json_build_object(
    'exit_code', job.exit_code,
    'reason', job.reason,
    'message', job.message,
    'log', job.log
  )
);

ALTER TABLE job
DROP COLUMN exit_code,
DROP COLUMN reason,
DROP COLUMN message,
DROP COLUMN log;
