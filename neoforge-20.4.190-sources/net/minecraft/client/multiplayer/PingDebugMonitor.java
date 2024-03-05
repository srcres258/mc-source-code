package net.minecraft.client.multiplayer;

import net.minecraft.Util;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.util.SampleLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PingDebugMonitor {
    private final ClientPacketListener connection;
    private final SampleLogger delayTimer;

    public PingDebugMonitor(ClientPacketListener pConnection, SampleLogger pDelayTimer) {
        this.connection = pConnection;
        this.delayTimer = pDelayTimer;
    }

    public void tick() {
        this.connection.send(new ServerboundPingRequestPacket(Util.getMillis()));
    }

    public void onPongReceived(ClientboundPongResponsePacket pPacket) {
        this.delayTimer.logSample(Util.getMillis() - pPacket.getTime());
    }
}
