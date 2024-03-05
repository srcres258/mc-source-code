package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
    @Nullable
    private ByteBuffer fontMemory;
    @Nullable
    private STBTTFontinfo font;
    final float oversample;
    private final IntSet skip = new IntArraySet();
    final float shiftX;
    final float shiftY;
    final float pointScale;
    final float ascent;

    public TrueTypeGlyphProvider(ByteBuffer pFontMemory, STBTTFontinfo pFont, float pHeight, float pOversample, float pShiftX, float pShiftY, String pSkip) {
        this.fontMemory = pFontMemory;
        this.font = pFont;
        this.oversample = pOversample;
        pSkip.codePoints().forEach(this.skip::add);
        this.shiftX = pShiftX * pOversample;
        this.shiftY = pShiftY * pOversample;
        this.pointScale = STBTruetype.stbtt_ScaleForPixelHeight(pFont, pHeight * pOversample);

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            IntBuffer intbuffer = memorystack.mallocInt(1);
            IntBuffer intbuffer1 = memorystack.mallocInt(1);
            IntBuffer intbuffer2 = memorystack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(pFont, intbuffer, intbuffer1, intbuffer2);
            this.ascent = (float)intbuffer.get(0) * this.pointScale;
        }
    }

    @Nullable
    @Override
    public GlyphInfo getGlyph(int pCharacter) {
        STBTTFontinfo stbttfontinfo = this.validateFontOpen();
        if (this.skip.contains(pCharacter)) {
            return null;
        } else {
            GlyphInfo.SpaceGlyphInfo glyphinfo$spaceglyphinfo;
            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                int i = STBTruetype.stbtt_FindGlyphIndex(stbttfontinfo, pCharacter);
                if (i == 0) {
                    return null;
                }

                IntBuffer intbuffer = memorystack.mallocInt(1);
                IntBuffer intbuffer1 = memorystack.mallocInt(1);
                IntBuffer intbuffer2 = memorystack.mallocInt(1);
                IntBuffer intbuffer3 = memorystack.mallocInt(1);
                IntBuffer intbuffer4 = memorystack.mallocInt(1);
                IntBuffer intbuffer5 = memorystack.mallocInt(1);
                STBTruetype.stbtt_GetGlyphHMetrics(stbttfontinfo, i, intbuffer4, intbuffer5);
                STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(
                    stbttfontinfo, i, this.pointScale, this.pointScale, this.shiftX, this.shiftY, intbuffer, intbuffer1, intbuffer2, intbuffer3
                );
                float f = (float)intbuffer4.get(0) * this.pointScale;
                int j = intbuffer2.get(0) - intbuffer.get(0);
                int k = intbuffer3.get(0) - intbuffer1.get(0);
                if (j > 0 && k > 0) {
                    return new TrueTypeGlyphProvider.Glyph(
                        intbuffer.get(0), intbuffer2.get(0), -intbuffer1.get(0), -intbuffer3.get(0), f, (float)intbuffer5.get(0) * this.pointScale, i
                    );
                }

                glyphinfo$spaceglyphinfo = () -> f / this.oversample;
            }

            return glyphinfo$spaceglyphinfo;
        }
    }

    STBTTFontinfo validateFontOpen() {
        if (this.fontMemory != null && this.font != null) {
            return this.font;
        } else {
            throw new IllegalArgumentException("Provider already closed");
        }
    }

    @Override
    public void close() {
        if (this.font != null) {
            this.font.free();
            this.font = null;
        }

        MemoryUtil.memFree(this.fontMemory);
        this.fontMemory = null;
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return IntStream.range(0, 65535)
            .filter(p_231118_ -> !this.skip.contains(p_231118_))
            .collect(IntOpenHashSet::new, IntCollection::add, IntCollection::addAll);
    }

    @OnlyIn(Dist.CLIENT)
    class Glyph implements GlyphInfo {
        final int width;
        final int height;
        final float bearingX;
        final float bearingY;
        private final float advance;
        final int index;

        Glyph(int pMinWidth, int pMaxWidth, int pMinHeight, int pMaxHeight, float pAdvance, float pBearingX, int pIndex) {
            this.width = pMaxWidth - pMinWidth;
            this.height = pMinHeight - pMaxHeight;
            this.advance = pAdvance / TrueTypeGlyphProvider.this.oversample;
            this.bearingX = (pBearingX + (float)pMinWidth + TrueTypeGlyphProvider.this.shiftX) / TrueTypeGlyphProvider.this.oversample;
            this.bearingY = (TrueTypeGlyphProvider.this.ascent - (float)pMinHeight + TrueTypeGlyphProvider.this.shiftY) / TrueTypeGlyphProvider.this.oversample;
            this.index = pIndex;
        }

        @Override
        public float getAdvance() {
            return this.advance;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> pGlyphProvider) {
            return pGlyphProvider.apply(
                new SheetGlyphInfo() {
                    @Override
                    public int getPixelWidth() {
                        return Glyph.this.width;
                    }
    
                    @Override
                    public int getPixelHeight() {
                        return Glyph.this.height;
                    }
    
                    @Override
                    public float getOversample() {
                        return TrueTypeGlyphProvider.this.oversample;
                    }
    
                    @Override
                    public float getBearingX() {
                        return Glyph.this.bearingX;
                    }
    
                    @Override
                    public float getBearingY() {
                        return Glyph.this.bearingY;
                    }
    
                    @Override
                    public void upload(int p_231126_, int p_231127_) {
                        STBTTFontinfo stbttfontinfo = TrueTypeGlyphProvider.this.validateFontOpen();
                        NativeImage nativeimage = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false);
                        nativeimage.copyFromFont(
                            stbttfontinfo,
                            Glyph.this.index,
                            Glyph.this.width,
                            Glyph.this.height,
                            TrueTypeGlyphProvider.this.pointScale,
                            TrueTypeGlyphProvider.this.pointScale,
                            TrueTypeGlyphProvider.this.shiftX,
                            TrueTypeGlyphProvider.this.shiftY,
                            0,
                            0
                        );
                        nativeimage.upload(0, p_231126_, p_231127_, 0, 0, Glyph.this.width, Glyph.this.height, false, true);
                    }
    
                    @Override
                    public boolean isColored() {
                        return false;
                    }
                }
            );
        }
    }
}
