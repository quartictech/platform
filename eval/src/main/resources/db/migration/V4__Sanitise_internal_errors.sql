-- Clear out the internal_error content
UPDATE event
  SET payload = jsonb_set(payload, '{"result"}', '{"type": "internal_error"}')
  WHERE
    payload->>'type' = 'phase_completed' AND
    payload#>>'{result,type}' = 'internal_error'
