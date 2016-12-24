package io.quartic.geojson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.quartic.common.serdes.ObjectMappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * GeoJsonParser is a very forgiving parser of geojson files containing a simple FeatureCollection
 */
public class GeoJsonParser implements Iterator<Feature> {
    enum State {
        START,
        FEATURES,
       END
    }
    private final JsonParser parser;
    private JsonToken token;
    private State state = State.START;


    public GeoJsonParser(InputStream inputStream) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.setCodec(ObjectMappers.OBJECT_MAPPER);
        parser = jsonFactory.createParser(inputStream);
    }

    private void until(JsonToken expected, String text) throws IOException {
        for (token = parser.nextToken() ; token != null; token = parser.nextToken()) {
            if (token.equals(expected) && parser.getText().equals(text)) {
                return;
            }
        }
        throw new IOException("reached end of input searching for: " + expected + "[text=" + text + "]");
    }

    private void expect(JsonToken expected) throws IOException {
        token = parser.nextToken();
        if (!token.equals(expected)) {
            throw new IOException("expected " + expected + " but found " + token);
        }
    }

    private State update() throws IOException {
        switch (state) {
            case START:
                until(JsonToken.FIELD_NAME, "features");
                expect(JsonToken.START_ARRAY);
                expect(JsonToken.START_OBJECT);
                return State.FEATURES;
            case FEATURES:
                if (token == JsonToken.START_OBJECT) {
                    return state;
                } else if (token == JsonToken.END_ARRAY) {
                    return State.END;
                } else throw new IOException("unexpected token: " + token);
            default:
                return state;
        }
    }

    private Feature getNext() throws IOException {
        switch (state) {
            case FEATURES:
                Feature feature = parser.readValueAs(Feature.class);
                token = parser.nextToken();
                return feature;
            default:
                throw new RuntimeException("getNext can only be called in START state");
        }
    }


    @Override
    public boolean hasNext() {
        try {
            state = update();
            return state == State.FEATURES && token == JsonToken.START_OBJECT;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Feature next() {
        try {
            state = update();
            return getNext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void validate() {
        while (hasNext()) {
            next();
        }
    }

    public Stream<Feature> features() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
                false);
    }

}
