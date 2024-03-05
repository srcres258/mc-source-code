package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class TrialSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<TrialSpawnerBlock> CODEC = simpleCodec(TrialSpawnerBlock::new);
    public static final EnumProperty<TrialSpawnerState> STATE = BlockStateProperties.TRIAL_SPAWNER_STATE;

    @Override
    public MapCodec<TrialSpawnerBlock> codec() {
        return CODEC;
    }

    public TrialSpawnerBlock(BlockBehaviour.Properties p_312795_) {
        super(p_312795_);
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, TrialSpawnerState.INACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_312785_) {
        p_312785_.add(STATE);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_312710_) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_311941_, BlockState p_312821_) {
        return new TrialSpawnerBlockEntity(p_311941_, p_312821_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_311756_, BlockState p_312797_, BlockEntityType<T> p_312122_) {
        return p_311756_ instanceof ServerLevel serverlevel
            ? createTickerHelper(
                p_312122_,
                BlockEntityType.TRIAL_SPAWNER,
                (p_312628_, p_312290_, p_312639_, p_311782_) -> p_311782_.getTrialSpawner().tickServer(serverlevel, p_312290_)
            )
            : createTickerHelper(
                p_312122_,
                BlockEntityType.TRIAL_SPAWNER,
                (p_312408_, p_312921_, p_312435_, p_312269_) -> p_312269_.getTrialSpawner().tickClient(p_312408_, p_312921_)
            );
    }

    @Override
    public void appendHoverText(ItemStack p_312446_, @Nullable BlockGetter p_312129_, List<Component> p_312088_, TooltipFlag p_311895_) {
        super.appendHoverText(p_312446_, p_312129_, p_312088_, p_311895_);
        Spawner.appendHoverText(p_312446_, p_312088_, "spawn_data");
    }
}
