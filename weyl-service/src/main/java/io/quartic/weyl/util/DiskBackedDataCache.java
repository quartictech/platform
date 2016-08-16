package io.quartic.weyl.util;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Map;
import java.util.Optional;

public class DiskBackedDataCache implements DataCache {
    private final File directory;
    private final long timeoutMillis;
    private final Map<String, Long> insertTimes;

    public DiskBackedDataCache(String path, long timeoutMillis) throws IOException {
        this.directory = new File(path);
        this.timeoutMillis = timeoutMillis;

        if (! directory.exists()) {
            throw new IOException("Directory " + path + " does not exist");
        }
        insertTimes = Maps.newHashMap();
    }

    private String fileName(String layer, int z, int x, int y) {
        return String.format("tile_%s_%d_%d_%d.pbf", layer, z, x, y);
    }

    private String fileName(String layer) {
        return String.format("vector_%s", layer);
    }

    private synchronized void put(String name, byte[] data) {
        File file = new File(directory, name);

        try {
            new FileOutputStream(file).write(data);
            insertTimes.put(name, System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<byte[]> get(String name) {
        File file = new File(directory, name);

        if (file.exists()) {
            if (insertTimes.containsKey(name)) {
                Long currentTime = System.currentTimeMillis();
                Long timeSincePut = currentTime - insertTimes.get(name);

                if (timeSincePut > timeoutMillis) {
                    return Optional.empty();
                }

                try {
                    byte[] data = IOUtils.toByteArray(new FileInputStream(file));
                    return Optional.of(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void putTile(String layer, int z, int x, int y, byte[] data) {
        put(fileName(layer, z, x, y), data);
    }

    @Override
    public Optional<byte[]> getTile(String layer, int z, int x, int y) {
        return get(fileName(layer, z, x, y));
    }

    @Override
    public void putVector(String layer, byte[] data) {
        put(fileName(layer), data);
    }

    @Override
    public Optional<byte[]> getVector(String layer) {
        return get(fileName(layer));
    }
}
