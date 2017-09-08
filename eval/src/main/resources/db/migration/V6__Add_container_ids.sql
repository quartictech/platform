-- We now record container IDs too, so set old entries to the nil UID (as we don't have that info)
UPDATE event
  SET payload = jsonb_set(payload, '{"containerId"}', '"00000000-0000-0000-0000-000000000000"')
  WHERE payload @> '{"type": "container_acquired_v1"}';
