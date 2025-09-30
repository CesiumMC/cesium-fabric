package de.yamayaki.cesium.mixin.gui;

import de.yamayaki.cesium.api.accessor.DatabaseSource;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.lmdbjava.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(DebugScreenEntries.class)
public class MixinDebugScreenEntries {
    @Unique
    private static final ResourceLocation CESIUM_STATS = DebugScreenEntries.register(ResourceLocation.fromNamespaceAndPath("cesium", "lmdb_stats"), (debugScreenDisplayer, level, levelChunk, levelChunk2) -> {
        if(!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final List<Stat> stats = ((DatabaseSource) serverLevel)
                .cesium$getStorage()
                .getStats();

        final int ms_depth = stats.stream().mapToInt(es -> es.depth).max().orElse(0);
        final long ms_branch_pages = stats.stream().mapToLong(es -> es.branchPages).sum();
        final long ms_leaf_pages = stats.stream().mapToLong(es -> es.leafPages).sum();
        final long ms_entries = stats.stream().mapToLong(es -> es.entries).sum();

        debugScreenDisplayer.addLine("ms_depth: " + ms_depth);
        debugScreenDisplayer.addLine("ms_branch_pages: " + ms_branch_pages);
        debugScreenDisplayer.addLine("ms_leaf_pages: " + ms_leaf_pages);
        debugScreenDisplayer.addLine("ms_entries: " + ms_entries);
    });
}
