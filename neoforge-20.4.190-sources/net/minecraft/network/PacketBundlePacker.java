package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundlePacker extends MessageToMessageDecoder<Packet<?>> {
    @Nullable
    private BundlerInfo.Bundler currentBundler;
    @Nullable
    private BundlerInfo infoForCurrentBundler;
    private final AttributeKey<? extends BundlerInfo.Provider> bundlerAttributeKey;

    public PacketBundlePacker(AttributeKey<? extends BundlerInfo.Provider> pBundleAttributeKey) {
        this.bundlerAttributeKey = pBundleAttributeKey;
    }

    protected void decode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> p_265368_) throws Exception {
        BundlerInfo.Provider bundlerinfo$provider = pContext.channel().attr(this.bundlerAttributeKey).get();
        if (bundlerinfo$provider == null) {
            throw new DecoderException("Bundler not configured: " + pPacket);
        } else {
            BundlerInfo bundlerinfo = bundlerinfo$provider.bundlerInfo();
            if (this.currentBundler != null) {
                if (this.infoForCurrentBundler != bundlerinfo) {
                    throw new DecoderException("Bundler handler changed during bundling");
                }

                Packet<?> packet = this.currentBundler.addPacket(pPacket);
                if (packet != null) {
                    this.infoForCurrentBundler = null;
                    this.currentBundler = null;
                    p_265368_.add(packet);
                }
            } else {
                BundlerInfo.Bundler bundlerinfo$bundler = bundlerinfo.startPacketBundling(pPacket);
                if (bundlerinfo$bundler != null) {
                    this.currentBundler = bundlerinfo$bundler;
                    this.infoForCurrentBundler = bundlerinfo;
                } else {
                    p_265368_.add(pPacket);
                }
            }
        }
    }
}
