package makeo.gadomancy.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import makeo.gadomancy.common.blocks.BlockNode;
import makeo.gadomancy.common.data.config.ModConfig;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.config.ConfigBlocks;

@SuppressWarnings("UnusedMixin")
@Mixin(value = ConfigBlocks.class, remap = false)
public class MixinConfigBlocks {

    @Redirect(method = "initializeBlocks", at = @At(value = "NEW", target = "()Lthaumcraft/common/blocks/BlockAiry;"))
    private static BlockAiry gadomancy$replaceBlockAiry() {
        // Replaces Thaumcraft's blockAiry with more nodes
        return ModConfig.enableAdditionalNodeTypes ? new BlockNode() : new BlockAiry();
    }
}
