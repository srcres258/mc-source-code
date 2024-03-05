package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements Clearable, ContainerSingleItem {
    private static final int SONG_END_PADDING = 20;
    private ItemStack item = ItemStack.EMPTY;
    private int ticksSinceLastEvent;
    private long tickCount;
    private long recordStartedTick;
    private boolean isPlaying;

    public JukeboxBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.JUKEBOX, pPos, pBlockState);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("RecordItem", 10)) {
            this.item = ItemStack.of(pTag.getCompound("RecordItem"));
        }

        this.isPlaying = pTag.getBoolean("IsPlaying");
        this.recordStartedTick = pTag.getLong("RecordStartTick");
        this.tickCount = pTag.getLong("TickCount");
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (!this.getTheItem().isEmpty()) {
            pTag.put("RecordItem", this.getTheItem().save(new CompoundTag()));
        }

        pTag.putBoolean("IsPlaying", this.isPlaying);
        pTag.putLong("RecordStartTick", this.recordStartedTick);
        pTag.putLong("TickCount", this.tickCount);
    }

    public boolean isRecordPlaying() {
        return !this.getTheItem().isEmpty() && this.isPlaying;
    }

    private void setHasRecordBlockState(@Nullable Entity pEntity, boolean pHasRecord) {
        if (this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, Boolean.valueOf(pHasRecord)), 2);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(pEntity, this.getBlockState()));
        }
    }

    @VisibleForTesting
    public void startPlaying() {
        this.recordStartedTick = this.tickCount;
        this.isPlaying = true;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.level.levelEvent(null, 1010, this.getBlockPos(), Item.getId(this.getTheItem().getItem()));
        this.setChanged();
    }

    private void stopPlaying() {
        this.isPlaying = false;
        this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.level.levelEvent(1011, this.getBlockPos(), 0);
        this.setChanged();
    }

    private void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        ++this.ticksSinceLastEvent;
        if (this.isRecordPlaying()) {
            Item itemx = this.getTheItem().getItem();
            if (itemx instanceof RecordItem recorditem) {
                if (this.shouldRecordStopPlaying(recorditem)) {
                    this.stopPlaying();
                } else if (this.shouldSendJukeboxPlayingEvent()) {
                    this.ticksSinceLastEvent = 0;
                    pLevel.gameEvent(GameEvent.JUKEBOX_PLAY, pPos, GameEvent.Context.of(pState));
                    this.spawnMusicParticles(pLevel, pPos);
                }
            }
        }

        ++this.tickCount;
    }

    private boolean shouldRecordStopPlaying(RecordItem pRecord) {
        return this.tickCount >= this.recordStartedTick + (long)pRecord.getLengthInTicks() + 20L;
    }

    private boolean shouldSendJukeboxPlayingEvent() {
        return this.ticksSinceLastEvent >= 20;
    }

    @Override
    public ItemStack getTheItem() {
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int pAmount) {
        ItemStack itemstack = this.item;
        this.item = ItemStack.EMPTY;
        if (!itemstack.isEmpty()) {
            this.setHasRecordBlockState(null, false);
            this.stopPlaying();
        }

        return itemstack;
    }

    @Override
    public void setTheItem(ItemStack pItem) {
        if (pItem.is(ItemTags.MUSIC_DISCS) && this.level != null) {
            this.item = pItem;
            this.setHasRecordBlockState(null, true);
            this.startPlaying();
        } else if (pItem.isEmpty()) {
            this.splitTheItem(1);
        }
    }

    /**
     * Returns the maximum stack size for an inventory slot. Seems to always be 64, possibly will be extended.
     */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int pSlot, ItemStack pStack) {
        return pStack.is(ItemTags.MUSIC_DISCS) && this.getItem(pSlot).isEmpty();
    }

    /**
     * {@return {@code true} if the given stack can be extracted into the target inventory}
     *
     * @param pTarget the container into which the item should be extracted
     * @param pSlot   the slot from which to extract the item
     * @param pStack  the item to extract
     */
    @Override
    public boolean canTakeItem(Container pTarget, int pSlot, ItemStack pStack) {
        return pTarget.hasAnyMatching(ItemStack::isEmpty);
    }

    private void spawnMusicParticles(Level pLevel, BlockPos pPos) {
        if (pLevel instanceof ServerLevel serverlevel) {
            Vec3 vec3 = Vec3.atBottomCenterOf(pPos).add(0.0, 1.2F, 0.0);
            float f = (float)pLevel.getRandom().nextInt(4) / 24.0F;
            serverlevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, (double)f, 0.0, 0.0, 1.0);
        }
    }

    public void popOutRecord() {
        if (this.level != null && !this.level.isClientSide) {
            BlockPos blockpos = this.getBlockPos();
            ItemStack itemstack = this.getTheItem();
            if (!itemstack.isEmpty()) {
                this.removeTheItem();
                Vec3 vec3 = Vec3.atLowerCornerWithOffset(blockpos, 0.5, 1.01, 0.5).offsetRandom(this.level.random, 0.7F);
                ItemStack itemstack1 = itemstack.copy();
                ItemEntity itementity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemstack1);
                itementity.setDefaultPickUpDelay();
                this.level.addFreshEntity(itementity);
            }
        }
    }

    public static void playRecordTick(Level pLevel, BlockPos pPos, BlockState pState, JukeboxBlockEntity pJukebox) {
        pJukebox.tick(pLevel, pPos, pState);
    }

    @VisibleForTesting
    public void setRecordWithoutPlaying(ItemStack pStack) {
        this.item = pStack;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.setChanged();
    }
}
