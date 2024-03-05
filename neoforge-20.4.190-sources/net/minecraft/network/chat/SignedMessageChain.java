package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.slf4j.Logger;

public class SignedMessageChain {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private SignedMessageLink nextLink;
    private Instant lastTimeStamp = Instant.EPOCH;

    public SignedMessageChain(UUID pSender, UUID pSessionId) {
        this.nextLink = SignedMessageLink.root(pSender, pSessionId);
    }

    public SignedMessageChain.Encoder encoder(Signer pSigner) {
        return p_248067_ -> {
            SignedMessageLink signedmessagelink = this.advanceLink();
            return signedmessagelink == null
                ? null
                : new MessageSignature(pSigner.sign(p_248065_ -> PlayerChatMessage.updateSignature(p_248065_, signedmessagelink, p_248067_)));
        };
    }

    public SignedMessageChain.Decoder decoder(ProfilePublicKey pPublicKey) {
        SignatureValidator signaturevalidator = pPublicKey.createSignatureValidator();
        return (p_314904_, p_314905_) -> {
            SignedMessageLink signedmessagelink = this.advanceLink();
            if (signedmessagelink == null) {
                throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.chain_broken"), false);
            } else if (pPublicKey.data().hasExpired()) {
                throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.expiredProfileKey"), false);
            } else if (p_314905_.timeStamp().isBefore(this.lastTimeStamp)) {
                throw new SignedMessageChain.DecodeException(Component.translatable("multiplayer.disconnect.out_of_order_chat"), true);
            } else {
                this.lastTimeStamp = p_314905_.timeStamp();
                PlayerChatMessage playerchatmessage = new PlayerChatMessage(signedmessagelink, p_314904_, p_314905_, null, FilterMask.PASS_THROUGH);
                if (!playerchatmessage.verify(signaturevalidator)) {
                    throw new SignedMessageChain.DecodeException(Component.translatable("multiplayer.disconnect.unsigned_chat"), true);
                } else {
                    if (playerchatmessage.hasExpiredServer(Instant.now())) {
                        LOGGER.warn("Received expired chat: '{}'. Is the client/server system time unsynchronized?", p_314905_.content());
                    }

                    return playerchatmessage;
                }
            }
        };
    }

    @Nullable
    private SignedMessageLink advanceLink() {
        SignedMessageLink signedmessagelink = this.nextLink;
        if (signedmessagelink != null) {
            this.nextLink = signedmessagelink.advance();
        }

        return signedmessagelink;
    }

    public static class DecodeException extends ThrowingComponent {
        private final boolean shouldDisconnect;

        public DecodeException(Component pComponent, boolean pShouldDisconnect) {
            super(pComponent);
            this.shouldDisconnect = pShouldDisconnect;
        }

        public boolean shouldDisconnect() {
            return this.shouldDisconnect;
        }
    }

    @FunctionalInterface
    public interface Decoder {
        static SignedMessageChain.Decoder unsigned(UUID pId, BooleanSupplier pShouldEnforceSecureProfile) {
            return (p_314908_, p_314909_) -> {
                if (pShouldEnforceSecureProfile.getAsBoolean()) {
                    throw new SignedMessageChain.DecodeException(Component.translatable("chat.disabled.missingProfileKey"), false);
                } else {
                    return PlayerChatMessage.unsigned(pId, p_314909_.content());
                }
            };
        }

        PlayerChatMessage unpack(@Nullable MessageSignature pSignature, SignedMessageBody pBody) throws SignedMessageChain.DecodeException;
    }

    @FunctionalInterface
    public interface Encoder {
        SignedMessageChain.Encoder UNSIGNED = p_250548_ -> null;

        @Nullable
        MessageSignature pack(SignedMessageBody pBody);
    }
}
