package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;

public final class JigsawStructure extends Structure {
    public static final int MAX_TOTAL_STRUCTURE_RANGE = 128;
    public static final int MAX_DEPTH = 20;
    public static final Codec<JigsawStructure> CODEC = ExtraCodecs.validate(
            RecordCodecBuilder.mapCodec(
                p_227640_ -> p_227640_.group(
                            settingsCodec(p_227640_),
                            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(p_227656_ -> p_227656_.startPool),
                            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(p_227654_ -> p_227654_.startJigsawName),
                            Codec.intRange(0, 20).fieldOf("size").forGetter(p_227652_ -> p_227652_.maxDepth),
                            HeightProvider.CODEC.fieldOf("start_height").forGetter(p_227649_ -> p_227649_.startHeight),
                            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(p_227646_ -> p_227646_.useExpansionHack),
                            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(p_227644_ -> p_227644_.projectStartToHeightmap),
                            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(p_227642_ -> p_227642_.maxDistanceFromCenter),
                            Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(p_307187_ -> p_307187_.poolAliases)
                        )
                        .apply(p_227640_, JigsawStructure::new)
            ),
            JigsawStructure::verifyRange
        )
        .codec();
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;

    private static DataResult<JigsawStructure> verifyRange(JigsawStructure p_286886_) {
        int i = switch(p_286886_.terrainAdaptation()) {
            case NONE -> 0;
            case BURY, BEARD_THIN, BEARD_BOX -> 12;
        };
        return p_286886_.maxDistanceFromCenter + i > 128
            ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128")
            : DataResult.success(p_286886_);
    }

    public JigsawStructure(
        Structure.StructureSettings p_227627_,
        Holder<StructureTemplatePool> p_227628_,
        Optional<ResourceLocation> p_227629_,
        int p_227630_,
        HeightProvider p_227631_,
        boolean p_227632_,
        Optional<Heightmap.Types> p_227633_,
        int p_227634_,
        List<PoolAliasBinding> p_307354_
    ) {
        super(p_227627_);
        this.startPool = p_227628_;
        this.startJigsawName = p_227629_;
        this.maxDepth = p_227630_;
        this.startHeight = p_227631_;
        this.useExpansionHack = p_227632_;
        this.projectStartToHeightmap = p_227633_;
        this.maxDistanceFromCenter = p_227634_;
        this.poolAliases = p_307354_;
    }

    public JigsawStructure(
        Structure.StructureSettings pSettings,
        Holder<StructureTemplatePool> pStartPool,
        int pMaxDepth,
        HeightProvider pStartHeight,
        boolean pUseExpansionHack,
        Heightmap.Types pProjectStartToHeightmap
    ) {
        this(pSettings, pStartPool, Optional.empty(), pMaxDepth, pStartHeight, pUseExpansionHack, Optional.of(pProjectStartToHeightmap), 80, List.of());
    }

    public JigsawStructure(
        Structure.StructureSettings pSettings, Holder<StructureTemplatePool> pStartPool, int pMaxDepth, HeightProvider pStartHeight, boolean pUseExpansionHack
    ) {
        this(pSettings, pStartPool, Optional.empty(), pMaxDepth, pStartHeight, pUseExpansionHack, Optional.empty(), 80, List.of());
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
        ChunkPos chunkpos = pContext.chunkPos();
        int i = this.startHeight.sample(pContext.random(), new WorldGenerationContext(pContext.chunkGenerator(), pContext.heightAccessor()));
        BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), i, chunkpos.getMinBlockZ());
        return JigsawPlacement.addPieces(
            pContext,
            this.startPool,
            this.startJigsawName,
            this.maxDepth,
            blockpos,
            this.useExpansionHack,
            this.projectStartToHeightmap,
            this.maxDistanceFromCenter,
            PoolAliasLookup.create(this.poolAliases, blockpos, pContext.seed())
        );
    }

    @Override
    public StructureType<?> type() {
        return StructureType.JIGSAW;
    }

    public List<PoolAliasBinding> getPoolAliases() {
        return this.poolAliases;
    }
}
