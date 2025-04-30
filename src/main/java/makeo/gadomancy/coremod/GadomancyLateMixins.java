package makeo.gadomancy.coremod;

import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import java.util.List;
import java.util.Set;
import makeo.gadomancy.coremod.mixins.Mixins;

@LateMixin
public class GadomancyLateMixins implements ILateMixinLoader {
    @Override
    public String getMixinConfig() {
        return "mixins.gadomancy.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
