package net.minecraft.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.util.UndashedUuid;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.Util;

public final class UUIDUtil {
    public static final Codec<UUID> CODEC = Codec.INT_STREAM
        .comapFlatMap(p_235884_ -> Util.fixedSize(p_235884_, 4).map(UUIDUtil::uuidFromIntArray), p_235888_ -> Arrays.stream(uuidToIntArray(p_235888_)));
    public static final Codec<Set<UUID>> CODEC_SET = Codec.list(CODEC).xmap(Sets::newHashSet, Lists::newArrayList);
    public static final Codec<UUID> STRING_CODEC = Codec.STRING.comapFlatMap(p_274732_ -> {
        try {
            return DataResult.success(UUID.fromString(p_274732_), Lifecycle.stable());
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Invalid UUID " + p_274732_ + ": " + illegalargumentexception.getMessage());
        }
    }, UUID::toString);
    public static Codec<UUID> AUTHLIB_CODEC = Codec.either(CODEC, Codec.STRING.comapFlatMap(p_293693_ -> {
        try {
            return DataResult.success(UndashedUuid.fromStringLenient(p_293693_), Lifecycle.stable());
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Invalid UUID " + p_293693_ + ": " + illegalargumentexception.getMessage());
        }
    }, UndashedUuid::toString)).xmap(p_304145_ -> p_304145_.map(p_253361_ -> p_253361_, p_304144_ -> p_304144_), Either::right);
    public static Codec<UUID> LENIENT_CODEC = Codec.either(CODEC, STRING_CODEC)
        .xmap(p_253364_ -> p_253364_.map(p_304143_ -> p_304143_, p_253362_ -> p_253362_), Either::left);
    public static final int UUID_BYTES = 16;
    private static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";

    private UUIDUtil() {
    }

    public static UUID uuidFromIntArray(int[] p_235886_) {
        return new UUID((long)p_235886_[0] << 32 | (long)p_235886_[1] & 4294967295L, (long)p_235886_[2] << 32 | (long)p_235886_[3] & 4294967295L);
    }

    public static int[] uuidToIntArray(UUID pUuid) {
        long i = pUuid.getMostSignificantBits();
        long j = pUuid.getLeastSignificantBits();
        return leastMostToIntArray(i, j);
    }

    private static int[] leastMostToIntArray(long pMost, long pLeast) {
        return new int[]{(int)(pMost >> 32), (int)pMost, (int)(pLeast >> 32), (int)pLeast};
    }

    public static byte[] uuidToByteArray(UUID pUuid) {
        byte[] abyte = new byte[16];
        ByteBuffer.wrap(abyte).order(ByteOrder.BIG_ENDIAN).putLong(pUuid.getMostSignificantBits()).putLong(pUuid.getLeastSignificantBits());
        return abyte;
    }

    public static UUID readUUID(Dynamic<?> pDynamic) {
        int[] aint = pDynamic.asIntStream().toArray();
        if (aint.length != 4) {
            throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + aint.length + ".");
        } else {
            return uuidFromIntArray(aint);
        }
    }

    public static UUID createOfflinePlayerUUID(String pUsername) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + pUsername).getBytes(StandardCharsets.UTF_8));
    }

    public static GameProfile createOfflineProfile(String pUsername) {
        UUID uuid = createOfflinePlayerUUID(pUsername);
        return new GameProfile(uuid, pUsername);
    }
}
