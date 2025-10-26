package de.yamayaki.cesium.mixin.core.players;

import com.llamalad7.mixinextras.sugar.Local;
import de.yamayaki.cesium.accessor.DatabaseSetter;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.PlayerDataStorage;
//? <= 1.20.6 {
/*import net.minecraft.world.entity.player.Player;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(PlayerDataStorage.class)
public class MixinPlayerDataStorage implements DatabaseSetter {
    @Unique private LMDBInstance database;

    @Override
    public void cesium$setStorage(LMDBInstance storage) {
        this.database = storage;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;mkdirs()Z"
            )
    )
    private boolean disableMkdirs(File file) {
        return true;
    }

    @Redirect(
            //? if >= 1.21.9 {
            method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
            //?} elif >= 1.20.6 {
            /*method = "load(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)Ljava/util/Optional;",
            *///?} else {
            /*method = "load(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/nbt/CompoundTag;",
            *///?}
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            )
    )
    public boolean redirectFileExists(File instance) {
        return true;
    }

    @Redirect(
            //? if >= 1.21.9 {
            method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
            //?} elif >= 1.20.6 {
            /*method = "load(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)Ljava/util/Optional;",
            *///?} else {
            /*method = "load",
            *///?}
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;isFile()Z"
            )
    )
    public boolean redirectFileIsFile(File instance) {
        return true;
    }

    @Redirect(
            //? if >= 1.21.9 {
            method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
            //?} elif >= 1.20.6 {
            /*method = "load(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)Ljava/util/Optional;",
            *///?} else {
            /*method = "load",
            *///?}
            at = @At(
                    value = "INVOKE",
                    //? >= 1.20.4 {
                    target = "Lnet/minecraft/nbt/NbtIo;readCompressed(Ljava/nio/file/Path;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;"
                    //?} else {
                    /*target = "Lnet/minecraft/nbt/NbtIo;readCompressed(Ljava/io/File;)Lnet/minecraft/nbt/CompoundTag;"
                    *///?}
            )
    )
    //? >= 1.20.4 {
    public CompoundTag redirectPlayerLoad(
            java.nio.file.Path path, net.minecraft.nbt.NbtAccounter nbtAccounter,
            //? if >= 1.21.9 {
            @Local(argsOnly = true) net.minecraft.server.players.NameAndId player
            //?} else {
            /*@Local(argsOnly = true) net.minecraft.world.entity.player.Player player
            *///?}
    ) {
    //?} else {
    /*public CompoundTag redirectPlayerLoad(File file, @Local(argsOnly = true) Player player) {
    *///?}
        return this.database
                .getDatabase(PlayerDatabaseSpecs.PLAYER_DATA)
                .getValue(player/*? >= 1.21.9 {*/ .id() /*?} else {*/ /*.getUUID() *//*?}*/);
    }

    @Redirect(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    //? >= 1.20.4 {
                    target = "Ljava/nio/file/Files;createTempFile(Ljava/nio/file/Path;Ljava/lang/String;Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;"
                    //?} else {
                    /*target = "Ljava/io/File;createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;"
                    *///?}
            )
    )
    //? >= 1.20.4 {
    public java.nio.file.Path disableFileCreation(java.nio.file.Path path, String a, String b, java.nio.file.attribute.FileAttribute<?>[] fileAttributes) {
    //?} else {
    /*public File disableFileCreation(String se, String prefix, File suffix) {
    *///?}
        return null;
    }

    @Redirect(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    //? >= 1.20.4 {
                    target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V"
                    //?} else {
                    /*target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"
                    *///?}
            )
    )
    //? >= 1.20.4 {
    public void redirectWrite(CompoundTag compoundTag, java.nio.file.Path path, @Local(argsOnly = true) net.minecraft.world.entity.player.Player player) {
    //?} else {
    /*public void redirectWrite(CompoundTag compoundTag, File file, @Local(argsOnly = true) Player player) {
    *///?}
        this.database
                .getDatabase(PlayerDatabaseSpecs.PLAYER_DATA)
                .stageChange(player.getUUID(), compoundTag);
    }

    @Redirect(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    //? >= 1.20.4 {
                    target = "Lnet/minecraft/Util;safeReplaceFile(Ljava/nio/file/Path;Ljava/nio/file/Path;Ljava/nio/file/Path;)V"
                    //?} else {
                    /*target = "Lnet/minecraft/Util;safeReplaceFile(Ljava/io/File;Ljava/io/File;Ljava/io/File;)V"
                    *///?}
            )
    )
    //? >=1.20.4 {
    public void disableFileMove(java.nio.file.Path path, java.nio.file.Path path2, java.nio.file.Path path3) {
    //?} else {
    /*public void disableFileMove(File file, File file2, File file3) {
    *///?}
        // Do nothing
    }
}
