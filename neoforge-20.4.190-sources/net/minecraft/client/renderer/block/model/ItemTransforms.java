package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemTransforms {
    public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms();
    public final ItemTransform thirdPersonLeftHand;
    public final ItemTransform thirdPersonRightHand;
    public final ItemTransform firstPersonLeftHand;
    public final ItemTransform firstPersonRightHand;
    public final ItemTransform head;
    public final ItemTransform gui;
    public final ItemTransform ground;
    public final ItemTransform fixed;
    public final com.google.common.collect.ImmutableMap<ItemDisplayContext, ItemTransform> moddedTransforms;

    private ItemTransforms() {
        this(
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM,
            ItemTransform.NO_TRANSFORM
        );
    }

    public ItemTransforms(ItemTransforms pTransforms) {
        this.thirdPersonLeftHand = pTransforms.thirdPersonLeftHand;
        this.thirdPersonRightHand = pTransforms.thirdPersonRightHand;
        this.firstPersonLeftHand = pTransforms.firstPersonLeftHand;
        this.firstPersonRightHand = pTransforms.firstPersonRightHand;
        this.head = pTransforms.head;
        this.gui = pTransforms.gui;
        this.ground = pTransforms.ground;
        this.fixed = pTransforms.fixed;
        this.moddedTransforms = pTransforms.moddedTransforms;
    }

    @Deprecated
    public ItemTransforms(
        ItemTransform pThirdPersonLeftHand,
        ItemTransform pThirdPersonRightHand,
        ItemTransform pFirstPersonLeftHand,
        ItemTransform pFirstPersonRightHand,
        ItemTransform pHead,
        ItemTransform pGui,
        ItemTransform pGround,
        ItemTransform pFixed
    ) {
        this(pThirdPersonLeftHand, pThirdPersonRightHand, pFirstPersonLeftHand, pFirstPersonRightHand, pHead, pGui, pGround, pFixed, com.google.common.collect.ImmutableMap.of());
    }

    public ItemTransforms(
        ItemTransform p_111798_,
        ItemTransform p_111799_,
        ItemTransform p_111800_,
        ItemTransform p_111801_,
        ItemTransform p_111802_,
        ItemTransform p_111803_,
        ItemTransform p_111804_,
        ItemTransform p_111805_,
        com.google.common.collect.ImmutableMap<ItemDisplayContext, ItemTransform> moddedTransforms
    ) {
        this.thirdPersonLeftHand = p_111798_;
        this.thirdPersonRightHand = p_111799_;
        this.firstPersonLeftHand = p_111800_;
        this.firstPersonRightHand = p_111801_;
        this.head = p_111802_;
        this.gui = p_111803_;
        this.ground = p_111804_;
        this.fixed = p_111805_;
        this.moddedTransforms = moddedTransforms;
    }

    public ItemTransform getTransform(ItemDisplayContext pDisplayContext) {
        return switch(pDisplayContext) {
            case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
            case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
            case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
            case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
            case HEAD -> this.head;
            case GUI -> this.gui;
            case GROUND -> this.ground;
            case FIXED -> this.fixed;
            default -> ItemTransform.NO_TRANSFORM;
        };
    }

    public boolean hasTransform(ItemDisplayContext pDisplayContext) {
        return this.getTransform(pDisplayContext) != ItemTransform.NO_TRANSFORM;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<ItemTransforms> {
        public ItemTransforms deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            ItemTransform itemtransform = this.getTransform(pContext, jsonobject, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            ItemTransform itemtransform1 = this.getTransform(pContext, jsonobject, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
            if (itemtransform1 == ItemTransform.NO_TRANSFORM) {
                itemtransform1 = itemtransform;
            }

            ItemTransform itemtransform2 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            ItemTransform itemtransform3 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            if (itemtransform3 == ItemTransform.NO_TRANSFORM) {
                itemtransform3 = itemtransform2;
            }

            ItemTransform itemtransform4 = this.getTransform(pContext, jsonobject, ItemDisplayContext.HEAD);
            ItemTransform itemtransform5 = this.getTransform(pContext, jsonobject, ItemDisplayContext.GUI);
            ItemTransform itemtransform6 = this.getTransform(pContext, jsonobject, ItemDisplayContext.GROUND);
            ItemTransform itemtransform7 = this.getTransform(pContext, jsonobject, ItemDisplayContext.FIXED);

            var builder = com.google.common.collect.ImmutableMap.<ItemDisplayContext, ItemTransform>builder();
            for (ItemDisplayContext type : ItemDisplayContext.values()) {
                if (type.isModded()) {
                    var transform = this.getTransform(pContext, jsonobject, type);
                    var fallbackType = type;
                    while (transform == ItemTransform.NO_TRANSFORM && fallbackType.fallback() != null) {
                        fallbackType = fallbackType.fallback();
                        transform = this.getTransform(pContext, jsonobject, fallbackType);
                    }
                    if (transform != ItemTransform.NO_TRANSFORM){
                        builder.put(type, transform);
                    }
                }
            }

            return new ItemTransforms(itemtransform1, itemtransform, itemtransform3, itemtransform2, itemtransform4, itemtransform5, itemtransform6, itemtransform7, builder.build());
        }

        private ItemTransform getTransform(JsonDeserializationContext pDeserializationContext, JsonObject pJson, ItemDisplayContext pDisplayContext) {
            String s = pDisplayContext.getSerializedName();
            return pJson.has(s) ? pDeserializationContext.deserialize(pJson.get(s), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
        }
    }
}
