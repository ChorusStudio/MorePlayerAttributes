package com.perry.moreplayerattribute.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.perry.moreplayerattribute.Moreplayerattribute;
import com.perry.moreplayerattribute.core.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    public abstract @Nullable AttributeInstance getAttribute(Holder<Attribute> holder);

    @Shadow
    public abstract double getAttributeValue(Holder<Attribute> holder);

    @Shadow
    public abstract boolean shouldDiscardFriction();

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private boolean isNotPlayer(){
        return getType() != EntityType.PLAYER;
    }

    @Unique
    private double fromAttribute(double old, AttributeInstance instance) {
        return Mth.clamp(1.0 - (1.0 - old) * instance.getValue(), 0.0, 1.0);
    }

    @WrapOperation(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float moreplayerattribute$blockFrictionModify(Block instance, Operation<Float> original) {
        float old = original.call(instance);
        if (isNotPlayer()) {
            return old;
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.BLOCK_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing block resistance attribute on " + getPlainTextName());
            return old;
        }
        return (float) fromAttribute(old, attributeInstance);
    }

    /**
     * 原版 travelInAir 中水平阻尼是 {@code vec3.x/z * (方块摩擦 * 0.91F)}，其中 0.91F 属于共享常量。
     *
     * <p>这里不能用 {@code @ModifyConstant} 去改那个 0.91F：Constant 修改属于 Redirect 型注入，
     * 同一个指令只允许一个注入器认领，优先级最高者胜出，输掉的一方会直接抛
     * {@code InjectionError}。Carpet 的 {@code LivingEntity_creativeFlyMixin}（creativeFlyDrag）
     * 也用 {@code @ModifyConstant(expect = 1)} 改这个常量，所以两边必然有一方启动崩溃。
     *
     * <p>改成 WrapOperation（MixinExtras，可叠加）包裹 handleRelativeFrictionAndCalculateMovement：
     * 该调用的返回值随后就要乘上 0.91F，所以把返回值按 {@code fromAttribute(0.91F) / 0.91F} 缩放
     * 与改那个常量完全等价（地面上还会再乘方块摩擦，也一并保留）。
     */
    @WrapOperation(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 morepayerattribute$fixAcceleration(LivingEntity instance, Vec3 dir, float friction, Operation<Vec3> original) {
        Vec3 movement = original.call(instance, dir, Mth.clamp(friction, 0.1f, 1f));
        if (isNotPlayer() || shouldDiscardFriction()) {
            // discardFriction 的分支根本不会乘阻尼系数，不需要（也不应该）缩放。
            return movement;
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.AIR_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing air resistance attribute on " + getPlainTextName());
            return movement;
        }
        double scale = fromAttribute(0.91F, attributeInstance) / 0.91F;
        if (scale == 1.0) {
            return movement;
        }
        return new Vec3(movement.x * scale, movement.y, movement.z * scale);
    }

    @ModifyConstant(method = "travelInAir", constant = @Constant(floatValue = 0.98F))
    private float moreplayerattribute$verticalAirDamping(float constant) {
        if (isNotPlayer()) {
            return constant;
        }
        AttributeInstance instance = getAttribute(ModAttributes.AIR_RESISTANCE);
        if (instance == null) {
            Moreplayerattribute.LOGGER.severe("Missing air resistance attribute on " + getPlainTextName());
            return constant;
        }
        return (float) fromAttribute(constant, instance);
    }

    @WrapOperation(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moreplayerattribute$modifyElytraFriction(Vec3 instance, double x, double y, double z, Operation<Vec3> original) {
        if (isNotPlayer()) {
            return original.call(instance, x, y, z);
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.AIR_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing air resistance attribute on " + getPlainTextName());
            return original.call(instance, x, y ,z);
        }
        return original.call(instance, fromAttribute(x, attributeInstance), fromAttribute(y, attributeInstance), fromAttribute(z, attributeInstance));
    }

    @WrapOperation(
            method = "travelInWater",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getWaterSlowDown()F"
            )
    )
    private float moreplayerattribute$modifySwimmingWaterDrag(LivingEntity instance, Operation<Float> original) {
        float old = original.call(instance);
        if (isNotPlayer()) {
            return old;
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.WATER_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing water resistance attribute on " + getPlainTextName());
            return old;
        }
        return (float) fromAttribute(old, attributeInstance);
    }

    @WrapOperation(method = "travelInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moreplayerattribute$modifySinkingWaterDrag(Vec3 instance, double x, double y, double z, Operation<Vec3> original) {
        if (isNotPlayer()) {
            return original.call(instance, x, y ,z);
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.WATER_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing water resistance attribute on " + getPlainTextName());
            return original.call(instance, x, y ,z);
        }
        // the x and z already dealt
        return original.call(instance, x, fromAttribute(y, attributeInstance), z);
    }

    @ModifyConstant(method = "travelInWater", constant = @Constant(floatValue = 0.9f))
    private float moreplayerattribute$modifyNonSwimmingWaterDrag(float constant) {
        if (isNotPlayer()) {
            return constant;
        }
        AttributeInstance instance = getAttribute(ModAttributes.WATER_RESISTANCE);
        if (instance == null) {
            Moreplayerattribute.LOGGER.severe("Missing water resistance attribute on " + getPlainTextName());
            return constant;
        }
        return (float) fromAttribute(constant, instance);
    }

    @ModifyConstant(method = "travelInWater", constant = @Constant(floatValue = 0.96f))
    private float moreplayerattribute$modifyWithDolphineWaterDrag(float constant) {
        if (isNotPlayer()) {
            return constant;
        }
        AttributeInstance instance = getAttribute(ModAttributes.WATER_RESISTANCE);
        if (instance == null) {
            Moreplayerattribute.LOGGER.severe("Missing water resistance attribute on " + getPlainTextName());
            return constant;
        }
        return (float) fromAttribute(constant, instance);
    }

    @WrapOperation(method = "travelInLava", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moreplayerattribute$modifyLavaDrag(Vec3 instance, double scaler, Operation<Vec3> original) {
        if (isNotPlayer()) {
            return original.call(instance, scaler);
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.LAVA_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing lava resistance attribute on " + getPlainTextName());
            return original.call(instance, scaler);
        }
        return original.call(instance, fromAttribute(scaler, attributeInstance));
    }

    @WrapOperation(method = "travelInLava", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moreplayerattribute$modifyLavaDrag(Vec3 instance, double x, double y, double z, Operation<Vec3> original) {
        if (isNotPlayer()) {
            return original.call(instance, x, y, z);
        }
        AttributeInstance attributeInstance = getAttribute(ModAttributes.LAVA_RESISTANCE);
        if (attributeInstance == null) {
            Moreplayerattribute.LOGGER.severe("Missing lava resistance attribute on " + getPlainTextName());
            return original.call(instance, x, y ,z);
        }
        return original.call(instance, fromAttribute(x, attributeInstance), fromAttribute(y, attributeInstance), fromAttribute(z, attributeInstance));
    }

    @ModifyReturnValue(method = "getFrictionInfluencedSpeed", at = @At("RETURN"))
    private float moreplayerattribute$groundAcceleration(float original) {
        if (isNotPlayer()) {
            return original;
        }
        return (float) (original * getAttributeValue(ModAttributes.MOVE_ACCELERATION));
    }
}
