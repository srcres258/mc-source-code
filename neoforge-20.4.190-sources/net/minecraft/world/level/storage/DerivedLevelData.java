package net.minecraft.world.level.storage;

import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.timers.TimerQueue;

public class DerivedLevelData implements ServerLevelData {
    private final WorldData worldData;
    private final ServerLevelData wrapped;

    public DerivedLevelData(WorldData pWorldData, ServerLevelData pWrapped) {
        this.worldData = pWorldData;
        this.wrapped = pWrapped;
    }

    /**
     * Returns the x spawn position
     */
    @Override
    public int getXSpawn() {
        return this.wrapped.getXSpawn();
    }

    /**
     * Return the Y axis spawning point of the player.
     */
    @Override
    public int getYSpawn() {
        return this.wrapped.getYSpawn();
    }

    /**
     * Returns the z spawn position
     */
    @Override
    public int getZSpawn() {
        return this.wrapped.getZSpawn();
    }

    @Override
    public float getSpawnAngle() {
        return this.wrapped.getSpawnAngle();
    }

    @Override
    public long getGameTime() {
        return this.wrapped.getGameTime();
    }

    /**
     * Get current world time
     */
    @Override
    public long getDayTime() {
        return this.wrapped.getDayTime();
    }

    /**
     * Get current world name
     */
    @Override
    public String getLevelName() {
        return this.worldData.getLevelName();
    }

    @Override
    public int getClearWeatherTime() {
        return this.wrapped.getClearWeatherTime();
    }

    @Override
    public void setClearWeatherTime(int pTime) {
    }

    /**
     * Returns {@code true} if it is thundering, {@code false} otherwise.
     */
    @Override
    public boolean isThundering() {
        return this.wrapped.isThundering();
    }

    /**
     * Returns the number of ticks until next thunderbolt.
     */
    @Override
    public int getThunderTime() {
        return this.wrapped.getThunderTime();
    }

    /**
     * Returns {@code true} if it is raining, {@code false} otherwise.
     */
    @Override
    public boolean isRaining() {
        return this.wrapped.isRaining();
    }

    /**
     * Return the number of ticks until rain.
     */
    @Override
    public int getRainTime() {
        return this.wrapped.getRainTime();
    }

    /**
     * Gets the GameType.
     */
    @Override
    public GameType getGameType() {
        return this.worldData.getGameType();
    }

    /**
     * Set the x spawn position to the passed in value
     */
    @Override
    public void setXSpawn(int pX) {
    }

    /**
     * Sets the y spawn position
     */
    @Override
    public void setYSpawn(int pY) {
    }

    /**
     * Set the z spawn position to the passed in value
     */
    @Override
    public void setZSpawn(int pZ) {
    }

    @Override
    public void setSpawnAngle(float pAngle) {
    }

    @Override
    public void setGameTime(long pTime) {
    }

    /**
     * Set current world time
     */
    @Override
    public void setDayTime(long pTime) {
    }

    @Override
    public void setSpawn(BlockPos pSpawnPoint, float pAngle) {
    }

    /**
     * Sets whether it is thundering or not.
     */
    @Override
    public void setThundering(boolean pThundering) {
    }

    /**
     * Defines the number of ticks until next thunderbolt.
     */
    @Override
    public void setThunderTime(int pTime) {
    }

    /**
     * Sets whether it is raining or not.
     */
    @Override
    public void setRaining(boolean pIsRaining) {
    }

    /**
     * Sets the number of ticks until rain.
     */
    @Override
    public void setRainTime(int pTime) {
    }

    @Override
    public void setGameType(GameType pType) {
    }

    /**
     * Returns {@code true} if hardcore mode is enabled, otherwise {@code false}.
     */
    @Override
    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    /**
     * Returns {@code true} if commands are allowed on this World.
     */
    @Override
    public boolean getAllowCommands() {
        return this.worldData.getAllowCommands();
    }

    /**
     * Returns {@code true} if the World is initialized.
     */
    @Override
    public boolean isInitialized() {
        return this.wrapped.isInitialized();
    }

    /**
     * Sets the initialization status of the World.
     */
    @Override
    public void setInitialized(boolean pInitialized) {
    }

    /**
     * Gets the GameRules class Instance.
     */
    @Override
    public GameRules getGameRules() {
        return this.worldData.getGameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return this.wrapped.getWorldBorder();
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings pSerializer) {
    }

    @Override
    public Difficulty getDifficulty() {
        return this.worldData.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return this.worldData.isDifficultyLocked();
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.wrapped.getScheduledEvents();
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return 0;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int pDelay) {
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return 0;
    }

    @Override
    public void setWanderingTraderSpawnChance(int pChance) {
    }

    @Override
    public UUID getWanderingTraderId() {
        return null;
    }

    @Override
    public void setWanderingTraderId(UUID pId) {
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory pCrashReportCategory, LevelHeightAccessor pLevel) {
        pCrashReportCategory.setDetail("Derived", true);
        this.wrapped.fillCrashReportCategory(pCrashReportCategory, pLevel);
    }
}
