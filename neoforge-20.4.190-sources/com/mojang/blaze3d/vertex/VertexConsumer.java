package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer extends net.neoforged.neoforge.client.extensions.IVertexConsumerExtension {
    VertexConsumer vertex(double pX, double pY, double pZ);

    VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha);

    VertexConsumer uv(float pU, float pV);

    VertexConsumer overlayCoords(int pU, int pV);

    VertexConsumer uv2(int pU, int pV);

    VertexConsumer normal(float pX, float pY, float pZ);

    void endVertex();

    default void vertex(
        float pX,
        float pY,
        float pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        float pTexU,
        float pTexV,
        int pOverlayUV,
        int pLightmapUV,
        float pNormalX,
        float pNormalY,
        float pNormalZ
    ) {
        this.vertex((double)pX, (double)pY, (double)pZ);
        this.color(pRed, pGreen, pBlue, pAlpha);
        this.uv(pTexU, pTexV);
        this.overlayCoords(pOverlayUV);
        this.uv2(pLightmapUV);
        this.normal(pNormalX, pNormalY, pNormalZ);
        this.endVertex();
    }

    void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA);

    void unsetDefaultColor();

    default VertexConsumer color(float pRed, float pGreen, float pBlue, float pAlpha) {
        return this.color((int)(pRed * 255.0F), (int)(pGreen * 255.0F), (int)(pBlue * 255.0F), (int)(pAlpha * 255.0F));
    }

    default VertexConsumer color(int pColorARGB) {
        return this.color(
            FastColor.ARGB32.red(pColorARGB), FastColor.ARGB32.green(pColorARGB), FastColor.ARGB32.blue(pColorARGB), FastColor.ARGB32.alpha(pColorARGB)
        );
    }

    default VertexConsumer uv2(int pLightmapUV) {
        return this.uv2(pLightmapUV & 65535, pLightmapUV >> 16 & 65535);
    }

    default VertexConsumer overlayCoords(int pOverlayUV) {
        return this.overlayCoords(pOverlayUV & 65535, pOverlayUV >> 16 & 65535);
    }

    default void putBulkData(PoseStack.Pose pPoseEntry, BakedQuad pQuad, float pRed, float pGreen, float pBlue, int pCombinedLight, int pCombinedOverlay) {
        this.putBulkData(
            pPoseEntry,
            pQuad,
            new float[]{1.0F, 1.0F, 1.0F, 1.0F},
            pRed,
            pGreen,
            pBlue,
            new int[]{pCombinedLight, pCombinedLight, pCombinedLight, pCombinedLight},
            pCombinedOverlay,
            false
        );
    }

    default void putBulkData(
        PoseStack.Pose pPoseEntry,
        BakedQuad pQuad,
        float[] pColorMuls,
        float pRed,
        float pGreen,
        float pBlue,
        int[] pCombinedLights,
        int pCombinedOverlay,
        boolean pMulColor
    ) {
        putBulkData(pPoseEntry, pQuad, pColorMuls, pRed, pGreen, pBlue, 1, pCombinedLights, pCombinedOverlay, pMulColor);
    }

    default void putBulkData(PoseStack.Pose p_85996_, BakedQuad p_85997_, float[] p_85998_, float p_85999_, float p_86000_, float p_86001_, float alpha, int[] p_86002_, int p_86003_, boolean p_86004_) {
        float[] afloat = new float[]{p_85998_[0], p_85998_[1], p_85998_[2], p_85998_[3]};
        int[] aint = new int[]{p_86002_[0], p_86002_[1], p_86002_[2], p_86002_[3]};
        int[] aint1 = p_85997_.getVertices();
        Vec3i vec3i = p_85997_.getDirection().getNormal();
        Matrix4f matrix4f = p_85996_.pose();
        Vector3f vector3f = p_85996_.normal().transform(new Vector3f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ()));
        int i = 8;
        int j = aint1.length / 8;

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for(int k = 0; k < j; ++k) {
                intbuffer.clear();
                intbuffer.put(aint1, k * 8, 8);
                float f = bytebuffer.getFloat(0);
                float f1 = bytebuffer.getFloat(4);
                float f2 = bytebuffer.getFloat(8);
                float f3;
                float f4;
                float f5;
                if (p_86004_) {
                    float f6 = (float)(bytebuffer.get(12) & 255) / 255.0F;
                    float f7 = (float)(bytebuffer.get(13) & 255) / 255.0F;
                    float f8 = (float)(bytebuffer.get(14) & 255) / 255.0F;
                    f3 = f6 * afloat[k] * p_85999_;
                    f4 = f7 * afloat[k] * p_86000_;
                    f5 = f8 * afloat[k] * p_86001_;
                } else {
                    f3 = afloat[k] * p_85999_;
                    f4 = afloat[k] * p_86000_;
                    f5 = afloat[k] * p_86001_;
                }

                int l = applyBakedLighting(p_86002_[k], bytebuffer);
                float f9 = bytebuffer.getFloat(16);
                float f10 = bytebuffer.getFloat(20);
                Vector4f vector4f = matrix4f.transform(new Vector4f(f, f1, f2, 1.0F));
                applyBakedNormals(vector3f, bytebuffer, p_85996_.normal());
                float vertexAlpha = p_86004_ ? alpha * (float) (bytebuffer.get(15) & 255) / 255.0F : alpha;
                this.vertex(vector4f.x(), vector4f.y(), vector4f.z(), f3, f4, f5, vertexAlpha, f9, f10, p_86003_, l, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }

    default VertexConsumer vertex(Matrix4f pMatrix, float pX, float pY, float pZ) {
        Vector4f vector4f = pMatrix.transform(new Vector4f(pX, pY, pZ, 1.0F));
        return this.vertex((double)vector4f.x(), (double)vector4f.y(), (double)vector4f.z());
    }

    default VertexConsumer normal(Matrix3f pMatrix, float pX, float pY, float pZ) {
        Vector3f vector3f = pMatrix.transform(new Vector3f(pX, pY, pZ));
        return this.normal(vector3f.x(), vector3f.y(), vector3f.z());
    }
}
