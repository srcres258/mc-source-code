package net.minecraft.client.resources.metadata.texture;

import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMetadataSectionSerializer implements MetadataSectionSerializer<TextureMetadataSection> {
    public TextureMetadataSection fromJson(JsonObject pJson) {
        boolean flag = GsonHelper.getAsBoolean(pJson, "blur", false);
        boolean flag1 = GsonHelper.getAsBoolean(pJson, "clamp", false);
        return new TextureMetadataSection(flag, flag1);
    }

    /**
     * The name of this section type as it appears in JSON.
     */
    @Override
    public String getMetadataSectionName() {
        return "texture";
    }
}
