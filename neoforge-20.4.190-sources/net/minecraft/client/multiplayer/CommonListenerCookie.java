package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CommonListenerCookie(
    GameProfile localGameProfile,
    WorldSessionTelemetryManager telemetryManager,
    RegistryAccess.Frozen receivedRegistries,
    FeatureFlagSet enabledFeatures,
    @Nullable String serverBrand,
    @Nullable ServerData serverData,
    @Nullable Screen postDisconnectScreen,
    net.neoforged.neoforge.network.connection.ConnectionType connectionType
) {

    /**
     * @deprecated Use {@link #CommonListenerCookie(GameProfile, WorldSessionTelemetryManager, RegistryAccess.Frozen, FeatureFlagSet, String, ServerData, Screen, net.neoforged.neoforge.network.connection.ConnectionType)} instead, to indicate whether the connection is modded.
     */
    @Deprecated
    public CommonListenerCookie(GameProfile localGameProfile, WorldSessionTelemetryManager telemetryManager, RegistryAccess.Frozen receivedRegistries, FeatureFlagSet enabledFeatures, @Nullable String serverBrand, @Nullable ServerData serverData, @Nullable Screen postDisconnectScreen) {
        this(localGameProfile, telemetryManager, receivedRegistries, enabledFeatures, serverBrand, serverData, postDisconnectScreen, net.neoforged.neoforge.network.connection.ConnectionType.VANILLA);
    }
}
