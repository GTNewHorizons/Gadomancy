package makeo.gadomancy.coremod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class GadomancyLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gadomancy.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (loadedMods.contains("Thaumcraft")) {
            // Replaces Thaumcraft's blockAiry with more nodes
            // Config for this mixin is in the mixin itself
            // config is not loaded at apply time
            return Collections.singletonList("thaumcraft.MixinConfigBlocks");
        }
        return Collections.emptyList();
    }
}
