package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V705 extends NamespacedSchema {
    protected static final HookFunction ADD_NAMES = new HookFunction() {
        @Override
        public <T> T apply(DynamicOps<T> p_18167_, T p_18168_) {
            return V99.addNames(new Dynamic<>(p_18167_, p_18168_), V704.ITEM_TO_BLOCKENTITY, "minecraft:armor_stand");
        }
    };

    public V705(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> V100.equipment(pSchema));
    }

    protected static void registerThrowableProjectile(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        pSchema.registerSimple(map, "minecraft:area_effect_cloud");
        registerMob(pSchema, map, "minecraft:armor_stand");
        pSchema.register(map, "minecraft:arrow", p_18164_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
        registerMob(pSchema, map, "minecraft:bat");
        registerMob(pSchema, map, "minecraft:blaze");
        pSchema.registerSimple(map, "minecraft:boat");
        registerMob(pSchema, map, "minecraft:cave_spider");
        pSchema.register(
            map,
            "minecraft:chest_minecart",
            p_18161_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        registerMob(pSchema, map, "minecraft:chicken");
        pSchema.register(map, "minecraft:commandblock_minecart", p_18158_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
        registerMob(pSchema, map, "minecraft:cow");
        registerMob(pSchema, map, "minecraft:creeper");
        pSchema.register(
            map,
            "minecraft:donkey",
            p_18155_ -> DSL.optionalFields(
                    "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
                )
        );
        pSchema.registerSimple(map, "minecraft:dragon_fireball");
        registerThrowableProjectile(pSchema, map, "minecraft:egg");
        registerMob(pSchema, map, "minecraft:elder_guardian");
        pSchema.registerSimple(map, "minecraft:ender_crystal");
        registerMob(pSchema, map, "minecraft:ender_dragon");
        pSchema.register(map, "minecraft:enderman", p_18146_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(pSchema), V100.equipment(pSchema)));
        registerMob(pSchema, map, "minecraft:endermite");
        registerThrowableProjectile(pSchema, map, "minecraft:ender_pearl");
        pSchema.registerSimple(map, "minecraft:eye_of_ender_signal");
        pSchema.register(
            map,
            "minecraft:falling_block",
            p_18143_ -> DSL.optionalFields("Block", References.BLOCK_NAME.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
        );
        registerThrowableProjectile(pSchema, map, "minecraft:fireball");
        pSchema.register(map, "minecraft:fireworks_rocket", p_18140_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(pSchema)));
        pSchema.register(map, "minecraft:furnace_minecart", p_18137_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
        registerMob(pSchema, map, "minecraft:ghast");
        registerMob(pSchema, map, "minecraft:giant");
        registerMob(pSchema, map, "minecraft:guardian");
        pSchema.register(
            map,
            "minecraft:hopper_minecart",
            p_18134_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        pSchema.register(
            map,
            "minecraft:horse",
            p_18131_ -> DSL.optionalFields(
                    "ArmorItem", References.ITEM_STACK.in(pSchema), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
                )
        );
        registerMob(pSchema, map, "minecraft:husk");
        pSchema.register(map, "minecraft:item", p_18128_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        pSchema.register(map, "minecraft:item_frame", p_18125_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        pSchema.registerSimple(map, "minecraft:leash_knot");
        registerMob(pSchema, map, "minecraft:magma_cube");
        pSchema.register(map, "minecraft:minecart", p_18122_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
        registerMob(pSchema, map, "minecraft:mooshroom");
        pSchema.register(
            map,
            "minecraft:mule",
            p_18119_ -> DSL.optionalFields(
                    "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
                )
        );
        registerMob(pSchema, map, "minecraft:ocelot");
        pSchema.registerSimple(map, "minecraft:painting");
        pSchema.registerSimple(map, "minecraft:parrot");
        registerMob(pSchema, map, "minecraft:pig");
        registerMob(pSchema, map, "minecraft:polar_bear");
        pSchema.register(
            map, "minecraft:potion", p_18116_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema), "inTile", References.BLOCK_NAME.in(pSchema))
        );
        registerMob(pSchema, map, "minecraft:rabbit");
        registerMob(pSchema, map, "minecraft:sheep");
        registerMob(pSchema, map, "minecraft:shulker");
        pSchema.registerSimple(map, "minecraft:shulker_bullet");
        registerMob(pSchema, map, "minecraft:silverfish");
        registerMob(pSchema, map, "minecraft:skeleton");
        pSchema.register(
            map, "minecraft:skeleton_horse", p_18113_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema))
        );
        registerMob(pSchema, map, "minecraft:slime");
        registerThrowableProjectile(pSchema, map, "minecraft:small_fireball");
        registerThrowableProjectile(pSchema, map, "minecraft:snowball");
        registerMob(pSchema, map, "minecraft:snowman");
        pSchema.register(
            map,
            "minecraft:spawner_minecart",
            p_18110_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
        );
        pSchema.register(map, "minecraft:spectral_arrow", p_18107_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
        registerMob(pSchema, map, "minecraft:spider");
        registerMob(pSchema, map, "minecraft:squid");
        registerMob(pSchema, map, "minecraft:stray");
        pSchema.registerSimple(map, "minecraft:tnt");
        pSchema.register(map, "minecraft:tnt_minecart", p_18104_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
        pSchema.register(
            map,
            "minecraft:villager",
            p_18101_ -> DSL.optionalFields(
                    "Inventory",
                    DSL.list(References.ITEM_STACK.in(pSchema)),
                    "Offers",
                    DSL.optionalFields(
                        "Recipes",
                        DSL.list(
                            DSL.optionalFields(
                                "buy",
                                References.ITEM_STACK.in(pSchema),
                                "buyB",
                                References.ITEM_STACK.in(pSchema),
                                "sell",
                                References.ITEM_STACK.in(pSchema)
                            )
                        )
                    ),
                    V100.equipment(pSchema)
                )
        );
        registerMob(pSchema, map, "minecraft:villager_golem");
        registerMob(pSchema, map, "minecraft:witch");
        registerMob(pSchema, map, "minecraft:wither");
        registerMob(pSchema, map, "minecraft:wither_skeleton");
        registerThrowableProjectile(pSchema, map, "minecraft:wither_skull");
        registerMob(pSchema, map, "minecraft:wolf");
        registerThrowableProjectile(pSchema, map, "minecraft:xp_bottle");
        pSchema.registerSimple(map, "minecraft:xp_orb");
        registerMob(pSchema, map, "minecraft:zombie");
        pSchema.register(
            map, "minecraft:zombie_horse", p_18092_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema))
        );
        registerMob(pSchema, map, "minecraft:zombie_pigman");
        registerMob(pSchema, map, "minecraft:zombie_villager");
        pSchema.registerSimple(map, "minecraft:evocation_fangs");
        registerMob(pSchema, map, "minecraft:evocation_illager");
        pSchema.registerSimple(map, "minecraft:illusion_illager");
        pSchema.register(
            map,
            "minecraft:llama",
            p_18081_ -> DSL.optionalFields(
                    "Items",
                    DSL.list(References.ITEM_STACK.in(pSchema)),
                    "SaddleItem",
                    References.ITEM_STACK.in(pSchema),
                    "DecorItem",
                    References.ITEM_STACK.in(pSchema),
                    V100.equipment(pSchema)
                )
        );
        pSchema.registerSimple(map, "minecraft:llama_spit");
        registerMob(pSchema, map, "minecraft:vex");
        registerMob(pSchema, map, "minecraft:vindication_illager");
        return map;
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(true, References.ENTITY, () -> DSL.taggedChoiceLazy("id", namespacedString(), pEntityTypes));
        pSchema.registerType(
            true,
            References.ITEM_STACK,
            () -> DSL.hook(
                    DSL.optionalFields(
                        "id",
                        References.ITEM_NAME.in(pSchema),
                        "tag",
                        DSL.optionalFields(
                            "EntityTag",
                            References.ENTITY_TREE.in(pSchema),
                            "BlockEntityTag",
                            References.BLOCK_ENTITY.in(pSchema),
                            "CanDestroy",
                            DSL.list(References.BLOCK_NAME.in(pSchema)),
                            "CanPlaceOn",
                            DSL.list(References.BLOCK_NAME.in(pSchema)),
                            "Items",
                            DSL.list(References.ITEM_STACK.in(pSchema))
                        )
                    ),
                    ADD_NAMES,
                    HookFunction.IDENTITY
                )
        );
    }
}
