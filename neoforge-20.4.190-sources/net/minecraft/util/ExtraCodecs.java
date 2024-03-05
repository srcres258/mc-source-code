package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.MapCodec.MapCodecCodec;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExtraCodecs {
    public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
    public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
    public static final Codec<JsonElement> FLAT_JSON = Codec.STRING.flatXmap(p_304325_ -> {
        try {
            return DataResult.success(JsonParser.parseString(p_304325_));
        } catch (JsonParseException jsonparseexception) {
            return DataResult.error(jsonparseexception::getMessage);
        }
    }, p_304324_ -> {
        try {
            return DataResult.success(GsonHelper.toStableString(p_304324_));
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(illegalargumentexception::getMessage);
        }
    });
    public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_253502_ -> Util.fixedSize(p_253502_, 3).map(p_253489_ -> new Vector3f(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2))),
            p_269787_ -> List.of(p_269787_.x(), p_269787_.y(), p_269787_.z())
        );
    public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_269773_ -> Util.fixedSize(p_269773_, 4).map(p_269785_ -> new Quaternionf(p_269785_.get(0), p_269785_.get(1), p_269785_.get(2), p_269785_.get(3))),
            p_269780_ -> List.of(p_269780_.x, p_269780_.y, p_269780_.z, p_269780_.w)
        );
    public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
        p_269774_ -> p_269774_.group(
                    Codec.FLOAT.fieldOf("angle").forGetter(p_269776_ -> p_269776_.angle),
                    VECTOR3F.fieldOf("axis").forGetter(p_269778_ -> new Vector3f(p_269778_.x, p_269778_.y, p_269778_.z))
                )
                .apply(p_269774_, AxisAngle4f::new)
    );
    public static final Codec<Quaternionf> QUATERNIONF = withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
    public static Codec<Matrix4f> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(p_269788_ -> Util.fixedSize(p_269788_, 16).map(p_269777_ -> {
            Matrix4f matrix4f = new Matrix4f();

            for(int i = 0; i < p_269777_.size(); ++i) {
                matrix4f.setRowColumn(i >> 2, i & 3, p_269777_.get(i));
            }

            return matrix4f.determineProperties();
        }), p_269775_ -> {
        FloatList floatlist = new FloatArrayList(16);

        for(int i = 0; i < 16; ++i) {
            floatlist.add(p_269775_.getRowColumn(i >> 2, i & 3));
        }

        return floatlist;
    });
    public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, p_275703_ -> "Value must be non-negative: " + p_275703_);
    public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, p_274847_ -> "Value must be positive: " + p_274847_);
    public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(
        0.0F, Float.MAX_VALUE, p_274876_ -> "Value must be positive: " + p_274876_
    );
    public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(p_274857_ -> {
        try {
            return DataResult.success(Pattern.compile(p_274857_));
        } catch (PatternSyntaxException patternsyntaxexception) {
            return DataResult.error(() -> "Invalid regex pattern '" + p_274857_ + "': " + patternsyntaxexception.getMessage());
        }
    }, Pattern::pattern);
    public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
    public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(p_274852_ -> {
        try {
            return DataResult.success(Base64.getDecoder().decode(p_274852_));
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Malformed base64 string");
        }
    }, p_216180_ -> Base64.getEncoder().encodeToString(p_216180_));
    public static final Codec<String> ESCAPED_STRING = Codec.STRING
        .comapFlatMap(p_301741_ -> DataResult.success(StringEscapeUtils.unescapeJava(p_301741_)), StringEscapeUtils::escapeJava);
    public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
        .comapFlatMap(
            p_216169_ -> p_216169_.startsWith("#")
                    ? ResourceLocation.read(p_216169_.substring(1)).map(p_216182_ -> new ExtraCodecs.TagOrElementLocation(p_216182_, true))
                    : ResourceLocation.read(p_216169_).map(p_216165_ -> new ExtraCodecs.TagOrElementLocation(p_216165_, false)),
            ExtraCodecs.TagOrElementLocation::decoratedId
        );
    public static final Function<Optional<Long>, OptionalLong> toOptionalLong = p_216176_ -> p_216176_.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = p_216178_ -> p_216178_.isPresent()
            ? Optional.of(p_216178_.getAsLong())
            : Optional.empty();
    public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
        .xmap(p_253514_ -> BitSet.valueOf(p_253514_.toArray()), p_253493_ -> Arrays.stream(p_253493_.toLongArray()));
    private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
        p_253491_ -> p_253491_.group(
                    Codec.STRING.fieldOf("name").forGetter(Property::name),
                    Codec.STRING.fieldOf("value").forGetter(Property::value),
                    Codec.STRING.optionalFieldOf("signature").forGetter(p_293823_ -> Optional.ofNullable(p_293823_.signature()))
                )
                .apply(p_253491_, (p_253494_, p_253495_, p_253496_) -> new Property(p_253494_, p_253495_, p_253496_.orElse(null)))
    );
    @VisibleForTesting
    public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf())
        .xmap(p_253515_ -> {
            PropertyMap propertymap = new PropertyMap();
            p_253515_.ifLeft(p_253506_ -> p_253506_.forEach((p_253500_, p_253501_) -> {
                    for(String s : p_253501_) {
                        propertymap.put(p_253500_, new Property(p_253500_, s));
                    }
                })).ifRight(p_293821_ -> {
                for(Property property : p_293821_) {
                    propertymap.put(property.name(), property);
                }
            });
            return propertymap;
        }, p_253504_ -> Either.right(p_253504_.values().stream().toList()));
    private static final MapCodec<GameProfile> GAME_PROFILE_WITHOUT_PROPERTIES = RecordCodecBuilder.mapCodec(
        p_293824_ -> p_293824_.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(GameProfile::getId), Codec.STRING.fieldOf("name").forGetter(GameProfile::getName)
                )
                .apply(p_293824_, GameProfile::new)
    );
    public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create(
        p_293825_ -> p_293825_.group(
                    GAME_PROFILE_WITHOUT_PROPERTIES.forGetter(Function.identity()),
                    PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)
                )
                .apply(p_293825_, (p_253518_, p_253519_) -> {
                    p_253519_.forEach((p_253511_, p_253512_) -> p_253518_.getProperties().put(p_253511_, p_253512_));
                    return p_253518_;
                })
    );
    public static final Codec<String> NON_EMPTY_STRING = validate(
        Codec.STRING, p_274858_ -> p_274858_.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(p_274858_)
    );
    public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(p_284688_ -> {
        int[] aint = p_284688_.codePoints().toArray();
        return aint.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + p_284688_) : DataResult.success(aint[0]);
    }, Character::toString);
    public static Codec<String> RESOURCE_PATH_CODEC = validate(
        Codec.STRING,
        p_293822_ -> !ResourceLocation.isValidPath(p_293822_)
                ? DataResult.error(() -> "Invalid string to use as a resource path element: " + p_293822_)
                : DataResult.success(p_293822_)
    );

    public static <T> Codec<T> converter(DynamicOps<T> pOps) {
        return Codec.PASSTHROUGH.xmap(p_304323_ -> p_304323_.convert(pOps).getValue(), p_304327_ -> new Dynamic<>(pOps, p_304327_));
    }

    public static <F, S> Codec<Either<F, S>> xor(Codec<F> pFirst, Codec<S> pSecond) {
        return new ExtraCodecs.XorCodec<>(pFirst, pSecond);
    }

    public static <P, I> Codec<I> intervalCodec(
        Codec<P> pCodec, String pMinFieldName, String pMaxFieldName, BiFunction<P, P, DataResult<I>> pFactory, Function<I, P> pMinGetter, Function<I, P> pMaxGetter
    ) {
        Codec<I> codec = Codec.list(pCodec).comapFlatMap(p_184398_ -> Util.fixedSize(p_184398_, 2).flatMap(p_184445_ -> {
                P p = p_184445_.get(0);
                P p1 = p_184445_.get(1);
                return pFactory.apply(p, p1);
            }), p_184459_ -> ImmutableList.of(pMinGetter.apply(p_184459_), pMaxGetter.apply(p_184459_)));
        Codec<I> codec1 = RecordCodecBuilder.<Pair<P, P>>create(
                p_184360_ -> p_184360_.group(pCodec.fieldOf(pMinFieldName).forGetter(Pair::getFirst), pCodec.fieldOf(pMaxFieldName).forGetter(Pair::getSecond))
                        .apply(p_184360_, Pair::of)
            )
            .comapFlatMap(
                p_184392_ -> pFactory.apply((P)p_184392_.getFirst(), (P)p_184392_.getSecond()),
                p_184449_ -> Pair.of(pMinGetter.apply(p_184449_), pMaxGetter.apply(p_184449_))
            );
        Codec<I> codec2 = withAlternative(codec, codec1);
        return Codec.either(pCodec, codec2)
            .comapFlatMap(p_184389_ -> p_184389_.map(p_184395_ -> pFactory.apply(p_184395_, p_184395_), DataResult::success), p_184411_ -> {
                P p = pMinGetter.apply(p_184411_);
                P p1 = pMaxGetter.apply(p_184411_);
                return Objects.equals(p, p1) ? Either.left(p) : Either.right(p_184411_);
            });
    }

    public static <A> ResultFunction<A> orElsePartial(final A p_184382_) {
        return new ResultFunction<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> p_184466_, T p_184467_, DataResult<Pair<A, T>> p_184468_) {
                MutableObject<String> mutableobject = new MutableObject<>();
                Optional<Pair<A, T>> optional = p_184468_.resultOrPartial(mutableobject::setValue);
                return optional.isPresent()
                    ? p_184468_
                    : DataResult.error(() -> "(" + (String)mutableobject.getValue() + " -> using default)", Pair.of(p_184382_, p_184467_));
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> p_184470_, A p_184471_, DataResult<T> p_184472_) {
                return p_184472_;
            }

            @Override
            public String toString() {
                return "OrElsePartial[" + p_184382_ + "]";
            }
        };
    }

    public static <E> Codec<E> idResolverCodec(ToIntFunction<E> pEncoder, IntFunction<E> pDecoder, int pNotFoundValue) {
        return Codec.INT
            .flatXmap(
                p_184414_ -> Optional.ofNullable(pDecoder.apply(p_184414_))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + p_184414_)),
                p_274850_ -> {
                    int i = pEncoder.applyAsInt(p_274850_);
                    return i == pNotFoundValue ? DataResult.error(() -> "Element with unknown id: " + p_274850_) : DataResult.success(i);
                }
            );
    }

    public static <E> Codec<E> stringResolverCodec(Function<E, String> pEncoder, Function<String, E> pDecoder) {
        return Codec.STRING
            .flatXmap(
                p_184404_ -> Optional.ofNullable(pDecoder.apply(p_184404_))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown element name:" + p_184404_)),
                p_184401_ -> Optional.ofNullable(pEncoder.apply(p_184401_))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Element with unknown name: " + p_184401_))
            );
    }

    public static <E> Codec<E> orCompressed(final Codec<E> pFirst, final Codec<E> pSecond) {
        return new Codec<E>() {
            @Override
            public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_) {
                return p_184484_.compressMaps() ? pSecond.encode(p_184483_, p_184484_, p_184485_) : pFirst.encode(p_184483_, p_184484_, p_184485_);
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_) {
                return p_184480_.compressMaps() ? pSecond.decode(p_184480_, p_184481_) : pFirst.decode(p_184480_, p_184481_);
            }

            @Override
            public String toString() {
                return pFirst + " orCompressed " + pSecond;
            }
        };
    }

    public static <E> MapCodec<E> orCompressed(final MapCodec<E> pFirst, final MapCodec<E> pSecond) {
        return new MapCodec<E>() {
            @Override
            public <T> RecordBuilder<T> encode(E p_304635_, DynamicOps<T> p_304428_, RecordBuilder<T> p_304777_) {
                return p_304428_.compressMaps() ? pSecond.encode(p_304635_, p_304428_, p_304777_) : pFirst.encode(p_304635_, p_304428_, p_304777_);
            }

            @Override
            public <T> DataResult<E> decode(DynamicOps<T> p_304666_, MapLike<T> p_304870_) {
                return p_304666_.compressMaps() ? pSecond.decode(p_304666_, p_304870_) : pFirst.decode(p_304666_, p_304870_);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_304533_) {
                return pSecond.keys(p_304533_);
            }

            @Override
            public String toString() {
                return pFirst + " orCompressed " + pSecond;
            }
        };
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> pCodec, final Function<E, Lifecycle> p_184370_, final Function<E, Lifecycle> p_184371_) {
        return pCodec.mapResult(new ResultFunction<E>() {
            @Override
            public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> p_304965_, T p_304933_, DataResult<Pair<E, T>> p_304851_) {
                return p_304851_.result().map(p_304330_ -> p_304851_.setLifecycle(p_184370_.apply(p_304330_.getFirst()))).orElse(p_304851_);
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> p_304780_, E p_304414_, DataResult<T> p_304779_) {
                return p_304779_.setLifecycle(p_184371_.apply(p_304414_));
            }

            @Override
            public String toString() {
                return "WithLifecycle[" + p_184370_ + " " + p_184371_ + "]";
            }
        });
    }

    public static <F, S> ExtraCodecs.EitherCodec<F, S> either(Codec<F> pFirst, Codec<S> pSecond) {
        return new ExtraCodecs.EitherCodec<>(pFirst, pSecond);
    }

    public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> pKey, Codec<V> pValue) {
        return new ExtraCodecs.StrictUnboundedMapCodec<>(pKey, pValue);
    }

    public static <T> Codec<T> validate(Codec<T> pCodec, Function<T, DataResult<T>> pValidator) {
        return pCodec instanceof MapCodecCodec mapcodeccodec ? validate(mapcodeccodec.codec(), pValidator).codec() : pCodec.flatXmap(pValidator, pValidator);
    }

    public static <T> MapCodec<T> validate(MapCodec<T> pCodec, Function<T, DataResult<T>> pValidator) {
        return pCodec.flatXmap(pValidator, pValidator);
    }

    private static Codec<Integer> intRangeWithMessage(int pMin, int pMax, Function<Integer, String> pErrorMessage) {
        return validate(
            Codec.INT,
            p_274889_ -> p_274889_.compareTo(pMin) >= 0 && p_274889_.compareTo(pMax) <= 0
                    ? DataResult.success(p_274889_)
                    : DataResult.error(() -> pErrorMessage.apply(p_274889_))
        );
    }

    public static Codec<Integer> intRange(int pMin, int pMax) {
        return intRangeWithMessage(pMin, pMax, p_269784_ -> "Value must be within range [" + pMin + ";" + pMax + "]: " + p_269784_);
    }

    private static Codec<Float> floatRangeMinExclusiveWithMessage(float pMin, float pMax, Function<Float, String> pErrorMessage) {
        return validate(
            Codec.FLOAT,
            p_274865_ -> p_274865_.compareTo(pMin) > 0 && p_274865_.compareTo(pMax) <= 0
                    ? DataResult.success(p_274865_)
                    : DataResult.error(() -> pErrorMessage.apply(p_274865_))
        );
    }

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> pCodec) {
        return validate(pCodec, p_274853_ -> p_274853_.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(p_274853_));
    }

    public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> pCodec) {
        return validate(
            pCodec,
            p_274860_ -> p_274860_.unwrap().right().filter(List::isEmpty).isPresent()
                    ? DataResult.error(() -> "List must have contents")
                    : DataResult.success(p_274860_)
        );
    }

    public static <T> Codec<T> recursive(String pName, Function<Codec<T>, Codec<T>> pWrapper) {
        return new ExtraCodecs.RecursiveCodec<>(pName, pWrapper);
    }

    public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> pDelegate) {
        return new ExtraCodecs.RecursiveCodec<>(pDelegate.toString(), p_297963_ -> pDelegate.get());
    }

    public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> pElementCodec, String pName) {
        return new ExtraCodecs.StrictOptionalFieldCodec<>(pName, pElementCodec);
    }

    public static <A> MapCodec<A> strictOptionalField(Codec<A> pElementCodec, String pName, A pFallback) {
        return strictOptionalField(pElementCodec, pName)
            .xmap(p_297957_ -> p_297957_.orElse(pFallback), p_297959_ -> Objects.equals(p_297959_, pFallback) ? Optional.empty() : Optional.of(p_297959_));
    }

    public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> p_203977_) {
        class ContextRetrievalCodec extends MapCodec<E> {
            @Override
            public <T> RecordBuilder<T> encode(E p_203993_, DynamicOps<T> p_203994_, RecordBuilder<T> p_203995_) {
                return p_203995_;
            }

            @Override
            public <T> DataResult<E> decode(DynamicOps<T> p_203990_, MapLike<T> p_203991_) {
                return p_203977_.apply(p_203990_);
            }

            @Override
            public String toString() {
                return "ContextRetrievalCodec[" + p_203977_ + "]";
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_203997_) {
                return Stream.empty();
            }
        }

        return new ContextRetrievalCodec();
    }

    public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> pTypeGetter) {
        return p_203980_ -> {
            Iterator<E> iterator = p_203980_.iterator();
            if (iterator.hasNext()) {
                T t = pTypeGetter.apply(iterator.next());

                while(iterator.hasNext()) {
                    E e = iterator.next();
                    T t1 = pTypeGetter.apply(e);
                    if (t1 != t) {
                        return DataResult.error(() -> "Mixed type list: element " + e + " had type " + t1 + ", but list is of type " + t);
                    }
                }
            }

            return DataResult.success(p_203980_, Lifecycle.stable());
        };
    }

    public static <A> Codec<A> catchDecoderException(final Codec<A> pCodec) {
        return Codec.of(pCodec, new Decoder<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_304559_, T p_304739_) {
                try {
                    return pCodec.decode(p_304559_, p_304739_);
                } catch (Exception exception) {
                    return DataResult.error(() -> "Caught exception decoding " + p_304739_ + ": " + exception.getMessage());
                }
            }
        });
    }

    public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter pDateTimeFormatter) {
        return Codec.STRING.comapFlatMap(p_300789_ -> {
            try {
                return DataResult.success(pDateTimeFormatter.parse(p_300789_));
            } catch (Exception exception) {
                return DataResult.error(exception::getMessage);
            }
        }, pDateTimeFormatter::format);
    }

    public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> pCodec) {
        return pCodec.xmap(toOptionalLong, fromOptionalLong);
    }

    public static Codec<String> sizeLimitedString(int pMinSize, int pMaxSize) {
        return validate(
            Codec.STRING,
            p_274879_ -> {
                int i = p_274879_.length();
                if (i < pMinSize) {
                    return DataResult.error(() -> "String \"" + p_274879_ + "\" is too short: " + i + ", expected range [" + pMinSize + "-" + pMaxSize + "]");
                } else {
                    return i > pMaxSize
                        ? DataResult.error(() -> "String \"" + p_274879_ + "\" is too long: " + i + ", expected range [" + pMinSize + "-" + pMaxSize + "]")
                        : DataResult.success(p_274879_);
                }
            }
        );
    }

    public static <T> Codec<T> withAlternative(Codec<T> pCodec, Codec<? extends T> pAlternative) {
        return Codec.either(pCodec, pAlternative).xmap(p_184355_ -> p_184355_.map(p_184461_ -> p_184461_, p_184455_ -> p_184455_), Either::left);
    }

    public static <T, U> Codec<T> withAlternative(Codec<T> pCodec, Codec<U> pAlternative, Function<U, T> pConverter) {
        return Codec.either(pCodec, pAlternative).xmap(p_293819_ -> p_293819_.map(p_296016_ -> p_296016_, pConverter), Either::left);
    }

    public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> pCodec) {
        return Codec.unboundedMap(pCodec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
    }

    @Deprecated
    public static <K, V> MapCodec<V> dispatchOptionalValue(
        final String p_312812_,
        final String p_312135_,
        final Codec<K> p_312848_,
        final Function<? super V, ? extends K> p_312622_,
        final Function<? super K, ? extends Codec<? extends V>> p_312836_
    ) {
        return new MapCodec<V>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_312501_) {
                return Stream.of(p_312501_.createString(p_312812_), p_312501_.createString(p_312135_));
            }

            @Override
            public <T> DataResult<V> decode(DynamicOps<T> p_312013_, MapLike<T> p_312527_) {
                T t = p_312527_.get(p_312812_);
                return t == null
                    ? DataResult.error(() -> "Missing \"" + p_312812_ + "\" in: " + p_312527_)
                    : p_312848_.decode(p_312013_, t).flatMap(p_311855_ -> {
                        T t1 = Objects.requireNonNullElseGet(p_312527_.get(p_312135_), p_312013_::emptyMap);
                        return p_312836_.apply(p_311855_.getFirst()).decode(p_312013_, t1).map(Pair::getFirst);
                    });
            }

            @Override
            public <T> RecordBuilder<T> encode(V p_312741_, DynamicOps<T> p_312649_, RecordBuilder<T> p_312297_) {
                K k = p_312622_.apply(p_312741_);
                p_312297_.add(p_312812_, p_312848_.encodeStart(p_312649_, k));
                DataResult<T> dataresult = this.encode(p_312836_.apply(k), p_312741_, p_312649_);
                if (dataresult.result().isEmpty() || !Objects.equals(dataresult.result().get(), p_312649_.emptyMap())) {
                    p_312297_.add(p_312135_, dataresult);
                }

                return p_312297_;
            }

            private <T, V2 extends V> DataResult<T> encode(Codec<? extends V> p_312014_, V p_312190_, DynamicOps<T> p_312075_) {
                return ((Codec<V2>)p_312014_).encodeStart(p_312075_, (V2)p_312190_);
            }
        };
    }

    public static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
        private final Codec<F> first;
        private final Codec<S> second;

        public EitherCodec(Codec<F> pFirst, Codec<S> pSecond) {
            this.first = pFirst;
            this.second = pSecond;
        }

        @Override
        public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> pOps, T pInput) {
            DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(pOps, pInput).map(p_184524_ -> p_184524_.mapFirst(Either::left));
            if (dataresult.error().isEmpty()) {
                return dataresult;
            } else {
                DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(pOps, pInput).map(p_184515_ -> p_184515_.mapFirst(Either::right));
                return dataresult1.error().isEmpty() ? dataresult1 : dataresult.apply2((p_184517_, p_184518_) -> p_184518_, dataresult1);
            }
        }

        public <T> DataResult<T> encode(Either<F, S> pInput, DynamicOps<T> pOps, T pPrefix) {
            return pInput.map(
                p_184528_ -> this.first.encode(p_184528_, pOps, pPrefix), p_184522_ -> this.second.encode(p_184522_, pOps, pPrefix)
            );
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                ExtraCodecs.EitherCodec<?, ?> eithercodec = (ExtraCodecs.EitherCodec)pOther;
                return Objects.equals(this.first, eithercodec.first) && Objects.equals(this.second, eithercodec.second);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.first, this.second);
        }

        @Override
        public String toString() {
            return "EitherCodec[" + this.first + ", " + this.second + "]";
        }
    }

    static class RecursiveCodec<T> implements Codec<T> {
        private final String name;
        private final Supplier<Codec<T>> wrapped;

        RecursiveCodec(String pName, Function<Codec<T>, Codec<T>> pWrapper) {
            this.name = pName;
            this.wrapped = Suppliers.memoize(() -> pWrapper.apply(this));
        }

        @Override
        public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> pOps, S pInput) {
            return this.wrapped.get().decode(pOps, pInput);
        }

        @Override
        public <S> DataResult<S> encode(T pInput, DynamicOps<S> pOps, S pValue) {
            return this.wrapped.get().encode(pInput, pOps, pValue);
        }

        @Override
        public String toString() {
            return "RecursiveCodec[" + this.name + "]";
        }
    }

    static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final String name;
        private final Codec<A> elementCodec;

        public StrictOptionalFieldCodec(String pName, Codec<A> pElementCodec) {
            this.name = pName;
            this.elementCodec = pElementCodec;
        }

        @Override
        public <T> DataResult<Optional<A>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
            T t = pInput.get(this.name);
            return t == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(pOps, t).map(Optional::of);
        }

        public <T> RecordBuilder<T> encode(Optional<A> pInput, DynamicOps<T> pOps, RecordBuilder<T> pPrefix) {
            return pInput.isPresent() ? pPrefix.add(this.name, this.elementCodec.encodeStart(pOps, pInput.get())) : pPrefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> pOps) {
            return Stream.of(pOps.createString(this.name));
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (!(pOther instanceof ExtraCodecs.StrictOptionalFieldCodec)) {
                return false;
            } else {
                ExtraCodecs.StrictOptionalFieldCodec<?> strictoptionalfieldcodec = (ExtraCodecs.StrictOptionalFieldCodec)pOther;
                return Objects.equals(this.name, strictoptionalfieldcodec.name) && Objects.equals(this.elementCodec, strictoptionalfieldcodec.elementCodec);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.elementCodec);
        }

        @Override
        public String toString() {
            return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
        }
    }

    public static record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
        @Override
        public <T> DataResult<Map<K, V>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
            Builder<K, V> builder = ImmutableMap.builder();

            for(Pair<T, T> pair : pInput.entries().toList()) {
                DataResult<K> dataresult = this.keyCodec().parse(pOps, pair.getFirst());
                DataResult<V> dataresult1 = this.elementCodec().parse(pOps, pair.getSecond());
                DataResult<Pair<K, V>> dataresult2 = dataresult.apply2stable(Pair::of, dataresult1);
                if (dataresult2.error().isPresent()) {
                    return DataResult.error(() -> {
                        PartialResult<Pair<K, V>> partialresult = dataresult2.error().get();
                        String s;
                        if (dataresult.result().isPresent()) {
                            s = "Map entry '" + dataresult.result().get() + "' : " + partialresult.message();
                        } else {
                            s = partialresult.message();
                        }

                        return s;
                    });
                }

                if (!dataresult2.result().isPresent()) {
                    return DataResult.error(() -> "Empty or invalid map contents are not allowed");
                }

                Pair<K, V> pair1 = dataresult2.result().get();
                builder.put(pair1.getFirst(), pair1.getSecond());
            }

            Map<K, V> map = builder.build();
            return DataResult.success(map);
        }

        @Override
        public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> pOps, T pInput) {
            return pOps.getMap(pInput)
                .setLifecycle(Lifecycle.stable())
                .flatMap(p_301208_ -> this.decode(pOps, p_301208_))
                .map(p_300941_ -> Pair.of(p_300941_, pInput));
        }

        public <T> DataResult<T> encode(Map<K, V> pInput, DynamicOps<T> pOps, T pValue) {
            return this.encode(pInput, pOps, pOps.mapBuilder()).build(pValue);
        }

        @Override
        public String toString() {
            return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
        }
    }

    public static record TagOrElementLocation(ResourceLocation id, boolean tag) {
        @Override
        public String toString() {
            return this.decoratedId();
        }

        private String decoratedId() {
            return this.tag ? "#" + this.id : this.id.toString();
        }
    }

    static record XorCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
        @Override
        public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> pOps, T pInput) {
            DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(pOps, pInput).map(p_144673_ -> p_144673_.mapFirst(Either::left));
            DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(pOps, pInput).map(p_144667_ -> p_144667_.mapFirst(Either::right));
            Optional<Pair<Either<F, S>, T>> optional = dataresult.result();
            Optional<Pair<Either<F, S>, T>> optional1 = dataresult1.result();
            if (optional.isPresent() && optional1.isPresent()) {
                return DataResult.error(
                    () -> "Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional1.get(),
                    optional.get()
                );
            } else if (optional.isPresent()) {
                return dataresult;
            } else {
                return optional1.isPresent() ? dataresult1 : dataresult.apply2((p_300790_, p_300791_) -> p_300791_, dataresult1);
            }
        }

        public <T> DataResult<T> encode(Either<F, S> pInput, DynamicOps<T> pOps, T pPrefix) {
            return pInput.map(
                p_144677_ -> this.first.encode(p_144677_, pOps, pPrefix), p_144671_ -> this.second.encode(p_144671_, pOps, pPrefix)
            );
        }

        @Override
        public String toString() {
            return "XorCodec[" + this.first + ", " + this.second + "]";
        }
    }
}
