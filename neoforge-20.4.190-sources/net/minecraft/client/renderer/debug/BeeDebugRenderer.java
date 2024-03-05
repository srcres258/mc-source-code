package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
    private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
    private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
    private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
    private static final boolean SHOW_PATH_FOR_ALL_BEES = false;
    private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_PATH_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_MEMBERS = true;
    private static final boolean SHOW_BLACKLISTS = true;
    private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
    private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final int HIVE_TIMEOUT = 20;
    private static final float TEXT_SCALE = 0.02F;
    private static final int WHITE = -1;
    private static final int YELLOW = -256;
    private static final int ORANGE = -23296;
    private static final int GREEN = -16711936;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private static final int RED = -65536;
    private final Minecraft minecraft;
    private final Map<BlockPos, BeeDebugRenderer.HiveDebugInfo> hives = new HashMap<>();
    private final Map<UUID, BeeDebugPayload.BeeInfo> beeInfosPerEntity = new HashMap<>();
    @Nullable
    private UUID lastLookedAtUuid;

    public BeeDebugRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    @Override
    public void clear() {
        this.hives.clear();
        this.beeInfosPerEntity.clear();
        this.lastLookedAtUuid = null;
    }

    public void addOrUpdateHiveInfo(HiveDebugPayload.HiveInfo pHiveInfo, long pLastSeen) {
        this.hives.put(pHiveInfo.pos(), new BeeDebugRenderer.HiveDebugInfo(pHiveInfo, pLastSeen));
    }

    public void addOrUpdateBeeInfo(BeeDebugPayload.BeeInfo pBeeInfo) {
        this.beeInfosPerEntity.put(pBeeInfo.uuid(), pBeeInfo);
    }

    public void removeBeeInfo(int pId) {
        this.beeInfosPerEntity.values().removeIf(p_293626_ -> p_293626_.id() == pId);
    }

    @Override
    public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, double pCamX, double pCamY, double pCamZ) {
        this.clearRemovedHives();
        this.clearRemovedBees();
        this.doRender(pPoseStack, pBuffer);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void clearRemovedBees() {
        this.beeInfosPerEntity.entrySet().removeIf(p_293633_ -> this.minecraft.level.getEntity(p_293633_.getValue().id()) == null);
    }

    private void clearRemovedHives() {
        long i = this.minecraft.level.getGameTime() - 20L;
        this.hives.entrySet().removeIf(p_293628_ -> p_293628_.getValue().lastSeen() < i);
    }

    private void doRender(PoseStack pPoseStack, MultiBufferSource pBuffer) {
        BlockPos blockpos = this.getCamera().getBlockPosition();
        this.beeInfosPerEntity.values().forEach(p_293636_ -> {
            if (this.isPlayerCloseEnoughToMob(p_293636_)) {
                this.renderBeeInfo(pPoseStack, pBuffer, p_293636_);
            }
        });
        this.renderFlowerInfos(pPoseStack, pBuffer);

        for(BlockPos blockpos1 : this.hives.keySet()) {
            if (blockpos.closerThan(blockpos1, 30.0)) {
                highlightHive(pPoseStack, pBuffer, blockpos1);
            }
        }

        Map<BlockPos, Set<UUID>> map = this.createHiveBlacklistMap();
        this.hives.values().forEach(p_293646_ -> {
            if (blockpos.closerThan(p_293646_.info.pos(), 30.0)) {
                Set<UUID> set = map.get(p_293646_.info.pos());
                this.renderHiveInfo(pPoseStack, pBuffer, p_293646_.info, (Collection<UUID>)(set == null ? Sets.newHashSet() : set));
            }
        });
        this.getGhostHives().forEach((p_269699_, p_269700_) -> {
            if (blockpos.closerThan(p_269699_, 30.0)) {
                this.renderGhostHive(pPoseStack, pBuffer, p_269699_, p_269700_);
            }
        });
    }

    private Map<BlockPos, Set<UUID>> createHiveBlacklistMap() {
        Map<BlockPos, Set<UUID>> map = Maps.newHashMap();
        this.beeInfosPerEntity
            .values()
            .forEach(
                p_293638_ -> p_293638_.blacklistedHives()
                        .forEach(p_293641_ -> map.computeIfAbsent(p_293641_, p_173777_ -> Sets.newHashSet()).add(p_293638_.uuid()))
            );
        return map;
    }

    private void renderFlowerInfos(PoseStack pPoseStack, MultiBufferSource pBuffer) {
        Map<BlockPos, Set<UUID>> map = Maps.newHashMap();
        this.beeInfosPerEntity.values().forEach(p_293651_ -> {
            if (p_293651_.flowerPos() != null) {
                map.computeIfAbsent(p_293651_.flowerPos(), p_293649_ -> new HashSet()).add(p_293651_.uuid());
            }
        });
        map.forEach((p_293631_, p_293632_) -> {
            Set<String> set = p_293632_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
            int i = 1;
            renderTextOverPos(pPoseStack, pBuffer, set.toString(), p_293631_, i++, -256);
            renderTextOverPos(pPoseStack, pBuffer, "Flower", p_293631_, i++, -1);
            float f = 0.05F;
            DebugRenderer.renderFilledBox(pPoseStack, pBuffer, p_293631_, 0.05F, 0.8F, 0.8F, 0.0F, 0.3F);
        });
    }

    private static String getBeeUuidsAsString(Collection<UUID> pBeeUuids) {
        if (pBeeUuids.isEmpty()) {
            return "-";
        } else {
            return pBeeUuids.size() > 3
                ? pBeeUuids.size() + " bees"
                : pBeeUuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
        }
    }

    private static void highlightHive(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pHivePos) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pHivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
    }

    private void renderGhostHive(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pHivePos, List<String> p_270221_) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pHivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
        renderTextOverPos(pPoseStack, pBuffer, p_270221_ + "", pHivePos, 0, -256);
        renderTextOverPos(pPoseStack, pBuffer, "Ghost Hive", pHivePos, 1, -65536);
    }

    private void renderHiveInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, HiveDebugPayload.HiveInfo pHiveInfo, Collection<UUID> pBeeUuids) {
        int i = 0;
        if (!pBeeUuids.isEmpty()) {
            renderTextOverHive(pPoseStack, pBuffer, "Blacklisted by " + getBeeUuidsAsString(pBeeUuids), pHiveInfo, i++, -65536);
        }

        renderTextOverHive(pPoseStack, pBuffer, "Out: " + getBeeUuidsAsString(this.getHiveMembers(pHiveInfo.pos())), pHiveInfo, i++, -3355444);
        if (pHiveInfo.occupantCount() == 0) {
            renderTextOverHive(pPoseStack, pBuffer, "In: -", pHiveInfo, i++, -256);
        } else if (pHiveInfo.occupantCount() == 1) {
            renderTextOverHive(pPoseStack, pBuffer, "In: 1 bee", pHiveInfo, i++, -256);
        } else {
            renderTextOverHive(pPoseStack, pBuffer, "In: " + pHiveInfo.occupantCount() + " bees", pHiveInfo, i++, -256);
        }

        renderTextOverHive(pPoseStack, pBuffer, "Honey: " + pHiveInfo.honeyLevel(), pHiveInfo, i++, -23296);
        renderTextOverHive(pPoseStack, pBuffer, pHiveInfo.hiveType() + (pHiveInfo.sedated() ? " (sedated)" : ""), pHiveInfo, i++, -1);
    }

    private void renderPath(PoseStack pPoseStack, MultiBufferSource pBuffer, BeeDebugPayload.BeeInfo pBeeInfo) {
        if (pBeeInfo.path() != null) {
            PathfindingRenderer.renderPath(
                pPoseStack,
                pBuffer,
                pBeeInfo.path(),
                0.5F,
                false,
                false,
                this.getCamera().getPosition().x(),
                this.getCamera().getPosition().y(),
                this.getCamera().getPosition().z()
            );
        }
    }

    private void renderBeeInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, BeeDebugPayload.BeeInfo pBeeInfo) {
        boolean flag = this.isBeeSelected(pBeeInfo);
        int i = 0;
        renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, pBeeInfo.toString(), -1, 0.03F);
        if (pBeeInfo.hivePos() == null) {
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, "No hive", -98404, 0.02F);
        } else {
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, "Hive: " + this.getPosDescription(pBeeInfo, pBeeInfo.hivePos()), -256, 0.02F);
        }

        if (pBeeInfo.flowerPos() == null) {
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, "No flower", -98404, 0.02F);
        } else {
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, "Flower: " + this.getPosDescription(pBeeInfo, pBeeInfo.flowerPos()), -256, 0.02F);
        }

        for(String s : pBeeInfo.goals()) {
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, s, -16711936, 0.02F);
        }

        if (flag) {
            this.renderPath(pPoseStack, pBuffer, pBeeInfo);
        }

        if (pBeeInfo.travelTicks() > 0) {
            int j = pBeeInfo.travelTicks() < 600 ? -3355444 : -23296;
            renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), i++, "Travelling: " + pBeeInfo.travelTicks() + " ticks", j, 0.02F);
        }
    }

    private static void renderTextOverHive(
        PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, HiveDebugPayload.HiveInfo pHiveInfo, int pLayer, int pColor
    ) {
        renderTextOverPos(pPoseStack, pBuffer, pText, pHiveInfo.pos(), pLayer, pColor);
    }

    private static void renderTextOverPos(PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, BlockPos pPos, int pLayer, int pColor) {
        double d0 = 1.3;
        double d1 = 0.2;
        double d2 = (double)pPos.getX() + 0.5;
        double d3 = (double)pPos.getY() + 1.3 + (double)pLayer * 0.2;
        double d4 = (double)pPos.getZ() + 0.5;
        DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, d2, d3, d4, pColor, 0.02F, true, 0.0F, true);
    }

    private static void renderTextOverMob(
        PoseStack pPoseStack, MultiBufferSource pBuffer, Position pPos, int pLayer, String pText, int pColor, float pScale
    ) {
        double d0 = 2.4;
        double d1 = 0.25;
        BlockPos blockpos = BlockPos.containing(pPos);
        double d2 = (double)blockpos.getX() + 0.5;
        double d3 = pPos.y() + 2.4 + (double)pLayer * 0.25;
        double d4 = (double)blockpos.getZ() + 0.5;
        float f = 0.5F;
        DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, d2, d3, d4, pColor, pScale, false, 0.5F, true);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }

    private Set<String> getHiveMemberNames(HiveDebugPayload.HiveInfo pHiveInfo) {
        return this.getHiveMembers(pHiveInfo.pos()).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
    }

    private String getPosDescription(BeeDebugPayload.BeeInfo pBeeInfo, BlockPos pPos) {
        double d0 = Math.sqrt(pPos.distToCenterSqr(pBeeInfo.pos()));
        double d1 = (double)Math.round(d0 * 10.0) / 10.0;
        return pPos.toShortString() + " (dist " + d1 + ")";
    }

    private boolean isBeeSelected(BeeDebugPayload.BeeInfo pBeeInfo) {
        return Objects.equals(this.lastLookedAtUuid, pBeeInfo.uuid());
    }

    private boolean isPlayerCloseEnoughToMob(BeeDebugPayload.BeeInfo pBeeInfo) {
        Player player = this.minecraft.player;
        BlockPos blockpos = BlockPos.containing(player.getX(), pBeeInfo.pos().y(), player.getZ());
        BlockPos blockpos1 = BlockPos.containing(pBeeInfo.pos());
        return blockpos.closerThan(blockpos1, 30.0);
    }

    private Collection<UUID> getHiveMembers(BlockPos pPos) {
        return this.beeInfosPerEntity
            .values()
            .stream()
            .filter(p_293648_ -> p_293648_.hasHive(pPos))
            .map(BeeDebugPayload.BeeInfo::uuid)
            .collect(Collectors.toSet());
    }

    private Map<BlockPos, List<String>> getGhostHives() {
        Map<BlockPos, List<String>> map = Maps.newHashMap();

        for(BeeDebugPayload.BeeInfo beedebugpayload$beeinfo : this.beeInfosPerEntity.values()) {
            if (beedebugpayload$beeinfo.hivePos() != null && !this.hives.containsKey(beedebugpayload$beeinfo.hivePos())) {
                map.computeIfAbsent(beedebugpayload$beeinfo.hivePos(), p_113140_ -> Lists.newArrayList()).add(beedebugpayload$beeinfo.generateName());
            }
        }

        return map;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
    }

    @OnlyIn(Dist.CLIENT)
    static record HiveDebugInfo(HiveDebugPayload.HiveInfo info, long lastSeen) {
    }
}
