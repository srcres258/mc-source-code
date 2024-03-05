package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlas extends AbstractTexture implements Dumpable, Tickable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated
    public static final ResourceLocation LOCATION_BLOCKS = InventoryMenu.BLOCK_ATLAS;
    @Deprecated
    public static final ResourceLocation LOCATION_PARTICLES = new ResourceLocation("textures/atlas/particles.png");
    private List<SpriteContents> sprites = List.of();
    private List<TextureAtlasSprite.Ticker> animatedTextures = List.of();
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName = Map.of();
    @Nullable
    private TextureAtlasSprite missingSprite;
    private final ResourceLocation location;
    private final int maxSupportedTextureSize;
    private int width;
    private int height;
    private int mipLevel;

    public TextureAtlas(ResourceLocation pLocation) {
        this.location = pLocation;
        this.maxSupportedTextureSize = RenderSystem.maxSupportedTextureSize();
    }

    @Override
    public void load(ResourceManager pResourceManager) {
    }

    public void upload(SpriteLoader.Preparations pPreparations) {
        LOGGER.info("Created: {}x{}x{} {}-atlas", pPreparations.width(), pPreparations.height(), pPreparations.mipLevel(), this.location);
        TextureUtil.prepareImage(this.getId(), pPreparations.mipLevel(), pPreparations.width(), pPreparations.height());
        this.width = pPreparations.width();
        this.height = pPreparations.height();
        this.mipLevel = pPreparations.mipLevel();
        this.clearTextureData();
        this.texturesByName = Map.copyOf(pPreparations.regions());
        this.missingSprite = this.texturesByName.get(MissingTextureAtlasSprite.getLocation());
        if (this.missingSprite == null) {
            throw new IllegalStateException("Atlas '" + this.location + "' (" + this.texturesByName.size() + " sprites) has no missing texture sprite");
        } else {
            List<SpriteContents> list = new ArrayList<>();
            List<TextureAtlasSprite.Ticker> list1 = new ArrayList<>();

            for(TextureAtlasSprite textureatlassprite : pPreparations.regions().values()) {
                list.add(textureatlassprite.contents());

                try {
                    textureatlassprite.uploadFirstFrame();
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Stitching texture atlas");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Texture being stitched together");
                    crashreportcategory.setDetail("Atlas path", this.location);
                    crashreportcategory.setDetail("Sprite", textureatlassprite);
                    throw new ReportedException(crashreport);
                }

                TextureAtlasSprite.Ticker textureatlassprite$ticker = textureatlassprite.createTicker();
                if (textureatlassprite$ticker != null) {
                    list1.add(textureatlassprite$ticker);
                }
            }

            this.sprites = List.copyOf(list);
            this.animatedTextures = List.copyOf(list1);
        }

        net.neoforged.neoforge.client.ClientHooks.onTextureAtlasStitched(this);
    }

    @Override
    public void dumpContents(ResourceLocation pResourceLocation, Path pPath) throws IOException {
        String s = pResourceLocation.toDebugFileName();
        TextureUtil.writeAsPNG(pPath, s, this.getId(), this.mipLevel, this.width, this.height);
        dumpSpriteNames(pPath, s, this.texturesByName);
    }

    private static void dumpSpriteNames(Path pOutputDir, String pOutputFilename, Map<ResourceLocation, TextureAtlasSprite> p_261722_) {
        Path path = pOutputDir.resolve(pOutputFilename + ".txt");

        try (Writer writer = Files.newBufferedWriter(path)) {
            for(Entry<ResourceLocation, TextureAtlasSprite> entry : p_261722_.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
                TextureAtlasSprite textureatlassprite = entry.getValue();
                writer.write(
                    String.format(
                        Locale.ROOT,
                        "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                        entry.getKey(),
                        textureatlassprite.getX(),
                        textureatlassprite.getY(),
                        textureatlassprite.contents().width(),
                        textureatlassprite.contents().height()
                    )
                );
            }
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to write file {}", path, ioexception);
        }
    }

    public void cycleAnimationFrames() {
        this.bind();

        for(TextureAtlasSprite.Ticker textureatlassprite$ticker : this.animatedTextures) {
            textureatlassprite$ticker.tickAndUpload();
        }
    }

    @Override
    public void tick() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::cycleAnimationFrames);
        } else {
            this.cycleAnimationFrames();
        }
    }

    public TextureAtlasSprite getSprite(ResourceLocation pName) {
        TextureAtlasSprite textureatlassprite = this.texturesByName.getOrDefault(pName, this.missingSprite);
        if (textureatlassprite == null) {
            throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
        } else {
            return textureatlassprite;
        }
    }

    public void clearTextureData() {
        this.sprites.forEach(SpriteContents::close);
        this.animatedTextures.forEach(TextureAtlasSprite.Ticker::close);
        this.sprites = List.of();
        this.animatedTextures = List.of();
        this.texturesByName = Map.of();
        this.missingSprite = null;
    }

    public ResourceLocation location() {
        return this.location;
    }

    public int maxSupportedTextureSize() {
        return this.maxSupportedTextureSize;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    public void updateFilter(SpriteLoader.Preparations pPreparations) {
        this.setFilter(false, pPreparations.mipLevel() > 0);
    }

    public Map<ResourceLocation, TextureAtlasSprite> getTextures() {
        return java.util.Collections.unmodifiableMap(texturesByName);
    }
}
