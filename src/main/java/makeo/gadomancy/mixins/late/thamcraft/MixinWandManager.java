package makeo.gadomancy.mixins.late.thamcraft;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import makeo.gadomancy.common.registration.RegisteredPotions;
import thaumcraft.common.items.wands.WandManager;

@Mixin(WandManager.class)
public class MixinWandManager {

    @ModifyReturnValue(method = "getTotalVisDiscount", at = @At(value = "RETURN", ordinal = 1), remap = false)
    private static float gadomancy$modifyVisDiscount(float original, EntityPlayer player) {
        if (player.isPotionActive(RegisteredPotions.VIS_DISCOUNT)) {
            original += (player.getActivePotionEffect(RegisteredPotions.VIS_DISCOUNT).getAmplifier() + 1) * 0.08F;
        }
        return original;
    }

}
