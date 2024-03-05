package net.minecraft.world.level;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.Difficulty;

public final class LevelSettings {
    private final String levelName;
    private final GameType gameType;
    private final boolean hardcore;
    private final Difficulty difficulty;
    private final boolean allowCommands;
    private final GameRules gameRules;
    private final WorldDataConfiguration dataConfiguration;
    private final com.mojang.serialization.Lifecycle lifecycle;

    public LevelSettings(
        String pLevelName, GameType pGameType, boolean pHardcore, Difficulty pDifficulty, boolean pAllowCommands, GameRules pGameRules, WorldDataConfiguration pDataConfiguration
    ) {
        this(pLevelName, pGameType, pHardcore, pDifficulty, pAllowCommands, pGameRules, pDataConfiguration, com.mojang.serialization.Lifecycle.stable());
    }
    public LevelSettings(String p_250485_, GameType p_250207_, boolean p_251631_, Difficulty p_252122_, boolean p_248961_, GameRules p_248536_, WorldDataConfiguration p_249797_, com.mojang.serialization.Lifecycle lifecycle) {
        this.levelName = p_250485_;
        this.gameType = p_250207_;
        this.hardcore = p_251631_;
        this.difficulty = p_252122_;
        this.allowCommands = p_248961_;
        this.gameRules = p_248536_;
        this.dataConfiguration = p_249797_;
        this.lifecycle = lifecycle;
    }

    public static LevelSettings parse(Dynamic<?> pLevelData, WorldDataConfiguration pDataConfiguration) {
        GameType gametype = GameType.byId(pLevelData.get("GameType").asInt(0));
        return new LevelSettings(
            pLevelData.get("LevelName").asString(""),
            gametype,
            pLevelData.get("hardcore").asBoolean(false),
            pLevelData.get("Difficulty").asNumber().map(p_46928_ -> Difficulty.byId(p_46928_.byteValue())).result().orElse(Difficulty.NORMAL),
            pLevelData.get("allowCommands").asBoolean(gametype == GameType.CREATIVE),
            new GameRules(pLevelData.get("GameRules")),
            pDataConfiguration,
            net.neoforged.neoforge.common.CommonHooks.parseLifecycle(pLevelData.get("forgeLifecycle").asString("stable"))
        );
    }

    public String levelName() {
        return this.levelName;
    }

    public GameType gameType() {
        return this.gameType;
    }

    public boolean hardcore() {
        return this.hardcore;
    }

    public Difficulty difficulty() {
        return this.difficulty;
    }

    public boolean allowCommands() {
        return this.allowCommands;
    }

    public GameRules gameRules() {
        return this.gameRules;
    }

    public WorldDataConfiguration getDataConfiguration() {
        return this.dataConfiguration;
    }

    public LevelSettings withGameType(GameType pGameType) {
        return new LevelSettings(this.levelName, pGameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, this.dataConfiguration, this.lifecycle);
    }

    public LevelSettings withDifficulty(Difficulty pDifficulty) {
        net.neoforged.neoforge.common.CommonHooks.onDifficultyChange(pDifficulty, this.difficulty);
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, pDifficulty, this.allowCommands, this.gameRules, this.dataConfiguration, this.lifecycle);
    }

    public LevelSettings withDataConfiguration(WorldDataConfiguration pDataConfiguration) {
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, dataConfiguration, this.lifecycle);
    }

    public LevelSettings copy() {
        return new LevelSettings(
            this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules.copy(), this.dataConfiguration, this.lifecycle
        );
    }
    public LevelSettings withLifecycle(com.mojang.serialization.Lifecycle lifecycle) {
        return new LevelSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, this.dataConfiguration, lifecycle);
    }
    public com.mojang.serialization.Lifecycle getLifecycle() {
        return this.lifecycle;
    }
}
