package com.perry.moreplayerattribute.mixin;

import com.perry.moreplayerattribute.core.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Attributes.class)
public class AttributesMixin {
    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void onBootstrap(Registry<Attribute> registry, CallbackInfoReturnable<Holder<Attribute>> cir) {
        ModAttributes.init();
    }
}
