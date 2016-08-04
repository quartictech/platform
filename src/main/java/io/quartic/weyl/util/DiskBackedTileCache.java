package io.quartic.weyl.util;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Map;
import java.util.Optional;

public class DiskBackedTileCache implements TileCache {
    private final File directory;
    private final long timeoutMillis;
    private final Map<String, Long> insertTimes;

    public DiskBackedTileCache(String path, long timeoutMillis) throws IOException {
        this.directory = new File(path);
        this.timeoutMillis = timeoutMillis;

        if (! directory.exists()) {
            throw new IOException("Directory " + path + " does not exist");
        }
        insertTimes = Maps.newHashMap();
    }

    private String fileName(String layer, int z, int x, int y) {
        return String.format("%s_%d_%d_%d.pbf", layer, z, x, y);
    }

    @Override
    public synchronized void put(String layer, int z, int x, int y, byte[] data) {
        String name = fileName(layer, z, x, y);
        File file = new File(directory, name);

        try {
            new FileOutputStream(file).write(data);
            insertTimes.put(name, System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<byte[]> get(String layer, int z, int x, int y) {
        String name = fileName(layer, z, x, y);

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
}
