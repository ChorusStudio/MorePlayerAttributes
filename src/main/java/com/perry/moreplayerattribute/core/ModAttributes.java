package com.perry.moreplayerattribute.core;

import com.perry.moreplayerattribute.Moreplayerattribute;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class ModAttributes {

    /** 空气阻力倍率，作用于整个速度向量（水平 0.91 与垂直 0.98 各缩放一次）。1.0 = 原版。 */
    public static final Holder<Attribute> AIR_RESISTANCE = register("air_resistance");

    /** 地面阻尼倍率，只作用于水平分量（地面摩擦是接触力，不该影响下落）。1.0 = 原版方块摩擦 * 0.91。 */
    public static final Holder<Attribute> BLOCK_RESISTANCE = register("block_resistance");

    /** 水阻力倍率，作用于整个速度向量（水平 getWaterSlowDown() 系列系数 + 垂直 0.8 下沉阻力）。 */
    public static final Holder<Attribute> WATER_RESISTANCE = register("water_resistance");

    /** 岩浆阻力倍率，作用于整个速度向量（水平 0.5 + 垂直 0.8）。1.0 = 原版。 */
    public static final Holder<Attribute> LAVA_RESISTANCE = register("lava_resistance");

    /** 地面加速倍率。1.0 = 原版 getSpeed() * 0.216 / friction^3。 */
    public static final Holder<Attribute> MOVE_ACCELERATION = register("move_acceleration");

    private static Holder<Attribute> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Moreplayerattribute.MOD_ID, path);
        return Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                id,
                new RangedAttribute(id.toLanguageKey("attribute.name"), 1.0, 0.0, 10.0)
                        // 双端同步的关键：客户端本地预测与服务端校验必须读到同一个值。
                        .setSyncable(true)
                        // 只影响 tooltip 里数值的颜色。
                        .setSentiment(Attribute.Sentiment.NEUTRAL)
        );
    }

    /** 空方法，仅用于从 mixin 触发本类的静态初始化。 */
    public static void init() {}
}
