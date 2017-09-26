-- This fixes a problem introduced in V5 whereby there was
-- a surplus layer of nesting in UserError->OtherException
UPDATE event e
SET payload = jsonb_set(e.payload, '{result, info, detail}', e.payload#>'{result, info, detail, detail}')
WHERE
  e.payload->>'type' = 'phase_completed' AND
  e.payload#>>'{result, type}' = 'user_error' AND
  e.payload#>>'{result, info, type}' = 'other_exception'
