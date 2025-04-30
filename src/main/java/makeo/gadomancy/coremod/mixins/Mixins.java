package makeo.gadomancy.coremod.mixins;

import static com.gtnewhorizon.gtnhlib.mixin.TargetedMod.THAUMCRAFT;

import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhlib.mixin.ITargetedMod;
import com.gtnewhorizon.gtnhlib.mixin.MixinBuilder;
import com.gtnewhorizon.gtnhlib.mixin.Phase;
import com.gtnewhorizon.gtnhlib.mixin.Side;
import java.util.List;
import java.util.function.Supplier;

public enum Mixins implements IMixins {
    // Config for this one is in the mixin itself, as it may not be present at apply time
    ADD_ADDITIONAL_NODES(new MixinBuilder("Replaces Thaumcraft's blockAiry with more nodes")
            .setPhase(Phase.LATE).setSide(Side.BOTH).addTargetedMod(THAUMCRAFT)
            .addMixinClasses("thaumcraft.MixinConfigBlocks").setApplyIf(() -> true));

    private final List<String> mixinClasses;
    private final List<ITargetedMod> targetedMods;
    private final List<ITargetedMod> excludedMods;
    private final Supplier<Boolean> applyIf;
    private final Phase phase;
    private final Side side;

    Mixins(MixinBuilder builder) {
        mixinClasses = builder.mixinClasses;
        targetedMods = builder.targetedMods;
        excludedMods = builder.excludedMods;
        applyIf = builder.applyIf;
        phase = builder.phase;
        side = builder.side;

        if (mixinClasses.isEmpty()) throw new RuntimeException("No mixin class specified for Mixin : " + name());
        if (targetedMods.isEmpty()) throw new RuntimeException("No targeted mods specified for Mixin : " + name());
        if (applyIf == null) throw new RuntimeException("No ApplyIf function specified for Mixin : " + name());
        if (phase == null) throw new RuntimeException("No Phase specified for Mixin : " + name());
        if (side == null) throw new RuntimeException("No Side function specified for Mixin : " + name());
    }

    @Override
    public List<String> getMixinClasses() {
        return mixinClasses;
    }

    @Override
    public Supplier<Boolean> getApplyIf() {
        return applyIf;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    @Override
    public Side getSide() {
        return side;
    }

    @Override
    public List<ITargetedMod> getTargetedMods() {
        return targetedMods;
    }

    @Override
    public List<ITargetedMod> getExcludedMods() {
        return excludedMods;
    }
}
