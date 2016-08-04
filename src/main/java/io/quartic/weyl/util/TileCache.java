package io.quartic.weyl.util;

import java.util.Optional;

public interface TileCache {
    void put(String layer, int z, int x, int y, byte[] data);
    Optional<byte[]> get(String layer, int z, int x, int y);
}
