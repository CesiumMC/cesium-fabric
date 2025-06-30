package de.yamayaki.cesium.api;

import java.io.IOException;

public interface IScannable<T> {
    void scan(byte[] input, T scanner) throws IOException;
}
