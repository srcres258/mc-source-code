package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
    static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftSessionService sessionService;
    private final LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> skinCache;
    private final SkinManager.TextureCache skinTextures;
    private final SkinManager.TextureCache capeTextures;
    private final SkinManager.TextureCache elytraTextures;

    public SkinManager(TextureManager pTextureManager, Path pRoot, final MinecraftSessionService pSessionService, final Executor pExecutor) {
        this.sessionService = pSessionService;
        this.skinTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.SKIN);
        this.capeTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.CAPE);
        this.elytraTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.ELYTRA);
        this.skinCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15L))
            .build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
                public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey p_296373_) {
                    return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                        Property property = p_296373_.packedTextures();
                        if (property == null) {
                            return MinecraftProfileTextures.EMPTY;
                        } else {
                            MinecraftProfileTextures minecraftprofiletextures = pSessionService.unpackTextures(property);
                            if (minecraftprofiletextures.signatureState() == SignatureState.INVALID) {
                                SkinManager.LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", p_296373_.profileId());
                            }
    
                            return minecraftprofiletextures;
                        }
                    }, Util.backgroundExecutor()).thenComposeAsync(p_307130_ -> SkinManager.this.registerTextures(p_296373_.profileId(), p_307130_), pExecutor);
                }
            });
    }

    public Supplier<PlayerSkin> lookupInsecure(GameProfile pProfile) {
        CompletableFuture<PlayerSkin> completablefuture = this.getOrLoad(pProfile);
        PlayerSkin playerskin = DefaultPlayerSkin.get(pProfile);
        return () -> completablefuture.getNow(playerskin);
    }

    public PlayerSkin getInsecureSkin(GameProfile pProfile) {
        PlayerSkin playerskin = this.getOrLoad(pProfile).getNow(null);
        return playerskin != null ? playerskin : DefaultPlayerSkin.get(pProfile);
    }

    public CompletableFuture<PlayerSkin> getOrLoad(GameProfile pProfile) {
        Property property = this.sessionService.getPackedTextures(pProfile);
        return this.skinCache.getUnchecked(new SkinManager.CacheKey(pProfile.getId(), property));
    }

    CompletableFuture<PlayerSkin> registerTextures(UUID p_307544_, MinecraftProfileTextures p_307606_) {
        MinecraftProfileTexture minecraftprofiletexture = p_307606_.skin();
        CompletableFuture<ResourceLocation> completablefuture;
        PlayerSkin.Model playerskin$model;
        if (minecraftprofiletexture != null) {
            completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
            playerskin$model = PlayerSkin.Model.byName(minecraftprofiletexture.getMetadata("model"));
        } else {
            PlayerSkin playerskin = DefaultPlayerSkin.get(p_307544_);
            completablefuture = CompletableFuture.completedFuture(playerskin.texture());
            playerskin$model = playerskin.model();
        }

        String s = Optionull.map(minecraftprofiletexture, MinecraftProfileTexture::getUrl);
        MinecraftProfileTexture minecraftprofiletexture1 = p_307606_.cape();
        CompletableFuture<ResourceLocation> completablefuture1 = minecraftprofiletexture1 != null
            ? this.capeTextures.getOrLoad(minecraftprofiletexture1)
            : CompletableFuture.completedFuture(null);
        MinecraftProfileTexture minecraftprofiletexture2 = p_307606_.elytra();
        CompletableFuture<ResourceLocation> completablefuture2 = minecraftprofiletexture2 != null
            ? this.elytraTextures.getOrLoad(minecraftprofiletexture2)
            : CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2)
            .thenApply(
                p_307126_ -> new PlayerSkin(
                        completablefuture.join(),
                        s,
                        completablefuture1.join(),
                        completablefuture2.join(),
                        playerskin$model,
                        p_307606_.signatureState() == SignatureState.SIGNED
                    )
            );
    }

    @OnlyIn(Dist.CLIENT)
    static record CacheKey(UUID profileId, @Nullable Property packedTextures) {
    }

    @OnlyIn(Dist.CLIENT)
    static class TextureCache {
        private final TextureManager textureManager;
        private final Path root;
        private final Type type;
        private final Map<String, CompletableFuture<ResourceLocation>> textures = new Object2ObjectOpenHashMap<>();

        TextureCache(TextureManager pTextureManager, Path pRoot, Type pType) {
            this.textureManager = pTextureManager;
            this.root = pRoot;
            this.type = pType;
        }

        public CompletableFuture<ResourceLocation> getOrLoad(MinecraftProfileTexture pTexture) {
            String s = pTexture.getHash();
            CompletableFuture<ResourceLocation> completablefuture = this.textures.get(s);
            if (completablefuture == null) {
                completablefuture = this.registerTexture(pTexture);
                this.textures.put(s, completablefuture);
            }

            return completablefuture;
        }

        private CompletableFuture<ResourceLocation> registerTexture(MinecraftProfileTexture pTexture) {
            String s = Hashing.sha1().hashUnencodedChars(pTexture.getHash()).toString();
            ResourceLocation resourcelocation = this.getTextureLocation(s);
            Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
            CompletableFuture<ResourceLocation> completablefuture = new CompletableFuture<>();
            HttpTexture httptexture = new HttpTexture(
                path.toFile(),
                pTexture.getUrl(),
                DefaultPlayerSkin.getDefaultTexture(),
                this.type == Type.SKIN,
                () -> completablefuture.complete(resourcelocation)
            );
            this.textureManager.register(resourcelocation, httptexture);
            return completablefuture;
        }

        private ResourceLocation getTextureLocation(String pName) {
            String s = switch(this.type) {
                case SKIN -> "skins";
                case CAPE -> "capes";
                case ELYTRA -> "elytra";
            };
            return new ResourceLocation(s + "/" + pName);
        }
    }
}
