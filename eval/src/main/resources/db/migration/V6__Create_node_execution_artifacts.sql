-- Create node_execution artifacts for successful node-related phases
UPDATE      event AS e1
SET         payload = jsonb_set(e1.payload, '{"result","artifact"}', '{"type": "node_execution", "skipped": false}')
FROM        event AS e2
WHERE
            e1.payload->>'phase_id' = e2.payload->>'phase_id' AND
            e1.payload->>'type' = 'phase_completed' AND
            e1.payload#>>'{result,type}' = 'success' AND
            e2.payload->>'type' = 'phase_started' AND
            e2.payload->>'description' LIKE '%for dataset%'

