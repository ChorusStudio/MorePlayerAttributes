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

    @ModifyConstant(method = "travelInAir", constant = @Constant(floatValue = 0.91F))
    private float moreplayerattribute$horizontalDamping(float constant) {
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

    @WrapOperation(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 morepayerattribute$fixAcceleration(LivingEntity instance, Vec3 dir, float friction, Operation<Vec3> original) {
        return original.call(instance, dir, Mth.clamp(friction, 0.1f, 1f));
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
