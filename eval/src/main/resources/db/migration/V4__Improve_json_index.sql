-- We need to index more paths in the JSON blobs now, so let's index the whole thing for now
DROP index idxgintype;
CREATE index payload_idx ON event USING gin ((payload));
