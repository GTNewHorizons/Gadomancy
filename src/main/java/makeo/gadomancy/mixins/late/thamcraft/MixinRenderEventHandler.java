package makeo.gadomancy.mixins.late.thamcraft;

import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import makeo.gadomancy.common.events.EventHandlerRedirect;
import thaumcraft.client.lib.RenderEventHandler;

@Mixin(value = RenderEventHandler.class, remap = false)
public class MixinRenderEventHandler {

    @Inject(method = "blockHighlight", at = @At("HEAD"))
    private void gadomancy$pre(DrawBlockHighlightEvent event, CallbackInfo ci) {
        EventHandlerRedirect.addGoggles(event.player);
    }

    @Inject(method = "blockHighlight", at = @At("TAIL"))
    private void gadomancy$post(DrawBlockHighlightEvent event, CallbackInfo ci) {
        EventHandlerRedirect.removeGoggles(event.player);
    }

}
