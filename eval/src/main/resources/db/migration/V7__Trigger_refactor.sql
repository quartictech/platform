
-- Move payload down a level into payload -> 'trigger'
UPDATE event
SET payload = json_build_object('trigger', event.payload, 'type', 'trigger_received_v1')
WHERE event.payload->>'type' = 'trigger_received_v1';

-- Set the type of the trigger to be a github webhook
UPDATE event
SET payload = jsonb_set(event.payload, '{"trigger", "type"}'::text[], '"github_webhook_v1"'::jsonb)
WHERE event.payload->>'type' = 'trigger_received_v1';

-- Delete cloneUrl, triggerType and repoFullName
UPDATE event
SET payload = jsonb_set(event.payload, '{"trigger"}', (event.payload->'trigger')::jsonb - 'cloneUrl' - 'triggerType' - 'repoFullName')
WHERE event.payload->>'type' = 'trigger_received_v1';