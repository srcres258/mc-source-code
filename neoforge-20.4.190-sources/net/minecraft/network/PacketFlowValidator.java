package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public class PacketFlowValidator extends MessageToMessageCodec<Packet<?>, Packet<?>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AttributeKey<ConnectionProtocol.CodecData<?>> decoderKey;
    private final AttributeKey<ConnectionProtocol.CodecData<?>> encoderKey;

    public PacketFlowValidator(AttributeKey<ConnectionProtocol.CodecData<?>> pDecoderKey, AttributeKey<ConnectionProtocol.CodecData<?>> pEncoderKey) {
        this.decoderKey = pDecoderKey;
        this.encoderKey = pEncoderKey;
    }

    private static void validatePacket(
        ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> pOutput, AttributeKey<ConnectionProtocol.CodecData<?>> pKey
    ) {
        Attribute<ConnectionProtocol.CodecData<?>> attribute = pContext.channel().attr(pKey);
        ConnectionProtocol.CodecData<?> codecdata = attribute.get();
        if (!codecdata.isValidPacketType(pPacket)) {
            LOGGER.error("Unrecognized packet in pipeline {}:{} - {}", codecdata.protocol().id(), codecdata.flow(), pPacket);
        }

        ReferenceCountUtil.retain(pPacket);
        pOutput.add(pPacket);
        ProtocolSwapHandler.swapProtocolIfNeeded(attribute, pPacket);
    }

    protected void decode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> pOutput) throws Exception {
        validatePacket(pContext, pPacket, pOutput, this.decoderKey);
    }

    protected void encode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> pOutput) throws Exception {
        validatePacket(pContext, pPacket, pOutput, this.encoderKey);
    }
}
