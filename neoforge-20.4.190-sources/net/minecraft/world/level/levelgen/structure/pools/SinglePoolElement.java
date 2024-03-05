package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SinglePoolElement extends StructurePoolElement {
    private static final Codec<Either<ResourceLocation, StructureTemplate>> TEMPLATE_CODEC = Codec.of(
        SinglePoolElement::encodeTemplate, ResourceLocation.CODEC.map(Either::left)
    );
    public static final Codec<SinglePoolElement> CODEC = RecordCodecBuilder.create(
        p_210429_ -> p_210429_.group(templateCodec(), processorsCodec(), projectionCodec()).apply(p_210429_, SinglePoolElement::new)
    );
    protected final Either<ResourceLocation, StructureTemplate> template;
    protected final Holder<StructureProcessorList> processors;

    private static <T> DataResult<T> encodeTemplate(Either<ResourceLocation, StructureTemplate> p_210425_, DynamicOps<T> p_210426_, T p_210427_) {
        Optional<ResourceLocation> optional = p_210425_.left();
        return optional.isEmpty()
            ? DataResult.error(() -> "Can not serialize a runtime pool element")
            : ResourceLocation.CODEC.encode(optional.get(), p_210426_, p_210427_);
    }

    protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
        return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(p_210464_ -> p_210464_.processors);
    }

    protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> templateCodec() {
        return TEMPLATE_CODEC.fieldOf("location").forGetter(p_210431_ -> p_210431_.template);
    }

    protected SinglePoolElement(
        Either<ResourceLocation, StructureTemplate> p_210415_, Holder<StructureProcessorList> p_210416_, StructureTemplatePool.Projection p_210417_
    ) {
        super(p_210417_);
        this.template = p_210415_;
        this.processors = p_210416_;
    }

    @Override
    public Vec3i getSize(StructureTemplateManager pStructureTemplateManager, Rotation pRotation) {
        StructureTemplate structuretemplate = this.getTemplate(pStructureTemplateManager);
        return structuretemplate.getSize(pRotation);
    }

    private StructureTemplate getTemplate(StructureTemplateManager pStructureTemplateManager) {
        return this.template.map(pStructureTemplateManager::getOrCreate, Function.identity());
    }

    public List<StructureTemplate.StructureBlockInfo> getDataMarkers(
        StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, boolean pRelativePosition
    ) {
        StructureTemplate structuretemplate = this.getTemplate(pStructureTemplateManager);
        List<StructureTemplate.StructureBlockInfo> list = structuretemplate.filterBlocks(
            pPos, new StructurePlaceSettings().setRotation(pRotation), Blocks.STRUCTURE_BLOCK, pRelativePosition
        );
        List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();

        for(StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : list) {
            CompoundTag compoundtag = structuretemplate$structureblockinfo.nbt();
            if (compoundtag != null) {
                StructureMode structuremode = StructureMode.valueOf(compoundtag.getString("mode"));
                if (structuremode == StructureMode.DATA) {
                    list1.add(structuretemplate$structureblockinfo);
                }
            }
        }

        return list1;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
        StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, RandomSource pRandom
    ) {
        StructureTemplate structuretemplate = this.getTemplate(pStructureTemplateManager);
        ObjectArrayList<StructureTemplate.StructureBlockInfo> objectarraylist = structuretemplate.filterBlocks(
            pPos, new StructurePlaceSettings().setRotation(pRotation), Blocks.JIGSAW, true
        );
        Util.shuffle(objectarraylist, pRandom);
        sortBySelectionPriority(objectarraylist);
        return objectarraylist;
    }

    @VisibleForTesting
    static void sortBySelectionPriority(List<StructureTemplate.StructureBlockInfo> pStructureBlockInfos) {
        pStructureBlockInfos.sort(
            Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                    p_308863_ -> Optionull.mapOrDefault(p_308863_.nbt(), p_308864_ -> p_308864_.getInt("selection_priority"), 0)
                )
                .reversed()
        );
    }

    @Override
    public BoundingBox getBoundingBox(StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation) {
        StructureTemplate structuretemplate = this.getTemplate(pStructureTemplateManager);
        return structuretemplate.getBoundingBox(new StructurePlaceSettings().setRotation(pRotation), pPos);
    }

    @Override
    public boolean place(
        StructureTemplateManager pStructureTemplateManager,
        WorldGenLevel pLevel,
        StructureManager pStructureManager,
        ChunkGenerator pGenerator,
        BlockPos pOffset,
        BlockPos pPos,
        Rotation pRotation,
        BoundingBox pBox,
        RandomSource pRandom,
        boolean pKeepJigsaws
    ) {
        StructureTemplate structuretemplate = this.getTemplate(pStructureTemplateManager);
        StructurePlaceSettings structureplacesettings = this.getSettings(pRotation, pBox, pKeepJigsaws);
        if (!structuretemplate.placeInWorld(pLevel, pOffset, pPos, structureplacesettings, pRandom, 18)) {
            return false;
        } else {
            for(StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : StructureTemplate.processBlockInfos(
                pLevel, pOffset, pPos, structureplacesettings, this.getDataMarkers(pStructureTemplateManager, pOffset, pRotation, false)
            )) {
                this.handleDataMarker(pLevel, structuretemplate$structureblockinfo, pOffset, pRotation, pRandom, pBox);
            }

            return true;
        }
    }

    protected StructurePlaceSettings getSettings(Rotation pRotation, BoundingBox pBoundingBox, boolean pOffset) {
        StructurePlaceSettings structureplacesettings = new StructurePlaceSettings();
        structureplacesettings.setBoundingBox(pBoundingBox);
        structureplacesettings.setRotation(pRotation);
        structureplacesettings.setKnownShape(true);
        structureplacesettings.setIgnoreEntities(false);
        structureplacesettings.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        structureplacesettings.setFinalizeEntities(true);
        if (!pOffset) {
            structureplacesettings.addProcessor(JigsawReplacementProcessor.INSTANCE);
        }

        this.processors.value().list().forEach(structureplacesettings::addProcessor);
        this.getProjection().getProcessors().forEach(structureplacesettings::addProcessor);
        return structureplacesettings;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return StructurePoolElementType.SINGLE;
    }

    @Override
    public String toString() {
        return "Single[" + this.template + "]";
    }
}
