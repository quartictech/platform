package io.quartic.weyl.core.export;

import io.quartic.weyl.core.model.Layer;

public interface LayerWriter {
    LayerExportResult write(Layer layer);
}
