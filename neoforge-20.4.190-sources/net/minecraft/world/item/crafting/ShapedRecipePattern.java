package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.CraftingContainer;

public record ShapedRecipePattern(int width, int height, NonNullList<Ingredient> ingredients, Optional<ShapedRecipePattern.Data> data) {
    /** @deprecated Neo: use {@link #getMaxWidth} and {@link #getMaxHeight} */ @Deprecated
    private static final int MAX_SIZE = 3;
    static int maxWidth = 3;
    static int maxHeight = 3;

    public static int getMaxWidth() {
        return maxWidth;
    }

    public static int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Expand the max width and height allowed in the deserializer.
     * This should be called by modders who add custom crafting tables that are larger than the vanilla 3x3.
     * @param width your max recipe width
     * @param height your max recipe height
     */
    public static void setCraftingSize(int width, int height) {
        if (maxWidth < width) maxWidth = width;
        if (maxHeight < height) maxHeight = height;
    }

    public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
        .flatXmap(
            ShapedRecipePattern::unpack,
            p_311830_ -> p_311830_.data().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
        );

    public static ShapedRecipePattern of(Map<Character, Ingredient> pKey, String... pPattern) {
        return of(pKey, List.of(pPattern));
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> pKey, List<String> pPattern) {
        ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(pKey, pPattern);
        return Util.getOrThrow(unpack(shapedrecipepattern$data), IllegalArgumentException::new);
    }

    private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data p_312037_) {
        String[] astring = shrink(p_312037_.pattern);
        int i = astring[0].length();
        int j = astring.length;
        NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i * j, Ingredient.EMPTY);
        CharSet charset = new CharArraySet(p_312037_.key.keySet());

        for(int k = 0; k < astring.length; ++k) {
            String s = astring[k];

            for(int l = 0; l < s.length(); ++l) {
                char c0 = s.charAt(l);
                Ingredient ingredient = c0 == ' ' ? Ingredient.EMPTY : p_312037_.key.get(c0);
                if (ingredient == null) {
                    return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
                }

                charset.remove(c0);
                nonnulllist.set(l + i * k, ingredient);
            }
        }

        return !charset.isEmpty()
            ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
            : DataResult.success(new ShapedRecipePattern(i, j, nonnulllist, Optional.of(p_312037_)));
    }

    @VisibleForTesting
    static String[] shrink(List<String> pPattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for(int i1 = 0; i1 < pPattern.size(); ++i1) {
            String s = pPattern.get(i1);
            i = Math.min(i, firstNonSpace(s));
            int j1 = lastNonSpace(s);
            j = Math.max(j, j1);
            if (j1 < 0) {
                if (k == i1) {
                    ++k;
                }

                ++l;
            } else {
                l = 0;
            }
        }

        if (pPattern.size() == l) {
            return new String[0];
        } else {
            String[] astring = new String[pPattern.size() - l - k];

            for(int k1 = 0; k1 < astring.length; ++k1) {
                astring[k1] = pPattern.get(k1 + k).substring(i, j + 1);
            }

            return astring;
        }
    }

    private static int firstNonSpace(String pRow) {
        int i = 0;

        while(i < pRow.length() && pRow.charAt(i) == ' ') {
            ++i;
        }

        return i;
    }

    private static int lastNonSpace(String pRow) {
        int i = pRow.length() - 1;

        while(i >= 0 && pRow.charAt(i) == ' ') {
            --i;
        }

        return i;
    }

    public boolean matches(CraftingContainer pContainer) {
        for(int i = 0; i <= pContainer.getWidth() - this.width; ++i) {
            for(int j = 0; j <= pContainer.getHeight() - this.height; ++j) {
                if (this.matches(pContainer, i, j, true)) {
                    return true;
                }

                if (this.matches(pContainer, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches(CraftingContainer pContainer, int pStartX, int pStartY, boolean pReverse) {
        for(int i = 0; i < pContainer.getWidth(); ++i) {
            for(int j = 0; j < pContainer.getHeight(); ++j) {
                int k = i - pStartX;
                int l = j - pStartY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    if (pReverse) {
                        ingredient = this.ingredients.get(this.width - k - 1 + l * this.width);
                    } else {
                        ingredient = this.ingredients.get(k + l * this.width);
                    }
                }

                if (!ingredient.test(pContainer.getItem(i + j * pContainer.getWidth()))) {
                    return false;
                }
            }
        }

        return true;
    }

    public void toNetwork(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.width);
        pBuffer.writeVarInt(this.height);

        for(Ingredient ingredient : this.ingredients) {
            ingredient.toNetwork(pBuffer);
        }
    }

    public static ShapedRecipePattern fromNetwork(FriendlyByteBuf pNuffer) {
        int i = pNuffer.readVarInt();
        int j = pNuffer.readVarInt();
        NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i * j, Ingredient.EMPTY);
        nonnulllist.replaceAll(p_312742_ -> Ingredient.fromNetwork(pNuffer));
        return new ShapedRecipePattern(i, j, nonnulllist, Optional.empty());
    }

    public static record Data(Map<Character, Ingredient> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(p_312085_ -> {
            if (p_312085_.size() > maxHeight) {
                return DataResult.error(() -> "Invalid pattern: too many rows, %s is maximum".formatted(maxHeight));
            } else if (p_312085_.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            } else {
                int i = p_312085_.get(0).length();

                for(String s : p_312085_) {
                    if (s.length() > maxWidth) {
                        return DataResult.error(() -> "Invalid pattern: too many columns, %s is maximum".formatted(maxWidth));
                    }

                    if (i != s.length()) {
                        return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                    }
                }

                return DataResult.success(p_312085_);
            }
        }, Function.identity());
        private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(p_312250_ -> {
            if (p_312250_.length() != 1) {
                return DataResult.error(() -> "Invalid key entry: '" + p_312250_ + "' is an invalid symbol (must be 1 character only).");
            } else {
                return " ".equals(p_312250_) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(p_312250_.charAt(0));
            }
        }, String::valueOf);
        public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_312573_ -> p_312573_.group(
                        ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(p_312509_ -> p_312509_.key),
                        PATTERN_CODEC.fieldOf("pattern").forGetter(p_312713_ -> p_312713_.pattern)
                    )
                    .apply(p_312573_, ShapedRecipePattern.Data::new)
        );
    }
}
