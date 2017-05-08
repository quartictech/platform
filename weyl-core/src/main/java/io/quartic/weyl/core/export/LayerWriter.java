package io.quartic.weyl.core.export;

import io.quartic.weyl.core.model.Layer;

import java.io.IOException;

public interface LayerWriter {
    LayerExportResult write(Layer layer) throws IOException;
}
