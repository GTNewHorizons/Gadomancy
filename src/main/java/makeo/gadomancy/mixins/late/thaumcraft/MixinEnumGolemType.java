package makeo.gadomancy.mixins.late.thaumcraft;

import java.util.ArrayList;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import thaumcraft.common.entities.golems.EnumGolemType;

@Mixin(EnumGolemType.class)
public class MixinEnumGolemType {

    @Unique
    private static final EnumGolemType SILVERWOOD_GOLEM = gadomancy$expandGolemEnum(
            "SILVERWOOD",
            20,
            9,
            0.38f,
            false,
            3,
            8,
            75,
            1);

    @Shadow(remap = false)
    @Final
    @Mutable
    private static EnumGolemType[] $VALUES;

    @Invoker(value = "<init>", remap = false)
    private static EnumGolemType gadomancy$InitInvoker(String name, int ordinal, int health, int armor, float speed,
            boolean fireResist, int upgrades, int carry, int regenDelay, int strength) {
        throw new IllegalStateException("Mixin stub invoked");
    }

    @Unique
    private static EnumGolemType gadomancy$expandGolemEnum(String name, int health, int armor, float speed,
            boolean fireResist, int upgrades, int carry, int regenDelay, int strength) {
        assert $VALUES != null;
        ArrayList<EnumGolemType> values = new ArrayList<>(Arrays.asList($VALUES));
        EnumGolemType golem = gadomancy$InitInvoker(
                name,
                values.get(values.size() - 1).ordinal() + 1,
                health,
                armor,
                speed,
                fireResist,
                upgrades,
                carry,
                regenDelay,
                strength);
        values.add(golem);
        $VALUES = values.toArray(new EnumGolemType[0]);
        return golem;
    }

}
