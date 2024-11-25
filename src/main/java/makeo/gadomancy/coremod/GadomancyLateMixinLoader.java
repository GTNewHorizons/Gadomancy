package makeo.gadomancy.coremod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class GadomancyLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gadomancy.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        if (FMLLaunchHandler.side().isClient()) {
            mixins.add("thamcraft.AccessorGuiResearchRecipe");
            mixins.add("thamcraft.MixinRenderEventHandler");
            mixins.add("thamcraft.MixinTileNodeRenderer");
        }
        mixins.add("thamcraft.MixinWandManager");
        return mixins;
    }

}
