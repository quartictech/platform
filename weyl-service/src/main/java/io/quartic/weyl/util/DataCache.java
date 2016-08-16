package io.quartic.weyl.util;

import java.util.Optional;

public interface DataCache {
    void putTile(String layer, int z, int x, int y, byte[] data);
    Optional<byte[]> getTile(String layer, int z, int x, int y);

    void putVector(String layer, byte[] data);
    Optional<byte[]> getVector(String layer);
}
