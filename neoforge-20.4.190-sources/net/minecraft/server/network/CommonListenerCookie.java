package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;

public record CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, net.neoforged.neoforge.network.connection.ConnectionType connectionType) {

    /**
     * @deprecated Use {@link #CommonListenerCookie(GameProfile, int, ClientInformation, net.neoforged.neoforge.network.connection.ConnectionType)} instead, to indicate whether the connection is modded.
     */
    @Deprecated
    public CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation) {
        this(gameProfile, latency, clientInformation, net.neoforged.neoforge.network.connection.ConnectionType.VANILLA);
    }

    public static CommonListenerCookie createInitial(GameProfile pGameProfile) {
        return new CommonListenerCookie(pGameProfile, 0, ClientInformation.createDefault());
    }
}
