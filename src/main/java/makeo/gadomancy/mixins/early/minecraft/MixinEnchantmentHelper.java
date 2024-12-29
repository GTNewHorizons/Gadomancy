package makeo.gadomancy.mixins.early.minecraft;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import makeo.gadomancy.common.registration.RegisteredPotions;

@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

    @ModifyReturnValue(method = { "getFortuneModifier", "getLootingModifier" }, at = @At("RETURN"))
    private static int gadomancy$modifyFortuneLevel(int original, EntityLivingBase entity) {
        if (entity.isPotionActive(RegisteredPotions.POTION_LUCK)) {
            return original + (entity.getActivePotionEffect(RegisteredPotions.POTION_LUCK).getAmplifier() + 1);
        }
        return original;
    }

}
