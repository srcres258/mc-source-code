package net.minecraft.client;

import com.mojang.util.UndashedUuid;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class User {
    private final String name;
    private final UUID uuid;
    private final String accessToken;
    private final Optional<String> xuid;
    private final Optional<String> clientId;
    private final User.Type type;

    public User(String pName, UUID pUuid, String pAccessToken, Optional<String> pXuid, Optional<String> pClientId, User.Type pType) {
        if (pName == null || pName.isEmpty()) {
            pName = "MissingName";
            pUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
            pAccessToken = "NotValid";
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass().getName());
            logger.warn("=========================================================");
            logger.warn("WARNING!! the username was not set for this session, typically");
            logger.warn("this means you installed Forge incorrectly. We have set your");
            logger.warn("name to \"MissingName\" and your session to nothing. Please");
            logger.warn("check your installation and post a console log from the launcher");
            logger.warn("when asking for help!");
            logger.warn("=========================================================");
        }
        this.name = pName;
        this.uuid = pUuid;
        this.accessToken = pAccessToken;
        this.xuid = pXuid;
        this.clientId = pClientId;
        this.type = pType;
    }

    public String getSessionId() {
        return "token:" + this.accessToken + ":" + UndashedUuid.toString(this.uuid);
    }

    public UUID getProfileId() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public Optional<String> getClientId() {
        return this.clientId;
    }

    public Optional<String> getXuid() {
        return this.xuid;
    }

    public User.Type getType() {
        return this.type;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        LEGACY("legacy"),
        MOJANG("mojang"),
        MSA("msa");

        private static final Map<String, User.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(p_92560_ -> p_92560_.name, Function.identity()));
        private final String name;

        private Type(String pName) {
            this.name = pName;
        }

        @Nullable
        public static User.Type byName(String pTypeName) {
            return BY_NAME.get(pTypeName.toLowerCase(Locale.ROOT));
        }

        public String getName() {
            return this.name;
        }
    }
}
