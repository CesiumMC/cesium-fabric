package de.yamayaki.cesium.common;

import de.yamayaki.cesium.api.ISerializer;
import de.yamayaki.cesium.common.serializer.ChunkPosSerializer;
import de.yamayaki.cesium.common.serializer.CompoundTagSerializer;
import de.yamayaki.cesium.common.serializer.StringSerializer;
import de.yamayaki.cesium.common.serializer.UUIDSerializer;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class DefaultSerializers {
    private static final Reference2ReferenceMap<Class<?>, ISerializer<?>> serializers = new Reference2ReferenceOpenHashMap<>();

    static {
        serializers.put(UUID.class, new UUIDSerializer());
        serializers.put(ChunkPos.class, new ChunkPosSerializer());

        serializers.put(CompoundTag.class, new CompoundTagSerializer());
        serializers.put(String.class, new StringSerializer());
    }

    @SuppressWarnings("unchecked")
    public static <K> ISerializer<K> getSerializer(Class<K> clazz) {
        ISerializer<?> serializer = DefaultSerializers.serializers.get(clazz);

        if (serializer == null) {
            throw new NullPointerException("No serializer exists for type: " + clazz.getName());
        }

        return (ISerializer<K>) serializer;
    }
}
