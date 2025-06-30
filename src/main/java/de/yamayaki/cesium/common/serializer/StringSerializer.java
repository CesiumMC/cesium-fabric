package de.yamayaki.cesium.common.serializer;

import de.yamayaki.cesium.api.ISerializer;

import java.nio.charset.StandardCharsets;

public class StringSerializer implements ISerializer<String> {
    @Override
    public byte[] serialize(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String deserialize(byte[] input) {
        return new String(input, StandardCharsets.UTF_8);
    }
}
