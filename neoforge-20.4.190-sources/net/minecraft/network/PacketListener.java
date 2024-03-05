package net.minecraft.network;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/**
 * Describes how packets are handled. There are various implementations of this class for each possible protocol (e.g. PLAY, CLIENTBOUND; PLAY, SERVERBOUND; etc.)
 */
public interface PacketListener {
    PacketFlow flow();

    ConnectionProtocol protocol();

    /**
     * Invoked when disconnecting, the parameter is a Component describing the reason for termination
     */
    void onDisconnect(Component pReason);

    boolean isAcceptingMessages();

    default boolean shouldHandleMessage(Packet<?> pPacket) {
        return this.isAcceptingMessages();
    }

    default boolean shouldPropagateHandlingExceptions() {
        return true;
    }

    default void fillCrashReport(CrashReport pCrashReport) {
        CrashReportCategory crashreportcategory = pCrashReport.addCategory("Connection");
        crashreportcategory.setDetail("Protocol", () -> this.protocol().id());
        crashreportcategory.setDetail("Flow", () -> this.flow().toString());
        this.fillListenerSpecificCrashDetails(crashreportcategory);
    }

    default void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
    }
}
