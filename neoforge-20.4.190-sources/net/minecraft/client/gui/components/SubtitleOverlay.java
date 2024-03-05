package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.ListenerTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SubtitleOverlay implements SoundEventListener {
    private static final long DISPLAY_TIME = 3000L;
    private final Minecraft minecraft;
    private final List<SubtitleOverlay.Subtitle> subtitles = Lists.newArrayList();
    private boolean isListening;
    private final List<SubtitleOverlay.Subtitle> audibleSubtitles = new ArrayList<>();

    public SubtitleOverlay(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void render(GuiGraphics pGuiGraphics) {
        SoundManager soundmanager = this.minecraft.getSoundManager();
        if (!this.isListening && this.minecraft.options.showSubtitles().get()) {
            soundmanager.addListener(this);
            this.isListening = true;
        } else if (this.isListening && !this.minecraft.options.showSubtitles().get()) {
            soundmanager.removeListener(this);
            this.isListening = false;
        }

        if (this.isListening) {
            ListenerTransform listenertransform = soundmanager.getListenerTransform();
            Vec3 vec3 = listenertransform.position();
            Vec3 vec31 = listenertransform.forward();
            Vec3 vec32 = listenertransform.right();
            this.audibleSubtitles.clear();

            for(SubtitleOverlay.Subtitle subtitleoverlay$subtitle : this.subtitles) {
                if (subtitleoverlay$subtitle.isAudibleFrom(vec3)) {
                    this.audibleSubtitles.add(subtitleoverlay$subtitle);
                }
            }

            if (!this.audibleSubtitles.isEmpty()) {
                int i2 = 0;
                int j2 = 0;
                double d0 = this.minecraft.options.notificationDisplayTime().get();
                Iterator<SubtitleOverlay.Subtitle> iterator = this.audibleSubtitles.iterator();

                while(iterator.hasNext()) {
                    SubtitleOverlay.Subtitle subtitleoverlay$subtitle1 = iterator.next();
                    if ((double)subtitleoverlay$subtitle1.getTime() + 3000.0 * d0 <= (double)Util.getMillis()) {
                        iterator.remove();
                    } else {
                        j2 = Math.max(j2, this.minecraft.font.width(subtitleoverlay$subtitle1.getText()));
                    }
                }

                j2 += this.minecraft.font.width("<") + this.minecraft.font.width(" ") + this.minecraft.font.width(">") + this.minecraft.font.width(" ");

                for(SubtitleOverlay.Subtitle subtitleoverlay$subtitle2 : this.audibleSubtitles) {
                    int i = 255;
                    Component component = subtitleoverlay$subtitle2.getText();
                    Vec3 vec33 = subtitleoverlay$subtitle2.getLocation().subtract(vec3).normalize();
                    double d1 = vec32.dot(vec33);
                    double d2 = vec31.dot(vec33);
                    boolean flag = d2 > 0.5;
                    int j = j2 / 2;
                    int k = 9;
                    int l = k / 2;
                    float f = 1.0F;
                    int i1 = this.minecraft.font.width(component);
                    int j1 = Mth.floor(Mth.clampedLerp(255.0F, 75.0F, (float)(Util.getMillis() - subtitleoverlay$subtitle2.getTime()) / (float)(3000.0 * d0)));
                    int k1 = j1 << 16 | j1 << 8 | j1;
                    pGuiGraphics.pose().pushPose();
                    pGuiGraphics.pose()
                        .translate(
                            (float)pGuiGraphics.guiWidth() - (float)j * 1.0F - 2.0F, (float)(pGuiGraphics.guiHeight() - 35) - (float)(i2 * (k + 1)) * 1.0F, 0.0F
                        );
                    pGuiGraphics.pose().scale(1.0F, 1.0F, 1.0F);
                    pGuiGraphics.fill(-j - 1, -l - 1, j + 1, l + 1, this.minecraft.options.getBackgroundColor(0.8F));
                    int l1 = k1 + -16777216;
                    if (!flag) {
                        if (d1 > 0.0) {
                            pGuiGraphics.drawString(this.minecraft.font, ">", j - this.minecraft.font.width(">"), -l, l1);
                        } else if (d1 < 0.0) {
                            pGuiGraphics.drawString(this.minecraft.font, "<", -j, -l, l1);
                        }
                    }

                    pGuiGraphics.drawString(this.minecraft.font, component, -i1 / 2, -l, l1);
                    pGuiGraphics.pose().popPose();
                    ++i2;
                }
            }
        }
    }

    @Override
    public void onPlaySound(SoundInstance pSound, WeighedSoundEvents pAccessor, float pRange) {
        if (pAccessor.getSubtitle() != null) {
            Component component = pAccessor.getSubtitle();
            if (!this.subtitles.isEmpty()) {
                for(SubtitleOverlay.Subtitle subtitleoverlay$subtitle : this.subtitles) {
                    if (subtitleoverlay$subtitle.getText().equals(component)) {
                        subtitleoverlay$subtitle.refresh(new Vec3(pSound.getX(), pSound.getY(), pSound.getZ()));
                        return;
                    }
                }
            }

            this.subtitles.add(new SubtitleOverlay.Subtitle(component, pRange, new Vec3(pSound.getX(), pSound.getY(), pSound.getZ())));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Subtitle {
        private final Component text;
        private final float range;
        private long time;
        private Vec3 location;

        public Subtitle(Component pText, float pRange, Vec3 pLocation) {
            this.text = pText;
            this.range = pRange;
            this.location = pLocation;
            this.time = Util.getMillis();
        }

        public Component getText() {
            return this.text;
        }

        public long getTime() {
            return this.time;
        }

        public Vec3 getLocation() {
            return this.location;
        }

        public void refresh(Vec3 pLocation) {
            this.location = pLocation;
            this.time = Util.getMillis();
        }

        public boolean isAudibleFrom(Vec3 pLocation) {
            return Float.isInfinite(this.range) || pLocation.closerThan(this.location, (double)this.range);
        }
    }
}
