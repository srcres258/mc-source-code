package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import java.util.List;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundleUnpacker extends MessageToMessageEncoder<Packet<?>> {
    private final AttributeKey<? extends BundlerInfo.Provider> bundlerAttributeKey;

    public PacketBundleUnpacker(AttributeKey<? extends BundlerInfo.Provider> pBundleAttributeKey) {
        this.bundlerAttributeKey = pBundleAttributeKey;
    }

    protected void encode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> p_265735_) throws Exception {
        BundlerInfo.Provider bundlerinfo$provider = pContext.channel().attr(this.bundlerAttributeKey).get();
        if (bundlerinfo$provider == null) {
            throw new EncoderException("Bundler not configured: " + pPacket);
        } else {
            bundlerinfo$provider.bundlerInfo().unbundlePacket(pPacket, p_265735_::add, pContext);
        }
    }
}
