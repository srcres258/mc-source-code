package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.SampleLogger;
import net.minecraft.util.TimeUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
    private static final int RED = -65536;
    private static final int YELLOW = -256;
    private static final int GREEN = -16711936;
    private final Supplier<Float> msptSupplier;

    public TpsDebugChart(Font pFont, SampleLogger pLogger, Supplier<Float> pMsptSupplier) {
        super(pFont, pLogger);
        this.msptSupplier = pMsptSupplier;
    }

    @Override
    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
        float f = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get();
        this.drawStringWithShade(pGuiGraphics, String.format("%.1f TPS", f), pX + 1, pHeight - 60 + 1);
    }

    @Override
    protected String toDisplayString(double pValue) {
        return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(pValue)));
    }

    @Override
    protected int getSampleHeight(double pValue) {
        return (int)Math.round(toMilliseconds(pValue) * 60.0 / (double)this.msptSupplier.get().floatValue());
    }

    @Override
    protected int getSampleColor(long pValue) {
        float f = this.msptSupplier.get();
        return this.getSampleColor(toMilliseconds((double)pValue), 0.0, -16711936, (double)f / 2.0, -256, (double)f, -65536);
    }

    private static double toMilliseconds(double pValue) {
        return pValue / 1000000.0;
    }
}
