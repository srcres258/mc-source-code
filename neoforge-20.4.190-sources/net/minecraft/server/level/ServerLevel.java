package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import org.slf4j.Logger;

public class ServerLevel extends Level implements WorldGenLevel {
    public static final BlockPos END_SPAWN_POINT = new BlockPos(100, 50, 0);
    public static final IntProvider RAIN_DELAY = UniformInt.of(12000, 180000);
    public static final IntProvider RAIN_DURATION = UniformInt.of(12000, 24000);
    private static final IntProvider THUNDER_DELAY = UniformInt.of(12000, 180000);
    public static final IntProvider THUNDER_DURATION = UniformInt.of(3600, 15600);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EMPTY_TIME_NO_TICK = 300;
    private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
    final List<ServerPlayer> players = Lists.newArrayList();
    private final ServerChunkCache chunkSource;
    private final MinecraftServer server;
    private final ServerLevelData serverLevelData;
    final EntityTickList entityTickList = new EntityTickList();
    private final PersistentEntitySectionManager<Entity> entityManager;
    private final GameEventDispatcher gameEventDispatcher;
    public boolean noSave;
    private final SleepStatus sleepStatus;
    private int emptyTime;
    private final PortalForcer portalForcer;
    private final LevelTicks<Block> blockTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
    private final LevelTicks<Fluid> fluidTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
    final Set<Mob> navigatingMobs = new ObjectOpenHashSet<>();
    volatile boolean isUpdatingNavigations;
    protected final Raids raids;
    private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
    private final List<BlockEventData> blockEventsToReschedule = new ArrayList<>(64);
    private boolean handlingTick;
    private final List<CustomSpawner> customSpawners;
    @Nullable
    private EndDragonFight dragonFight;
    final Int2ObjectMap<net.neoforged.neoforge.entity.PartEntity<?>> dragonParts = new Int2ObjectOpenHashMap<>();
    private final StructureManager structureManager;
    private final StructureCheck structureCheck;
    private final boolean tickTime;
    private final RandomSequences randomSequences;

    public ServerLevel(
        MinecraftServer pServer,
        Executor pDispatcher,
        LevelStorageSource.LevelStorageAccess pLevelStorageAccess,
        ServerLevelData pServerLevelData,
        ResourceKey<Level> pDimension,
        LevelStem pLevelStem,
        ChunkProgressListener pProgressListener,
        boolean pIsDebug,
        long pBiomeZoomSeed,
        List<CustomSpawner> pCustomSpawners,
        boolean pTickTime,
        @Nullable RandomSequences pRandomSequences
    ) {
        super(
            pServerLevelData,
            pDimension,
            pServer.registryAccess(),
            pLevelStem.type(),
            pServer::getProfiler,
            false,
            pIsDebug,
            pBiomeZoomSeed,
            pServer.getMaxChainedNeighborUpdates()
        );
        this.tickTime = pTickTime;
        this.server = pServer;
        this.customSpawners = pCustomSpawners;
        this.serverLevelData = pServerLevelData;
        ChunkGenerator chunkgenerator = pLevelStem.generator();
        boolean flag = pServer.forceSynchronousWrites();
        DataFixer datafixer = pServer.getFixerUpper();
        EntityPersistentStorage<Entity> entitypersistentstorage = new EntityStorage(
            this, pLevelStorageAccess.getDimensionPath(pDimension).resolve("entities"), datafixer, flag, pServer
        );
        this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new ServerLevel.EntityCallbacks(), entitypersistentstorage);
        this.chunkSource = new ServerChunkCache(
            this,
            pLevelStorageAccess,
            datafixer,
            pServer.getStructureManager(),
            pDispatcher,
            chunkgenerator,
            pServer.getPlayerList().getViewDistance(),
            pServer.getPlayerList().getSimulationDistance(),
            flag,
            pProgressListener,
            this.entityManager::updateChunkStatus,
            () -> pServer.overworld().getDataStorage()
        );
        this.chunkSource.getGeneratorState().ensureStructuresGenerated();
        this.portalForcer = new PortalForcer(this);
        this.updateSkyBrightness();
        this.prepareWeather();
        this.getWorldBorder().setAbsoluteMaxSize(pServer.getAbsoluteMaxWorldSize());
        this.raids = this.getDataStorage().computeIfAbsent(Raids.factory(this), Raids.getFileId(this.dimensionTypeRegistration()));
        if (!pServer.isSingleplayer()) {
            pServerLevelData.setGameType(pServer.getDefaultGameType());
        }

        long i = pServer.getWorldData().worldGenOptions().seed();
        this.structureCheck = new StructureCheck(
            this.chunkSource.chunkScanner(),
            this.registryAccess(),
            pServer.getStructureManager(),
            pDimension,
            chunkgenerator,
            this.chunkSource.randomState(),
            this,
            chunkgenerator.getBiomeSource(),
            i,
            datafixer
        );
        this.structureManager = new StructureManager(this, pServer.getWorldData().worldGenOptions(), this.structureCheck);
        if (this.dimension() == Level.END && this.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
            this.dragonFight = new EndDragonFight(this, i, pServer.getWorldData().endDragonFightData());
        } else {
            this.dragonFight = null;
        }

        this.sleepStatus = new SleepStatus();
        this.gameEventDispatcher = new GameEventDispatcher(this);
        this.randomSequences = Objects.requireNonNullElseGet(
            pRandomSequences, () -> this.getDataStorage().computeIfAbsent(RandomSequences.factory(i), "random_sequences")
        );

