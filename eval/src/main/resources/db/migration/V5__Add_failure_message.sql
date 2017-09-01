-- Add a default message
UPDATE event
  SET payload = jsonb_set(payload, '{"description"}', '"Unknown error"')
  WHERE payload @> '{"type": "build_failed_v1"}';
