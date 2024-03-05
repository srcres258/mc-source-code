package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class BlockMath {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL = Util.make(Maps.newEnumMap(Direction.class), p_121851_ -> {
        p_121851_.put(Direction.SOUTH, Transformation.identity());
        p_121851_.put(Direction.EAST, new Transformation(null, new Quaternionf().rotateY((float) (Math.PI / 2)), null, null));
        p_121851_.put(Direction.WEST, new Transformation(null, new Quaternionf().rotateY((float) (-Math.PI / 2)), null, null));
        p_121851_.put(Direction.NORTH, new Transformation(null, new Quaternionf().rotateY((float) Math.PI), null, null));
        p_121851_.put(Direction.UP, new Transformation(null, new Quaternionf().rotateX((float) (-Math.PI / 2)), null, null));
        p_121851_.put(Direction.DOWN, new Transformation(null, new Quaternionf().rotateX((float) (Math.PI / 2)), null, null));
    });
    public static final Map<Direction, Transformation> VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL = Util.make(Maps.newEnumMap(Direction.class), p_121849_ -> {
        for(Direction direction : Direction.values()) {
            p_121849_.put(direction, VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction).inverse());
        }
    });

    public static Transformation blockCenterToCorner(Transformation pTransformation) {
        Matrix4f matrix4f = new Matrix4f().translation(0.5F, 0.5F, 0.5F);
        matrix4f.mul(pTransformation.getMatrix());
        matrix4f.translate(-0.5F, -0.5F, -0.5F);
        return new Transformation(matrix4f);
    }

    public static Transformation blockCornerToCenter(Transformation pTransformation) {
        Matrix4f matrix4f = new Matrix4f().translation(-0.5F, -0.5F, -0.5F);
        matrix4f.mul(pTransformation.getMatrix());
        matrix4f.translate(0.5F, 0.5F, 0.5F);
        return new Transformation(matrix4f);
    }

    public static Transformation getUVLockTransform(Transformation pTransformation, Direction pDirection, Supplier<String> pWarningMessage) {
        Direction direction = Direction.rotate(pTransformation.getMatrix(), pDirection);
        Transformation transformation = pTransformation.inverse();
        if (transformation == null) {
            LOGGER.warn(pWarningMessage.get());
            return new Transformation(null, null, new Vector3f(0.0F, 0.0F, 0.0F), null);
        } else {
            Transformation transformation1 = VANILLA_UV_TRANSFORM_GLOBAL_TO_LOCAL.get(pDirection)
                .compose(transformation)
                .compose(VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction));
            return blockCenterToCorner(transformation1);
        }
    }
}
