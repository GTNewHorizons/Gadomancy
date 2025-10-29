package makeo.gadomancy.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import makeo.gadomancy.common.events.EventHandlerRedirect;
import thaumcraft.client.renderers.tile.TileNodeRenderer;

@Mixin(value = TileNodeRenderer.class)
public class MixinTileNodeRenderer {

    @Inject(method = "renderTileEntityAt", at = @At("HEAD"))
    private void gadomancy$preRender(CallbackInfo ci) {
        EventHandlerRedirect.preNodeRender();
    }

    @Inject(method = "renderTileEntityAt", at = @At("RETURN"))
    private void gadomancy$postRender(CallbackInfo ci) {
        EventHandlerRedirect.postNodeRender();
    }

}
