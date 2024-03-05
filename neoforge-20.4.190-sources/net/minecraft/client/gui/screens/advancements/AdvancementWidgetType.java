package net.minecraft.client.gui.screens.advancements;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AdvancementWidgetType {
    OBTAINED(
        new ResourceLocation("advancements/box_obtained"),
        new ResourceLocation("advancements/task_frame_obtained"),
        new ResourceLocation("advancements/challenge_frame_obtained"),
        new ResourceLocation("advancements/goal_frame_obtained")
    ),
    UNOBTAINED(
        new ResourceLocation("advancements/box_unobtained"),
        new ResourceLocation("advancements/task_frame_unobtained"),
        new ResourceLocation("advancements/challenge_frame_unobtained"),
        new ResourceLocation("advancements/goal_frame_unobtained")
    );

    private final ResourceLocation boxSprite;
    private final ResourceLocation taskFrameSprite;
    private final ResourceLocation challengeFrameSprite;
    private final ResourceLocation goalFrameSprite;

    private AdvancementWidgetType(ResourceLocation pBoxSprite, ResourceLocation pTaskFrameSprite, ResourceLocation pChallengeFrameSprite, ResourceLocation pGoalFrameSprite) {
        this.boxSprite = pBoxSprite;
        this.taskFrameSprite = pTaskFrameSprite;
        this.challengeFrameSprite = pChallengeFrameSprite;
        this.goalFrameSprite = pGoalFrameSprite;
    }

    public ResourceLocation boxSprite() {
        return this.boxSprite;
    }

    public ResourceLocation frameSprite(AdvancementType pType) {
        return switch(pType) {
            case TASK -> this.taskFrameSprite;
            case CHALLENGE -> this.challengeFrameSprite;
            case GOAL -> this.goalFrameSprite;
        };
    }
}
