package de.yamayaki.cesium;

public class SerializationHelper {
    public static void writeInt(final byte[] array, final int index, final int value) {
        final int offset = index * Integer.BYTES;

        array[offset + 0] = (byte) (value >> 24);
        array[offset + 1] = (byte) (value >> 16);
        array[offset + 2] = (byte) (value >> 8);
        array[offset + 3] = (byte) value;
    }

    public static int readInt(final byte[] array, final int index) {
        final int offset = index * Integer.BYTES;

        int val = array[offset + 0] << 24;
        val |= (array[offset + 1] & 0xFF) << 16;
        val |= (array[offset + 2] & 0xFF) << 8;
        val |= (array[offset + 3] & 0xFF);

        return val;
    }

    public static void writeLong(final byte[] array, final int index, final long value) {
        final int offset = index * Long.BYTES;

        array[offset + 0] = (byte) (value >> 56);
        array[offset + 1] = (byte) (value >> 48);
        array[offset + 2] = (byte) (value >> 40);
        array[offset + 3] = (byte) (value >> 32);
        array[offset + 4] = (byte) (value >> 24);
        array[offset + 5] = (byte) (value >> 16);
        array[offset + 6] = (byte) (value >> 8);
        array[offset + 7] = (byte) value;
    }

    public static long readLong(final byte[] array, final int index) {
        final int offset = index * Long.BYTES;

        long val = (long) array[offset + 0] << 56;
        val |= (array[offset + 1] & 0xFFL) << 48;
        val |= (array[offset + 2] & 0xFFL) << 40;
        val |= (array[offset + 3] & 0xFFL) << 32;
        val |= (array[offset + 4] & 0xFFL) << 24;
        val |= (array[offset + 5] & 0xFFL) << 16;
        val |= (array[offset + 6] & 0xFFL) << 8;
        val |= (array[offset + 7] & 0xFFL);

        return val;
    }
}
