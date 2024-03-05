package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public interface RealmsError {
    Component NO_MESSAGE = Component.translatable("mco.errorMessage.noDetails");
    Logger LOGGER = LogUtils.getLogger();

    int errorCode();

    Component errorMessage();

    String logMessage();

    static RealmsError parse(int pHttpCode, String pPayload) {
        if (pHttpCode == 429) {
            return RealmsError.CustomError.SERVICE_BUSY;
        } else if (Strings.isNullOrEmpty(pPayload)) {
            return RealmsError.CustomError.noPayload(pHttpCode);
        } else {
            try {
                JsonObject jsonobject = JsonParser.parseString(pPayload).getAsJsonObject();
                String s = GsonHelper.getAsString(jsonobject, "reason", null);
                String s1 = GsonHelper.getAsString(jsonobject, "errorMsg", null);
                int i = GsonHelper.getAsInt(jsonobject, "errorCode", -1);
                if (s1 != null || s != null || i != -1) {
                    return new RealmsError.ErrorWithJsonPayload(pHttpCode, i != -1 ? i : pHttpCode, s, s1);
                }
            } catch (Exception exception) {
                LOGGER.error("Could not parse RealmsError", (Throwable)exception);
            }

            return new RealmsError.ErrorWithRawPayload(pHttpCode, pPayload);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record AuthenticationError(String message) implements RealmsError {
        public static final int ERROR_CODE = 401;

        @Override
        public int errorCode() {
            return 401;
        }

        @Override
        public Component errorMessage() {
            return Component.literal(this.message);
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms authentication error with message '%s'", this.message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record CustomError(int httpCode, @Nullable Component payload) implements RealmsError {
        public static final RealmsError.CustomError SERVICE_BUSY = new RealmsError.CustomError(429, Component.translatable("mco.errorMessage.serviceBusy"));
        public static final Component RETRY_MESSAGE = Component.translatable("mco.errorMessage.retry");

        public static RealmsError.CustomError unknownCompatibilityResponse(String pPayload) {
            return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.unknownCompatibility", pPayload));
        }

        public static RealmsError.CustomError connectivityError(RealmsHttpException pPayload) {
            return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.connectivity", pPayload.getMessage()));
        }

        public static RealmsError.CustomError retry(int pHttpCode) {
            return new RealmsError.CustomError(pHttpCode, RETRY_MESSAGE);
        }

        public static RealmsError.CustomError noPayload(int pHttpCode) {
            return new RealmsError.CustomError(pHttpCode, null);
        }

        @Override
        public int errorCode() {
            return this.httpCode;
        }

        @Override
        public Component errorMessage() {
            return this.payload != null ? this.payload : NO_MESSAGE;
        }

        @Override
        public String logMessage() {
            return this.payload != null
                ? String.format(Locale.ROOT, "Realms service error (%d) with message '%s'", this.httpCode, this.payload.getString())
                : String.format(Locale.ROOT, "Realms service error (%d) with no payload", this.httpCode);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record ErrorWithJsonPayload(int httpCode, int code, @Nullable String reason, @Nullable String message) implements RealmsError {
        @Override
        public int errorCode() {
            return this.code;
        }

        @Override
        public Component errorMessage() {
            String s = "mco.errorMessage." + this.code;
            if (I18n.exists(s)) {
                return Component.translatable(s);
            } else {
                if (this.reason != null) {
                    String s1 = "mco.errorReason." + this.reason;
                    if (I18n.exists(s1)) {
                        return Component.translatable(s1);
                    }
                }

                return (Component)(this.message != null ? Component.literal(this.message) : NO_MESSAGE);
            }
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms service error (%d/%d/%s) with message '%s'", this.httpCode, this.code, this.reason, this.message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record ErrorWithRawPayload(int httpCode, String payload) implements RealmsError {
        @Override
        public int errorCode() {
            return this.httpCode;
        }

        @Override
        public Component errorMessage() {
            return Component.literal(this.payload);
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms service error (%d) with raw payload '%s'", this.httpCode, this.payload);
        }
    }
}
