package net.minecraft.client.renderer.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderChunkRegion implements BlockAndTintGetter {
    private final int centerX;
    private final int centerZ;
    protected final RenderChunk[][] chunks;
    protected final Level level;
    @Nullable
    private final net.neoforged.neoforge.client.model.data.ModelDataManager.Snapshot modelDataManager;

    @Deprecated
    RenderChunkRegion(Level pLevel, int pCenterX, int pCenterZ, RenderChunk[][] pChunks) {
        this(pLevel, pCenterX, pCenterZ, pChunks, null);
    }
    RenderChunkRegion(Level p_200456_, int p_200457_, int p_200458_, RenderChunk[][] p_200459_, @Nullable net.neoforged.neoforge.client.model.data.ModelDataManager.Snapshot modelDataManager) {
        this.level = p_200456_;
        this.centerX = p_200457_;
        this.centerZ = p_200458_;
        this.chunks = p_200459_;
        this.modelDataManager = modelDataManager;
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX()) - this.centerX;
        int j = SectionPos.blockToSectionCoord(pPos.getZ()) - this.centerZ;
        return this.chunks[i][j].getBlockState(pPos);
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX()) - this.centerX;
        int j = SectionPos.blockToSectionCoord(pPos.getZ()) - this.centerZ;
        return this.chunks[i][j].getBlockState(pPos).getFluidState();
    }

    @Override
    public float getShade(Direction pDirection, boolean pShade) {
        return this.level.getShade(pDirection, pShade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX()) - this.centerX;
        int j = SectionPos.blockToSectionCoord(pPos.getZ()) - this.centerZ;
        return this.chunks[i][j].getBlockEntity(pPos);
    }

    @Override
    public int getBlockTint(BlockPos pPos, ColorResolver pColorResolver) {
        return this.level.getBlockTint(pPos, pColorResolver);
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level.getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    @Nullable
    public net.neoforged.neoforge.client.model.data.ModelDataManager.Snapshot getModelDataManager() {
        return modelDataManager;
    }

    @Override
    public net.neoforged.neoforge.common.world.AuxiliaryLightManager getAuxLightManager(net.minecraft.world.level.ChunkPos pos) {
        int relX = pos.x - this.centerX;
        int relZ = pos.z - this.centerZ;
        return this.chunks[relX][relZ].wrapped.getAuxLightManager(pos);
    }
}
