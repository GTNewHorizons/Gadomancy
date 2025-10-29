package makeo.gadomancy.coremod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class GadomancyLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gadomancy.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        if (loadedMods.contains("Thaumcraft")) {
            if (FMLLaunchHandler.side().isClient()) {
                mixins.add("thaumcraft.MixinRenderEventHandler");
                mixins.add("thaumcraft.MixinTileNodeRenderer");
            }
            mixins.add("thaumcraft.MixinConfigBlocks");
        }
        return mixins;
    }
}