        net.neoforged.neoforge.attachment.LevelAttachmentsSavedData.init(this);
    }

    @Deprecated
    @VisibleForTesting
    public void setDragonFight(@Nullable EndDragonFight pDragonFight) {
        this.dragonFight = pDragonFight;
    }

    public void setWeatherParameters(int pClearTime, int pWeatherTime, boolean pIsRaining, boolean pIsThundering) {
        this.serverLevelData.setClearWeatherTime(pClearTime);
        this.serverLevelData.setRainTime(pWeatherTime);
        this.serverLevelData.setThunderTime(pWeatherTime);
        this.serverLevelData.setRaining(pIsRaining);
        this.serverLevelData.setThundering(pIsThundering);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
        return this.getChunkSource()
            .getGenerator()
            .getBiomeSource()
            .getNoiseBiome(pX, pY, pZ, this.getChunkSource().randomState().sampler());
    }

    public StructureManager structureManager() {
        return this.structureManager;
    }

    /**
     * Runs a single tick for the world
     */
    public void tick(BooleanSupplier pHasTimeLeft) {
        ProfilerFiller profilerfiller = this.getProfiler();
        this.handlingTick = true;
        TickRateManager tickratemanager = this.tickRateManager();
        boolean flag = tickratemanager.runsNormally();
        if (flag) {
            profilerfiller.push("world border");
            this.getWorldBorder().tick();
            profilerfiller.popPush("weather");
            this.advanceWeatherCycle();
        }

        int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (this.sleepStatus.areEnoughSleeping(i) && this.sleepStatus.areEnoughDeepSleeping(i, this.players)) {
            if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                long j = this.levelData.getDayTime() + 24000L;
                this.setDayTime(net.neoforged.neoforge.event.EventHooks.onSleepFinished(this, j - j % 24000L, this.getDayTime()));
            }

            this.wakeUpAllPlayers();
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && this.isRaining()) {
                this.resetWeatherCycle();
            }
        }

        this.updateSkyBrightness();
        if (flag) {
            this.tickTime();
        }

        profilerfiller.popPush("tickPending");
        if (!this.isDebug() && flag) {
            long k = this.getGameTime();
            profilerfiller.push("blockTicks");
            this.blockTicks.tick(k, 65536, this::tickBlock);
            profilerfiller.popPush("fluidTicks");
            this.fluidTicks.tick(k, 65536, this::tickFluid);
            profilerfiller.pop();
        }

        profilerfiller.popPush("raid");
        if (flag) {
            this.raids.tick();
        }

        profilerfiller.popPush("chunkSource");
        this.getChunkSource().tick(pHasTimeLeft, true);
        profilerfiller.popPush("blockEvents");
        if (flag) {
            this.runBlockEvents();
        }

        this.handlingTick = false;
        profilerfiller.pop();
        boolean flag1 = !this.players.isEmpty() || net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.hasForcedChunks(this); // Neo: Replace vanilla's has forced chunk check with neo's that checks both the vanilla and neo added ones
        if (flag1) {
            this.resetEmptyTime();
        }

        if (flag1 || this.emptyTime++ < 300) {
            profilerfiller.push("entities");
            if (this.dragonFight != null && flag) {
                profilerfiller.push("dragonFight");
                this.dragonFight.tick();
                profilerfiller.pop();
            }

            this.entityTickList.forEach(p_308566_ -> {
                if (!p_308566_.isRemoved()) {
                    if (this.shouldDiscardEntity(p_308566_)) {
                        p_308566_.discard();
                    } else if (!tickratemanager.isEntityFrozen(p_308566_)) {
                        profilerfiller.push("checkDespawn");
                        p_308566_.checkDespawn();
                        profilerfiller.pop();
                        if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(p_308566_.chunkPosition().toLong())) {
                            Entity entity = p_308566_.getVehicle();
                            if (entity != null) {
                                if (!entity.isRemoved() && entity.hasPassenger(p_308566_)) {
                                    return;
                                }

                                p_308566_.stopRiding();
                            }

                            profilerfiller.push("tick");
                            if (!p_308566_.isRemoved() && !(p_308566_ instanceof net.neoforged.neoforge.entity.PartEntity)) {
                                this.guardEntityTick(this::tickNonPassenger, p_308566_);
                            }
                            profilerfiller.pop();
                        }
                    }
                }
            });
            profilerfiller.pop();
            this.tickBlockEntities();
        }

        profilerfiller.push("entityManagement");
        this.entityManager.tick();
        profilerfiller.pop();
    }

    @Override
    public boolean shouldTickBlocksAt(long pChunkPos) {
        return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(pChunkPos);
    }

    protected void tickTime() {
        if (this.tickTime) {
            long i = this.levelData.getGameTime() + 1L;
            this.serverLevelData.setGameTime(i);
            this.serverLevelData.getScheduledEvents().tick(this.server, i);
            if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                this.setDayTime(this.levelData.getDayTime() + 1L);
            }
        }
    }

    public void setDayTime(long pTime) {
        this.serverLevelData.setDayTime(pTime);
    }

    public void tickCustomSpawners(boolean pSpawnEnemies, boolean pSpawnFriendlies) {
        for(CustomSpawner customspawner : this.customSpawners) {
            customspawner.tick(this, pSpawnEnemies, pSpawnFriendlies);
        }
    }

    private boolean shouldDiscardEntity(Entity pEntity) {
        if (this.server.isSpawningAnimals() || !(pEntity instanceof Animal) && !(pEntity instanceof WaterAnimal)) {
            return !this.server.areNpcsEnabled() && pEntity instanceof Npc;
        } else {
            return true;
        }
    }

    private void wakeUpAllPlayers() {
        this.sleepStatus.removeAllSleepers();
        this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()).forEach(p_184116_ -> p_184116_.stopSleepInBed(false, false));
    }

    public void tickChunk(LevelChunk pChunk, int pRandomTickSpeed) {
        ChunkPos chunkpos = pChunk.getPos();
        boolean flag = this.isRaining();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        ProfilerFiller profilerfiller = this.getProfiler();
        profilerfiller.push("thunder");
        if (flag && this.isThundering() && this.random.nextInt(100000) == 0) {
            BlockPos blockpos = this.findLightningTargetAround(this.getBlockRandomPos(i, 0, j, 15));
            if (this.isRainingAt(blockpos)) {
                DifficultyInstance difficultyinstance = this.getCurrentDifficultyAt(blockpos);
                boolean flag1 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                    && this.random.nextDouble() < (double)difficultyinstance.getEffectiveDifficulty() * 0.01
                    && !this.getBlockState(blockpos.below()).is(Blocks.LIGHTNING_ROD);
                if (flag1) {
                    SkeletonHorse skeletonhorse = EntityType.SKELETON_HORSE.create(this);
                    if (skeletonhorse != null) {
                        skeletonhorse.setTrap(true);
                        skeletonhorse.setAge(0);
                        skeletonhorse.setPos((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
                        this.addFreshEntity(skeletonhorse);
                    }
                }

                LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(this);
                if (lightningbolt != null) {
                    lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos));
                    lightningbolt.setVisualOnly(flag1);
                    this.addFreshEntity(lightningbolt);
                }
            }
        }

        profilerfiller.popPush("iceandsnow");

        for(int i1 = 0; i1 < pRandomTickSpeed; ++i1) {
            if (this.random.nextInt(48) == 0) {
                this.tickPrecipitation(this.getBlockRandomPos(i, 0, j, 15));
            }
        }

        profilerfiller.popPush("tickBlocks");
        if (pRandomTickSpeed > 0) {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for(int j1 = 0; j1 < alevelchunksection.length; ++j1) {
                LevelChunkSection levelchunksection = alevelchunksection[j1];
                if (levelchunksection.isRandomlyTicking()) {
                    int k1 = pChunk.getSectionYFromSectionIndex(j1);
                    int k = SectionPos.sectionToBlockCoord(k1);

                    for(int l = 0; l < pRandomTickSpeed; ++l) {
                        BlockPos blockpos1 = this.getBlockRandomPos(i, k, j, 15);
                        profilerfiller.push("randomTick");
                        BlockState blockstate = levelchunksection.getBlockState(blockpos1.getX() - i, blockpos1.getY() - k, blockpos1.getZ() - j);
                        if (blockstate.isRandomlyTicking()) {
                            blockstate.randomTick(this, blockpos1, this.random);
                        }

                        FluidState fluidstate = blockstate.getFluidState();
                        if (fluidstate.isRandomlyTicking()) {
                            fluidstate.randomTick(this, blockpos1, this.random);
                        }

                        profilerfiller.pop();
                    }
                }
            }
        }

        profilerfiller.pop();
    }

    @VisibleForTesting
    public void tickPrecipitation(BlockPos p_295060_) {
        BlockPos blockpos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p_295060_);
        BlockPos blockpos1 = blockpos.below();
        Biome biome = this.getBiome(blockpos).value();
        if (this.isAreaLoaded(blockpos1, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
        if (biome.shouldFreeze(this, blockpos1)) {
            this.setBlockAndUpdate(blockpos1, Blocks.ICE.defaultBlockState());
        }

        if (this.isRaining()) {
            int i = this.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
            if (i > 0 && biome.shouldSnow(this, blockpos)) {
                BlockState blockstate = this.getBlockState(blockpos);
                if (blockstate.is(Blocks.SNOW)) {
                    int j = blockstate.getValue(SnowLayerBlock.LAYERS);
                    if (j < Math.min(i, 8)) {
                        BlockState blockstate1 = blockstate.setValue(SnowLayerBlock.LAYERS, Integer.valueOf(j + 1));
                        Block.pushEntitiesUp(blockstate, blockstate1, this, blockpos);
                        this.setBlockAndUpdate(blockpos, blockstate1);
                    }
                } else {
                    this.setBlockAndUpdate(blockpos, Blocks.SNOW.defaultBlockState());
                }
            }

            Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos1);
            if (biome$precipitation != Biome.Precipitation.NONE) {
                BlockState blockstate2 = this.getBlockState(blockpos1);
                blockstate2.getBlock().handlePrecipitation(blockstate2, this, blockpos1, biome$precipitation);
            }
        }
    }

    private Optional<BlockPos> findLightningRod(BlockPos pPos) {
        Optional<BlockPos> optional = this.getPoiManager()
            .findClosest(
                p_215059_ -> p_215059_.is(PoiTypes.LIGHTNING_ROD),
                p_184055_ -> p_184055_.getY() == this.getHeight(Heightmap.Types.WORLD_SURFACE, p_184055_.getX(), p_184055_.getZ()) - 1,
                pPos,
                128,
                PoiManager.Occupancy.ANY
            );
        return optional.map(p_184053_ -> p_184053_.above(1));
    }

    protected BlockPos findLightningTargetAround(BlockPos pPos) {
        BlockPos blockpos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos);
        Optional<BlockPos> optional = this.findLightningRod(blockpos);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            AABB aabb = AABB.encapsulatingFullBlocks(blockpos, new BlockPos(blockpos.atY(this.getMaxBuildHeight()))).inflate(3.0);
            List<LivingEntity> list = this.getEntitiesOfClass(
                LivingEntity.class, aabb, p_313585_ -> p_313585_ != null && p_313585_.isAlive() && this.canSeeSky(p_313585_.blockPosition())
            );
            if (!list.isEmpty()) {
                return list.get(this.random.nextInt(list.size())).blockPosition();
            } else {
                if (blockpos.getY() == this.getMinBuildHeight() - 1) {
                    blockpos = blockpos.above(2);
                }

                return blockpos;
            }
        }
    }

    public boolean isHandlingTick() {
        return this.handlingTick;
    }

    public boolean canSleepThroughNights() {
        return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
    }

    private void announceSleepStatus() {
        if (this.canSleepThroughNights()) {
            if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
                int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
                Component component;
                if (this.sleepStatus.areEnoughSleeping(i)) {
                    component = Component.translatable("sleep.skipping_night");
                } else {
                    component = Component.translatable("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(i));
                }

                for(ServerPlayer serverplayer : this.players) {
                    serverplayer.displayClientMessage(component, true);
                }
            }
        }
    }

    /**
     * Updates the flag that indicates whether all players in the world are sleeping.
     */
    public void updateSleepingPlayerList() {
        if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
            this.announceSleepStatus();
        }
    }

    public ServerScoreboard getScoreboard() {
        return this.server.getScoreboard();
    }

    private void advanceWeatherCycle() {
        boolean flag = this.isRaining();
        if (this.dimensionType().hasSkyLight()) {
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int i = this.serverLevelData.getClearWeatherTime();
                int j = this.serverLevelData.getThunderTime();
                int k = this.serverLevelData.getRainTime();
                boolean flag1 = this.levelData.isThundering();
                boolean flag2 = this.levelData.isRaining();
                if (i > 0) {
                    --i;
                    j = flag1 ? 0 : 1;
                    k = flag2 ? 0 : 1;
                    flag1 = false;
                    flag2 = false;
                } else {
                    if (j > 0) {
                        if (--j == 0) {
                            flag1 = !flag1;
                        }
                    } else if (flag1) {
                        j = THUNDER_DURATION.sample(this.random);
                    } else {
                        j = THUNDER_DELAY.sample(this.random);
                    }

                    if (k > 0) {
                        if (--k == 0) {
                            flag2 = !flag2;
                        }
                    } else if (flag2) {
                        k = RAIN_DURATION.sample(this.random);
                    } else {
                        k = RAIN_DELAY.sample(this.random);
                    }
                }

                this.serverLevelData.setThunderTime(j);
                this.serverLevelData.setRainTime(k);
                this.serverLevelData.setClearWeatherTime(i);
                this.serverLevelData.setThundering(flag1);
                this.serverLevelData.setRaining(flag2);
            }

            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) {
                this.thunderLevel += 0.01F;
            } else {
                this.thunderLevel -= 0.01F;
            }

            this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
            this.oRainLevel = this.rainLevel;
            if (this.levelData.isRaining()) {
                this.rainLevel += 0.01F;
            } else {
                this.rainLevel -= 0.01F;
            }

            this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        if (this.oRainLevel != this.rainLevel) {
            this.server
                .getPlayerList()
                .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server
                .getPlayerList()
                .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        /* The function in use here has been replaced in order to only send the weather info to players in the correct dimension,
         * rather than to all players on the server. This is what causes the client-side rain, as the
         * client believes that it has started raining locally, rather than in another dimension.
         */
        if (flag != this.isRaining()) {
            if (flag) {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F), this.dimension());
            } else {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F), this.dimension());
            }

            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }
    }

    @VisibleForTesting
    public void resetWeatherCycle() {
        this.serverLevelData.setRainTime(0);
        this.serverLevelData.setRaining(false);
        this.serverLevelData.setThunderTime(0);
        this.serverLevelData.setThundering(false);
    }

    /**
     * Resets the updateEntityTick field to 0
     */
    public void resetEmptyTime() {
        this.emptyTime = 0;
    }

    private void tickFluid(BlockPos p_184077_, Fluid p_184078_) {
        FluidState fluidstate = this.getFluidState(p_184077_);
        if (fluidstate.is(p_184078_)) {
            fluidstate.tick(this, p_184077_);
        }
    }

    private void tickBlock(BlockPos p_184113_, Block p_184114_) {
        BlockState blockstate = this.getBlockState(p_184113_);
        if (blockstate.is(p_184114_)) {
            blockstate.tick(this, p_184113_, this.random);
        }
    }

    public void tickNonPassenger(Entity p_8648_) {
        p_8648_.setOldPosAndRot();
        ProfilerFiller profilerfiller = this.getProfiler();
        ++p_8648_.tickCount;
        this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(p_8648_.getType()).toString());
        profilerfiller.incrementCounter("tickNonPassenger");
        p_8648_.tick();
        this.getProfiler().pop();

        for(Entity entity : p_8648_.getPassengers()) {
            this.tickPassenger(p_8648_, entity);
        }
    }

    private void tickPassenger(Entity pRidingEntity, Entity pPassengerEntity) {
        if (pPassengerEntity.isRemoved() || pPassengerEntity.getVehicle() != pRidingEntity) {
            pPassengerEntity.stopRiding();
        } else if (pPassengerEntity instanceof Player || this.entityTickList.contains(pPassengerEntity)) {
            pPassengerEntity.setOldPosAndRot();
            ++pPassengerEntity.tickCount;
            ProfilerFiller profilerfiller = this.getProfiler();
            profilerfiller.push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(pPassengerEntity.getType()).toString());
            profilerfiller.incrementCounter("tickPassenger");
                if (pPassengerEntity.canUpdate())
            pPassengerEntity.rideTick();
            profilerfiller.pop();

            for(Entity entity : pPassengerEntity.getPassengers()) {
                this.tickPassenger(pPassengerEntity, entity);
            }
        }
    }

    @Override
    public boolean mayInteract(Player pPlayer, BlockPos pPos) {
        return !this.server.isUnderSpawnProtection(this, pPos, pPlayer) && this.getWorldBorder().isWithinBounds(pPos);
    }

    public void save(@Nullable ProgressListener pProgress, boolean pFlush, boolean pSkipSave) {
        ServerChunkCache serverchunkcache = this.getChunkSource();
        if (!pSkipSave) {
            if (pProgress != null) {
                pProgress.progressStartNoAbort(Component.translatable("menu.savingLevel"));
            }

            this.saveLevelData();
            if (pProgress != null) {
                pProgress.progressStage(Component.translatable("menu.savingChunks"));
            }

            serverchunkcache.save(pFlush);
            if (pFlush) {
                this.entityManager.saveAll();
            } else {
                this.entityManager.autoSave();
            }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Save(this));
        }
    }

    /**
     * Saves the chunks to disk.
     */
    private void saveLevelData() {
        if (this.dragonFight != null) {
            this.server.getWorldData().setEndDragonFightData(this.dragonFight.saveData());
        }

        this.getChunkSource().getDataStorage().save();
    }

    public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> pTypeTest, Predicate<? super T> pPredicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities(pTypeTest, pPredicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> pTypeTest, Predicate<? super T> pPredicate, List<? super T> pOutput) {
        this.getEntities(pTypeTest, pPredicate, pOutput, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> pTypeTest, Predicate<? super T> pPredicate, List<? super T> pOutput, int pMaxResults) {
        this.getEntities().get(pTypeTest, p_261428_ -> {
            if (pPredicate.test(p_261428_)) {
                pOutput.add(p_261428_);
                if (pOutput.size() >= pMaxResults) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }

            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
    }

    public List<? extends EnderDragon> getDragons() {
        return this.getEntities(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
    }

    public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> pPredicate) {
        return this.getPlayers(pPredicate, Integer.MAX_VALUE);
    }

    public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> pPredicate, int pMaxResults) {
        List<ServerPlayer> list = Lists.newArrayList();

        for(ServerPlayer serverplayer : this.players) {
            if (pPredicate.test(serverplayer)) {
                list.add(serverplayer);
                if (list.size() >= pMaxResults) {
                    return list;
                }
            }
        }

        return list;
    }

    @Nullable
    public ServerPlayer getRandomPlayer() {
        List<ServerPlayer> list = this.getPlayers(LivingEntity::isAlive);
        return list.isEmpty() ? null : list.get(this.random.nextInt(list.size()));
    }

    @Override
    public boolean addFreshEntity(Entity pEntity) {
        return this.addEntity(pEntity);
    }

    /**
     * Used for "unnatural" ways of entities appearing in the world, e.g. summon command, interdimensional teleports
     */
    public boolean addWithUUID(Entity pEntity) {
        return this.addEntity(pEntity);
    }

    public void addDuringTeleport(Entity pEntity) {
        this.addEntity(pEntity);
    }

    public void addDuringCommandTeleport(ServerPlayer pPlayer) {
        this.addPlayer(pPlayer);
    }

    public void addDuringPortalTeleport(ServerPlayer pPlayer) {
        this.addPlayer(pPlayer);
    }

    public void addNewPlayer(ServerPlayer pPlayer) {
        this.addPlayer(pPlayer);
    }

    public void addRespawnedPlayer(ServerPlayer pPlayer) {
        this.addPlayer(pPlayer);
    }

    private void addPlayer(ServerPlayer pPlayer) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(pPlayer, this)).isCanceled()) return;
        Entity entity = this.getEntities().get(pPlayer.getUUID());
        if (entity != null) {
            LOGGER.warn("Force-added player with duplicate UUID {}", pPlayer.getUUID());
            entity.unRide();
            this.removePlayerImmediately((ServerPlayer)entity, Entity.RemovalReason.DISCARDED);
        }

        this.entityManager.addNewEntityWithoutEvent(pPlayer);
        pPlayer.onAddedToWorld();
    }

    /**
     * Called when an entity is spawned in the world. This includes players.
     */
    private boolean addEntity(Entity pEntity) {
        if (pEntity.isRemoved()) {
            LOGGER.warn("Tried to add entity {} but it was marked as removed already", EntityType.getKey(pEntity.getType()));
            return false;
        } else {
            if (this.entityManager.addNewEntity(pEntity)) {
                pEntity.onAddedToWorld();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Attempts to summon an entity and it's passangers. They will only be summoned if all entities are unique and not already in queue to be summoned.
     */
    public boolean tryAddFreshEntityWithPassengers(Entity pEntity) {
        if (pEntity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        } else {
            this.addFreshEntityWithPassengers(pEntity);
            return true;
        }
    }

    public void unload(LevelChunk pChunk) {
        pChunk.clearAllBlockEntities();
        pChunk.unregisterTickContainerFromLevel(this);
    }

    public void removePlayerImmediately(ServerPlayer pPlayer, Entity.RemovalReason pReason) {
        pPlayer.remove(pReason);
    }

    @Override
    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        for(ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            if (serverplayer != null && serverplayer.level() == this && serverplayer.getId() != pBreakerId) {
                double d0 = (double)pPos.getX() - serverplayer.getX();
                double d1 = (double)pPos.getY() - serverplayer.getY();
                double d2 = (double)pPos.getZ() - serverplayer.getZ();
                if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0) {
                    serverplayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
                }
            }
        }
    }

    @Override
    public void playSeededSound(
        @Nullable Player pPlayer,
        double pX,
        double pY,
        double pZ,
        Holder<SoundEvent> pSound,
        SoundSource pCategory,
        float pVolume,
        float pPitch,
        long pSeed
    ) {
        net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition event = net.neoforged.neoforge.event.EventHooks.onPlaySoundAtPosition(this, pX, pY, pZ, pSound, pCategory, pVolume, pPitch);
        if (event.isCanceled() || event.getSound() == null) return;
        pSound = event.getSound();
        pCategory = event.getSource();
        pVolume = event.getNewVolume();
        pPitch = event.getNewPitch();
        this.server
            .getPlayerList()
            .broadcast(
                pPlayer,
                pX,
                pY,
                pZ,
                (double)pSound.value().getRange(pVolume),
                this.dimension(),
                new ClientboundSoundPacket(pSound, pCategory, pX, pY, pZ, pVolume, pPitch, pSeed)
            );
    }

    @Override
    public void playSeededSound(
        @Nullable Player pPlayer, Entity pEntity, Holder<SoundEvent> pSound, SoundSource pCategory, float pVolume, float pPitch, long pSeed
    ) {
        net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity event = net.neoforged.neoforge.event.EventHooks.onPlaySoundAtEntity(pEntity, pSound, pCategory, pVolume, pPitch);
        if (event.isCanceled() || event.getSound() == null) return;
        pSound = event.getSound();
        pCategory = event.getSource();
        pVolume = event.getNewVolume();
        pPitch = event.getNewPitch();
        this.server
            .getPlayerList()
            .broadcast(
                pPlayer,
                pEntity.getX(),
                pEntity.getY(),
                pEntity.getZ(),
                (double)pSound.value().getRange(pVolume),
                this.dimension(),
                new ClientboundSoundEntityPacket(pSound, pCategory, pEntity, pVolume, pPitch, pSeed)
            );
    }

    @Override
    public void globalLevelEvent(int pId, BlockPos pPos, int pData) {
        if (this.getGameRules().getBoolean(GameRules.RULE_GLOBAL_SOUND_EVENTS)) {
            this.server.getPlayerList().broadcastAll(new ClientboundLevelEventPacket(pId, pPos, pData, true));
        } else {
            this.levelEvent(null, pId, pPos, pData);
        }
    }

    @Override
    public void levelEvent(@Nullable Player pPlayer, int pType, BlockPos pPos, int pData) {
        this.server
            .getPlayerList()
            .broadcast(
                pPlayer,
                (double)pPos.getX(),
                (double)pPos.getY(),
                (double)pPos.getZ(),
                64.0,
                this.dimension(),
                new ClientboundLevelEventPacket(pType, pPos, pData, false)
            );
    }

    public int getLogicalHeight() {
        return this.dimensionType().logicalHeight();
    }

    @Override
    public void gameEvent(GameEvent pEvent, Vec3 pPosition, GameEvent.Context pContext) {
        if (!net.neoforged.neoforge.common.CommonHooks.onVanillaGameEvent(this, pEvent, pPosition, pContext)) return;
        this.gameEventDispatcher.post(pEvent, pPosition, pContext);
    }

    /**
     * Flags are as in setBlockState
     */
    @Override
    public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        if (this.isUpdatingNavigations) {
            String s = "recursive call to sendBlockUpdated";
            Util.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
        }

        this.getChunkSource().blockChanged(pPos);
        VoxelShape voxelshape1 = pOldState.getCollisionShape(this, pPos);
        VoxelShape voxelshape = pNewState.getCollisionShape(this, pPos);
        if (Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.NOT_SAME)) {
            List<PathNavigation> list = new ObjectArrayList<>();

            for(Mob mob : this.navigatingMobs) {
                PathNavigation pathnavigation = mob.getNavigation();
                if (pathnavigation.shouldRecomputePath(pPos)) {
                    list.add(pathnavigation);
                }
            }

            try {
                this.isUpdatingNavigations = true;

                for(PathNavigation pathnavigation1 : list) {
                    pathnavigation1.recomputePath();
                }
            } finally {
                this.isUpdatingNavigations = false;
            }
        }
    }

    @Override
    public void updateNeighborsAt(BlockPos pPos, Block pBlock) {
        net.neoforged.neoforge.event.EventHooks.onNeighborNotify(this, pPos, this.getBlockState(pPos), java.util.EnumSet.allOf(Direction.class), false).isCanceled();
        this.neighborUpdater.updateNeighborsAtExceptFromFacing(pPos, pBlock, null);
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos pPos, Block pBlockType, Direction pSkipSide) {
        java.util.EnumSet<Direction> directions = java.util.EnumSet.allOf(Direction.class);
        directions.remove(pSkipSide);
        if (net.neoforged.neoforge.event.EventHooks.onNeighborNotify(this, pPos, this.getBlockState(pPos), directions, false).isCanceled())
            return;
        this.neighborUpdater.updateNeighborsAtExceptFromFacing(pPos, pBlockType, pSkipSide);
    }

    @Override
    public void neighborChanged(BlockPos pPos, Block pBlock, BlockPos pFromPos) {
        this.neighborUpdater.neighborChanged(pPos, pBlock, pFromPos);
    }

    @Override
    public void neighborChanged(BlockState pState, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        this.neighborUpdater.neighborChanged(pState, pPos, pBlock, pFromPos, pIsMoving);
    }

    /**
     * Sends a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket} to all tracked players of that entity.
     */
    @Override
    public void broadcastEntityEvent(Entity pEntity, byte pState) {
        this.getChunkSource().broadcastAndSend(pEntity, new ClientboundEntityEventPacket(pEntity, pState));
    }

    @Override
    public void broadcastDamageEvent(Entity pEntity, DamageSource pDamageSource) {
        this.getChunkSource().broadcastAndSend(pEntity, new ClientboundDamageEventPacket(pEntity, pDamageSource));
    }

    /**
     * Gets the world's chunk provider
     */
    public ServerChunkCache getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public Explosion explode(
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        SoundEvent pExplosionSound
    ) {
        Explosion explosion = this.explode(
            pSource, pDamageSource, pDamageCalculator, pX, pY, pZ, pRadius, pFire, pExplosionInteraction, false, pSmallExplosionParticles, pLargeExplosionParticles, pExplosionSound
        );
        if (!explosion.interactsWithBlocks()) {
            explosion.clearToBlow();
        }

        for(ServerPlayer serverplayer : this.players) {
            if (serverplayer.distanceToSqr(pX, pY, pZ) < 4096.0) {
                serverplayer.connection
                    .send(
                        new ClientboundExplodePacket(
                            pX,
                            pY,
                            pZ,
                            pRadius,
                            explosion.getToBlow(),
                            explosion.getHitPlayers().get(serverplayer),
                            explosion.getBlockInteraction(),
                            explosion.getSmallExplosionParticles(),
                            explosion.getLargeExplosionParticles(),
                            explosion.getExplosionSound()
                        )
                    );
            }
        }

        return explosion;
    }

    @Override
    public void blockEvent(BlockPos pPos, Block pBlock, int pEventID, int pEventParam) {
        this.blockEvents.add(new BlockEventData(pPos, pBlock, pEventID, pEventParam));
    }

    private void runBlockEvents() {
        this.blockEventsToReschedule.clear();

        while(!this.blockEvents.isEmpty()) {
            BlockEventData blockeventdata = this.blockEvents.removeFirst();
            if (this.shouldTickBlocksAt(blockeventdata.pos())) {
                if (this.doBlockEvent(blockeventdata)) {
                    this.server
                        .getPlayerList()
                        .broadcast(
                            null,
                            (double)blockeventdata.pos().getX(),
                            (double)blockeventdata.pos().getY(),
                            (double)blockeventdata.pos().getZ(),
                            64.0,
                            this.dimension(),
                            new ClientboundBlockEventPacket(blockeventdata.pos(), blockeventdata.block(), blockeventdata.paramA(), blockeventdata.paramB())
                        );
                }
            } else {
                this.blockEventsToReschedule.add(blockeventdata);
            }
        }

        this.blockEvents.addAll(this.blockEventsToReschedule);
    }

    private boolean doBlockEvent(BlockEventData pEvent) {
        BlockState blockstate = this.getBlockState(pEvent.pos());
        return blockstate.is(pEvent.block()) ? blockstate.triggerEvent(this, pEvent.pos(), pEvent.paramA(), pEvent.paramB()) : false;
    }

    public LevelTicks<Block> getBlockTicks() {
        return this.blockTicks;
    }

    public LevelTicks<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Nonnull
    @Override
    public MinecraftServer getServer() {
        return this.server;
    }

    public PortalForcer getPortalForcer() {
        return this.portalForcer;
    }

    public StructureTemplateManager getStructureManager() {
        return this.server.getStructureManager();
    }

    public <T extends ParticleOptions> int sendParticles(
        T pType, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed
    ) {
        ClientboundLevelParticlesPacket clientboundlevelparticlespacket = new ClientboundLevelParticlesPacket(
            pType, false, pPosX, pPosY, pPosZ, (float)pXOffset, (float)pYOffset, (float)pZOffset, (float)pSpeed, pParticleCount
        );
        int i = 0;

        for(int j = 0; j < this.players.size(); ++j) {
            ServerPlayer serverplayer = this.players.get(j);
            if (this.sendParticles(serverplayer, false, pPosX, pPosY, pPosZ, clientboundlevelparticlespacket)) {
                ++i;
            }
        }

        return i;
    }

    public <T extends ParticleOptions> boolean sendParticles(
        ServerPlayer pPlayer,
        T pType,
        boolean pLongDistance,
        double pPosX,
        double pPosY,
        double pPosZ,
        int pParticleCount,
        double pXOffset,
        double pYOffset,
        double pZOffset,
        double pSpeed
    ) {
        Packet<?> packet = new ClientboundLevelParticlesPacket(
            pType, pLongDistance, pPosX, pPosY, pPosZ, (float)pXOffset, (float)pYOffset, (float)pZOffset, (float)pSpeed, pParticleCount
        );
        return this.sendParticles(pPlayer, pLongDistance, pPosX, pPosY, pPosZ, packet);
    }

    private boolean sendParticles(ServerPlayer pPlayer, boolean pLongDistance, double pPosX, double pPosY, double pPosZ, Packet<?> pPacket) {
        if (pPlayer.level() != this) {
            return false;
        } else {
            BlockPos blockpos = pPlayer.blockPosition();
            if (blockpos.closerToCenterThan(new Vec3(pPosX, pPosY, pPosZ), pLongDistance ? 512.0 : 32.0)) {
                pPlayer.connection.send(pPacket);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this World.
     */
    @Nullable
    @Override
    public Entity getEntity(int pId) {
        return this.getEntities().get(pId);
    }

    @Deprecated
    @Nullable
    public Entity getEntityOrPart(int pId) {
        Entity entity = this.getEntities().get(pId);
        return entity != null ? entity : this.dragonParts.get(pId);
    }

    @Nullable
    public Entity getEntity(UUID pUniqueId) {
        return this.getEntities().get(pUniqueId);
    }

    @Nullable
    public BlockPos findNearestMapStructure(TagKey<Structure> pStructureTag, BlockPos pPos, int pRadius, boolean pSkipExistingChunks) {
        if (!this.server.getWorldData().worldGenOptions().generateStructures()) {
            return null;
        } else {
            Optional<HolderSet.Named<Structure>> optional = this.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(pStructureTag);
            if (optional.isEmpty()) {
                return null;
            } else {
                Pair<BlockPos, Holder<Structure>> pair = this.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(this, optional.get(), pPos, pRadius, pSkipExistingChunks);
                return pair != null ? pair.getFirst() : null;
            }
        }
    }

    @Nullable
    public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(Predicate<Holder<Biome>> pBiomePredicate, BlockPos pPos, int p_215072_, int p_215073_, int p_215074_) {
        return this.getChunkSource()
            .getGenerator()
            .getBiomeSource()
            .findClosestBiome3d(pPos, p_215072_, p_215073_, p_215074_, pBiomePredicate, this.getChunkSource().randomState().sampler(), this);
    }

    @Override
    public RecipeManager getRecipeManager() {
        return this.server.getRecipeManager();
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.server.tickRateManager();
    }

    @Override
    public boolean noSave() {
        return this.noSave;
    }

    public DimensionDataStorage getDataStorage() {
        return this.getChunkSource().getDataStorage();
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(String pMapName) {
        return this.getServer().overworld().getDataStorage().get(MapItemSavedData.factory(), pMapName);
    }

    @Override
    public void setMapData(String pMapName, MapItemSavedData pData) {
        this.getServer().overworld().getDataStorage().set(pMapName, pData);
    }

    @Override
    public int getFreeMapId() {
        return this.getServer().overworld().getDataStorage().computeIfAbsent(MapIndex.factory(), "idcounts").getFreeAuxValueForMap();
    }

    public void setDefaultSpawnPos(BlockPos pPos, float pAngle) {
        ChunkPos chunkpos = new ChunkPos(new BlockPos(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));
        this.levelData.setSpawn(pPos, pAngle);
        this.getChunkSource().removeRegionTicket(TicketType.START, chunkpos, 11, Unit.INSTANCE);
        this.getChunkSource().addRegionTicket(TicketType.START, new ChunkPos(pPos), 11, Unit.INSTANCE);
        this.getServer().getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(pPos, pAngle));
    }

    public LongSet getForcedChunks() {
        ForcedChunksSavedData forcedchunkssaveddata = this.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        return (LongSet)(forcedchunkssaveddata != null ? LongSets.unmodifiable(forcedchunkssaveddata.getChunks()) : LongSets.EMPTY_SET);
    }

    public boolean setChunkForced(int pChunkX, int pChunkZ, boolean pAdd) {
        ForcedChunksSavedData forcedchunkssaveddata = this.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks");
        ChunkPos chunkpos = new ChunkPos(pChunkX, pChunkZ);
        long i = chunkpos.toLong();
        boolean flag;
        if (pAdd) {
            flag = forcedchunkssaveddata.getChunks().add(i);
            if (flag) {
                this.getChunk(pChunkX, pChunkZ);
            }
        } else {
            flag = forcedchunkssaveddata.getChunks().remove(i);
        }

        forcedchunkssaveddata.setDirty(flag);
        if (flag) {
            this.getChunkSource().updateChunkForced(chunkpos, pAdd);
        }

        return flag;
    }

    @Override
    public List<ServerPlayer> players() {
        return this.players;
    }

    @Override
    public void onBlockStateChange(BlockPos pPos, BlockState pBlockState, BlockState pNewState) {
        Optional<Holder<PoiType>> optional = PoiTypes.forState(pBlockState);
        Optional<Holder<PoiType>> optional1 = PoiTypes.forState(pNewState);
        if (!Objects.equals(optional, optional1)) {
            BlockPos blockpos = pPos.immutable();
            optional.ifPresent(p_215081_ -> this.getServer().execute(() -> {
                    this.getPoiManager().remove(blockpos);
                    DebugPackets.sendPoiRemovedPacket(this, blockpos);
                }));
            optional1.ifPresent(p_215057_ -> this.getServer().execute(() -> {
                    this.getPoiManager().add(blockpos, p_215057_);
                    DebugPackets.sendPoiAddedPacket(this, blockpos);
                }));
        }
    }

    public PoiManager getPoiManager() {
        return this.getChunkSource().getPoiManager();
    }

    public boolean isVillage(BlockPos pPos) {
        return this.isCloseToVillage(pPos, 1);
    }

    public boolean isVillage(SectionPos pPos) {
        return this.isVillage(pPos.center());
    }

    public boolean isCloseToVillage(BlockPos pPos, int pSections) {
        if (pSections > 6) {
            return false;
        } else {
            return this.sectionsToVillage(SectionPos.of(pPos)) <= pSections;
        }
    }

    public int sectionsToVillage(SectionPos pPos) {
        return this.getPoiManager().sectionsToVillage(pPos);
    }

    public Raids getRaids() {
        return this.raids;
    }

    @Nullable
    public Raid getRaidAt(BlockPos pPos) {
        return this.raids.getNearbyRaid(pPos, 9216);
    }

    public boolean isRaided(BlockPos pPos) {
        return this.getRaidAt(pPos) != null;
    }

    public void onReputationEvent(ReputationEventType pType, Entity pTarget, ReputationEventHandler pHost) {
        pHost.onReputationEventFrom(pType, pTarget);
    }

    public void saveDebugReport(Path pPath) throws IOException {
        ChunkMap chunkmap = this.getChunkSource().chunkMap;

        try (Writer writer = Files.newBufferedWriter(pPath.resolve("stats.txt"))) {
            writer.write(String.format(Locale.ROOT, "spawning_chunks: %d\n", chunkmap.getDistanceManager().getNaturalSpawnChunkCount()));
            NaturalSpawner.SpawnState naturalspawner$spawnstate = this.getChunkSource().getLastSpawnState();
            if (naturalspawner$spawnstate != null) {
                for(Entry<MobCategory> entry : naturalspawner$spawnstate.getMobCategoryCounts().object2IntEntrySet()) {
                    writer.write(String.format(Locale.ROOT, "spawn_count.%s: %d\n", entry.getKey().getName(), entry.getIntValue()));
                }
            }

            writer.write(String.format(Locale.ROOT, "entities: %s\n", this.entityManager.gatherStats()));
            writer.write(String.format(Locale.ROOT, "block_entity_tickers: %d\n", this.blockEntityTickers.size()));
            writer.write(String.format(Locale.ROOT, "block_ticks: %d\n", this.getBlockTicks().count()));
            writer.write(String.format(Locale.ROOT, "fluid_ticks: %d\n", this.getFluidTicks().count()));
            writer.write("distance_manager: " + chunkmap.getDistanceManager().getDebugStatus() + "\n");
            writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
        }

        CrashReport crashreport = new CrashReport("Level dump", new Exception("dummy"));
        this.fillReportDetails(crashreport);

        try (Writer writer3 = Files.newBufferedWriter(pPath.resolve("example_crash.txt"))) {
            writer3.write(crashreport.getFriendlyReport());
        }

        Path path = pPath.resolve("chunks.csv");

        try (Writer writer4 = Files.newBufferedWriter(path)) {
            chunkmap.dumpChunks(writer4);
        }

        Path path1 = pPath.resolve("entity_chunks.csv");

        try (Writer writer5 = Files.newBufferedWriter(path1)) {
            this.entityManager.dumpSections(writer5);
        }

        Path path2 = pPath.resolve("entities.csv");

        try (Writer writer1 = Files.newBufferedWriter(path2)) {
            dumpEntities(writer1, this.getEntities().getAll());
        }

        Path path3 = pPath.resolve("block_entities.csv");

        try (Writer writer2 = Files.newBufferedWriter(path3)) {
            this.dumpBlockEntityTickers(writer2);
        }
    }

    private static void dumpEntities(Writer pWriter, Iterable<Entity> pEntities) throws IOException {
        CsvOutput csvoutput = CsvOutput.builder()
            .addColumn("x")
            .addColumn("y")
            .addColumn("z")
            .addColumn("uuid")
            .addColumn("type")
            .addColumn("alive")
            .addColumn("display_name")
            .addColumn("custom_name")
            .build(pWriter);

        for(Entity entity : pEntities) {
            Component component = entity.getCustomName();
            Component component1 = entity.getDisplayName();
            csvoutput.writeRow(
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getUUID(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                entity.isAlive(),
                component1.getString(),
                component != null ? component.getString() : null
            );
        }
    }

    private void dumpBlockEntityTickers(Writer pOutput) throws IOException {
        CsvOutput csvoutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(pOutput);

        for(TickingBlockEntity tickingblockentity : this.blockEntityTickers) {
            BlockPos blockpos = tickingblockentity.getPos();
            csvoutput.writeRow(blockpos.getX(), blockpos.getY(), blockpos.getZ(), tickingblockentity.getType());
        }
    }

    @VisibleForTesting
    public void clearBlockEvents(BoundingBox pBoundingBox) {
        this.blockEvents.removeIf(p_207568_ -> pBoundingBox.isInside(p_207568_.pos()));
    }

    @Override
    public void blockUpdated(BlockPos pPos, Block pBlock) {
        if (!this.isDebug()) {
            this.updateNeighborsAt(pPos, pBlock);
        }
    }

    @Override
    public float getShade(Direction pDirection, boolean pShade) {
        return 1.0F;
    }

    /**
     * Gets an unmodifiable iterator of all loaded entities in the world.
     */
    public Iterable<Entity> getAllEntities() {
        return this.getEntities().getAll();
    }

    @Override
    public String toString() {
        return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
    }

    public boolean isFlat() {
        return this.server.getWorldData().isFlatWorld();
    }

    /**
     * Gets the random world seed.
     */
    @Override
    public long getSeed() {
        return this.server.getWorldData().worldGenOptions().seed();
    }

    @Nullable
    public EndDragonFight getDragonFight() {
        return this.dragonFight;
    }

    @Override
    public ServerLevel getLevel() {
        return this;
    }

    @VisibleForTesting
    public String getWatchdogStats() {
        return String.format(
            Locale.ROOT,
            "players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s",
            this.players.size(),
            this.entityManager.gatherStats(),
            getTypeCount(this.entityManager.getEntityGetter().getAll(), p_258244_ -> BuiltInRegistries.ENTITY_TYPE.getKey(p_258244_.getType()).toString()),
            this.blockEntityTickers.size(),
            getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType),
            this.getBlockTicks().count(),
            this.getFluidTicks().count(),
            this.gatherChunkSourceStats()
        );
    }

    private static <T> String getTypeCount(Iterable<T> pObjects, Function<T, String> pTypeGetter) {
        try {
            Object2IntOpenHashMap<String> object2intopenhashmap = new Object2IntOpenHashMap<>();

            for(T t : pObjects) {
                String s = pTypeGetter.apply(t);
                object2intopenhashmap.addTo(s, 1);
            }

            return object2intopenhashmap.object2IntEntrySet()
                .stream()
                .sorted(Comparator.<Entry<String>, Integer>comparing(Entry::getIntValue).reversed())
                .limit(5L)
                .map(p_207570_ -> (String)p_207570_.getKey() + ":" + p_207570_.getIntValue())
                .collect(Collectors.joining(","));
        } catch (Exception exception) {
            return "";
        }
    }

    public static void makeObsidianPlatform(ServerLevel pServerLevel) {
        BlockPos blockpos = END_SPAWN_POINT;
        int i = blockpos.getX();
        int j = blockpos.getY() - 2;
        int k = blockpos.getZ();
        BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2)
            .forEach(p_207578_ -> pServerLevel.setBlockAndUpdate(p_207578_, Blocks.AIR.defaultBlockState()));
        BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2)
            .forEach(p_184101_ -> pServerLevel.setBlockAndUpdate(p_184101_, Blocks.OBSIDIAN.defaultBlockState()));
    }

    @Override
    public LevelEntityGetter<Entity> getEntities() {
        return this.entityManager.getEntityGetter();
    }

    public void addLegacyChunkEntities(Stream<Entity> pEntities) {
        this.entityManager.addLegacyChunkEntities(pEntities);
    }

    public void addWorldGenChunkEntities(Stream<Entity> pEntities) {
        this.entityManager.addWorldGenChunkEntities(pEntities);
    }

    public void startTickingChunk(LevelChunk pChunk) {
        pChunk.unpackTicks(this.getLevelData().getGameTime());
    }

    public void onStructureStartsAvailable(ChunkAccess pChunk) {
        this.server.execute(() -> this.structureCheck.onStructureLoad(pChunk.getPos(), pChunk.getAllStarts()));
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.entityManager.close();
    }

    /**
     * Returns the name of the current chunk provider, by calling chunkprovider.makeString()
     */
    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[S] W: " + this.chunkSource.gatherStats() + " E: " + this.entityManager.gatherStats();
    }

    public boolean areEntitiesLoaded(long pChunkPos) {
        return this.entityManager.areEntitiesLoaded(pChunkPos);
    }

    private boolean isPositionTickingWithEntitiesLoaded(long p_184111_) {
        return this.areEntitiesLoaded(p_184111_) && this.chunkSource.isPositionTicking(p_184111_);
    }

    public boolean isPositionEntityTicking(BlockPos pPos) {
        return this.entityManager.canPositionTick(pPos) && this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.asLong(pPos));
    }

    public boolean isNaturalSpawningAllowed(BlockPos pPos) {
        return this.entityManager.canPositionTick(pPos);
    }

    public boolean isNaturalSpawningAllowed(ChunkPos pChunkPos) {
        return this.entityManager.canPositionTick(pChunkPos);
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.server.getWorldData().enabledFeatures();
    }

    public RandomSource getRandomSequence(ResourceLocation pLocation) {
        return this.randomSequences.get(pLocation);
    }

    public RandomSequences getRandomSequences() {
        return this.randomSequences;
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    @Override
    public CrashReportCategory fillReportDetails(CrashReport pReport) {
        CrashReportCategory crashreportcategory = super.fillReportDetails(pReport);
        crashreportcategory.setDetail("Loaded entity count", () -> String.valueOf(this.entityManager.count()));
        return crashreportcategory;
    }

    final class EntityCallbacks implements LevelCallback<Entity> {
        public void onCreated(Entity p_143355_) {
        }

        public void onDestroyed(Entity p_143359_) {
            ServerLevel.this.getScoreboard().entityRemoved(p_143359_);
        }

        public void onTickingStart(Entity p_143363_) {
            ServerLevel.this.entityTickList.add(p_143363_);
        }

        public void onTickingEnd(Entity p_143367_) {
            ServerLevel.this.entityTickList.remove(p_143367_);
        }

        public void onTrackingStart(Entity p_143371_) {
            ServerLevel.this.getChunkSource().addEntity(p_143371_);
            if (p_143371_ instanceof ServerPlayer serverplayer) {
                ServerLevel.this.players.add(serverplayer);
                ServerLevel.this.updateSleepingPlayerList();
            }

            if (p_143371_ instanceof Mob mob) {
                if (ServerLevel.this.isUpdatingNavigations) {
                    String s = "onTrackingStart called during navigation iteration";
                    Util.logAndPauseIfInIde(
                        "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
                    );
                }

                ServerLevel.this.navigatingMobs.add(mob);
            }

            if (p_143371_.isMultipartEntity()) {
                for(net.neoforged.neoforge.entity.PartEntity<?> enderdragonpart : p_143371_.getParts()) {
                    ServerLevel.this.dragonParts.put(enderdragonpart.getId(), enderdragonpart);
                }
            }

            p_143371_.updateDynamicGameEventListener(DynamicGameEventListener::add);
        }

        public void onTrackingEnd(Entity p_143375_) {
            ServerLevel.this.getChunkSource().removeEntity(p_143375_);
            if (p_143375_ instanceof ServerPlayer serverplayer) {
                ServerLevel.this.players.remove(serverplayer);
                ServerLevel.this.updateSleepingPlayerList();
            }

            if (p_143375_ instanceof Mob mob) {
                if (ServerLevel.this.isUpdatingNavigations) {
                    String s = "onTrackingStart called during navigation iteration";
                    Util.logAndPauseIfInIde(
                              "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
                    );
                }

                ServerLevel.this.navigatingMobs.remove(mob);
            }

            if (p_143375_.isMultipartEntity()) {
                for(net.neoforged.neoforge.entity.PartEntity<?> enderdragonpart : p_143375_.getParts()) {
                    ServerLevel.this.dragonParts.remove(enderdragonpart.getId());
                }
            }

            p_143375_.updateDynamicGameEventListener(DynamicGameEventListener::remove);

            p_143375_.onRemovedFromWorld();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent(p_143375_, ServerLevel.this));
        }

        public void onSectionChange(Entity p_215086_) {
            p_215086_.updateDynamicGameEventListener(DynamicGameEventListener::move);
        }
    }

    @Override
    public java.util.Collection<net.neoforged.neoforge.entity.PartEntity<?>> getPartEntities() {
        return this.dragonParts.values();
    }

    private final net.neoforged.neoforge.capabilities.CapabilityListenerHolder capListenerHolder = new net.neoforged.neoforge.capabilities.CapabilityListenerHolder();

    @Override
    public void invalidateCapabilities(BlockPos pos) {
        capListenerHolder.invalidatePos(pos);
    }

    @Override
    public void invalidateCapabilities(ChunkPos pos) {
        capListenerHolder.invalidateChunk(pos);
    }

    /**
     * Register a listener for capability invalidation.
     * @see net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener
     */
    public void registerCapabilityListener(BlockPos pos, net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener listener) {
        capListenerHolder.addListener(pos, listener);
    }

    /**
     * Internal method, used to clean capability listeners that are not referenced.
     * Do not call.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public void cleanCapabilityListenerReferences() {
        capListenerHolder.clean();
    }
}
