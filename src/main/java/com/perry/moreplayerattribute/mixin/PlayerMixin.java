package com.perry.moreplayerattribute.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.perry.moreplayerattribute.core.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 把五个属性挂到玩家身上。
 *
 * <p>DefaultAttributes 在静态初始化时调用 {@code Player.createAttributes()} 构建玩家的
 * AttributeSupplier，所以在这里加进去，玩家（含客户端本地玩家，LocalPlayer 也是 Player）
 * 就都有了这些属性。无参的 add 取的是 RangedAttribute 构造里传的默认值，也就是 1.0。</p>
 *
 * <p>这一步不能省：属性只注册不挂载的话，读它只会抛 IllegalArgumentException，而不是拿到默认值。</p>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
    private static AttributeSupplier.Builder moreplayerattribute$addAttributes(AttributeSupplier.Builder builder) {
        return builder
                .add(ModAttributes.AIR_RESISTANCE)
                .add(ModAttributes.BLOCK_RESISTANCE)
                .add(ModAttributes.WATER_RESISTANCE)
                .add(ModAttributes.LAVA_RESISTANCE)
                .add(ModAttributes.MOVE_ACCELERATION);
    }
}
