package makeo.gadomancy.common.familiar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import com.github.bsideup.jabel.Desugar;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 28.12.2015 23:44
 */
public class FamiliarAugment {

    private static final String FORMAT_NAME = "familiar.augment.%s.name";

    // Main effects
    public static final FamiliarAugment SHOCK = new FamiliarAugment(
            "shock",
            new AspectList().add(Aspect.AIR, 3).add(Aspect.ENTROPY, 2));
    public static final FamiliarAugment POISON = new FamiliarAugment(
            "poison",
            new AspectList().add(Aspect.WATER, 3).add(Aspect.ENTROPY, 4)).addConflict(FamiliarAugment.SHOCK);
    public static final FamiliarAugment FIRE = new FamiliarAugment(
            "fire",
            new AspectList().add(Aspect.FIRE, 4).add(Aspect.ORDER, 2))
                    .addConflict(FamiliarAugment.SHOCK, FamiliarAugment.POISON);
    public static final FamiliarAugment WEAKNESS = new FamiliarAugment(
            "weakness",
            new AspectList().add(Aspect.ENTROPY, 3).add(Aspect.EARTH, 3))
                    .addConflict(FamiliarAugment.SHOCK, FamiliarAugment.POISON, FamiliarAugment.FIRE);

    // Side effects
    public static final FamiliarAugment DAMAGE_INCREASE = new FamiliarAugment(
            "damage",
            new AspectList().add(Aspect.FIRE, 3)).addCondition(
                    new PreconditionAny(
                            FamiliarAugment.SHOCK,
                            FamiliarAugment.POISON,
                            FamiliarAugment.FIRE,
                            FamiliarAugment.WEAKNESS));
    public static final FamiliarAugment RANGE_INCREASE = new FamiliarAugment(
            "range",
            new AspectList().add(Aspect.AIR, 2).add(Aspect.ORDER, 1)).addConflict(FamiliarAugment.DAMAGE_INCREASE)
                    .addCondition(
                            new PreconditionAny(
                                    FamiliarAugment.SHOCK,
                                    FamiliarAugment.POISON,
                                    FamiliarAugment.FIRE,
                                    FamiliarAugment.WEAKNESS));
    public static final FamiliarAugment ATTACK_SPEED = new FamiliarAugment(
            "speed",
            new AspectList().add(Aspect.ORDER, 2).add(Aspect.FIRE, 1))
                    .addConflict(FamiliarAugment.DAMAGE_INCREASE, FamiliarAugment.RANGE_INCREASE).addCondition(
                            new PreconditionAny(
                                    FamiliarAugment.SHOCK,
                                    FamiliarAugment.POISON,
                                    FamiliarAugment.FIRE,
                                    FamiliarAugment.WEAKNESS));

    private final String unlocalizedName;
    private final List<FamiliarAugmentPrecondition> preconditions = new ArrayList<FamiliarAugmentPrecondition>();
    private boolean requiresPrevLevel = true;
    private final AspectList costsPerLevel;
    private final List<FamiliarAugment> conflicts = new ArrayList<FamiliarAugment>();

    private static final Map<String, FamiliarAugment> BY_NAME = new HashMap<String, FamiliarAugment>();

    private FamiliarAugment(String unlocalizedName, AspectList costs) {
        this.unlocalizedName = unlocalizedName;
        this.costsPerLevel = costs;
    }

    private FamiliarAugment setIgnorePreviousLevel() {
        this.requiresPrevLevel = false;
        return this;
    }

    private FamiliarAugment addCondition(FamiliarAugmentPrecondition condition) {
        this.preconditions.add(condition);
        return this;
    }

    private FamiliarAugment addConflict(FamiliarAugment... others) {
        for (FamiliarAugment augment : others) {
            if (augment == null) continue;
            this.addConflict(augment);
        }
        return this;
    }

    private FamiliarAugment addConflict(FamiliarAugment other) {
        if (!this.conflicts.contains(other)) this.conflicts.add(other);
        if (!other.conflicts.contains(this)) other.conflicts.add(this);
        return this;
    }

    public AspectList getCostsPerLevel() {
        return this.costsPerLevel;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public String getLocalizedName() {
        return StatCollector.translateToLocal(String.format(FamiliarAugment.FORMAT_NAME, this.unlocalizedName));
    }

    public static FamiliarAugment getByUnlocalizedName(String name) {
        return FamiliarAugment.BY_NAME.get(name.toLowerCase());
    }

    // Returns true, if the current conditions for application are fulfilled for a familiar with given augments.
    public boolean checkConditions(FamiliarAugmentList currentAugments, int levelToSet) {
        if (levelToSet <= 0) return false;
        for (FamiliarAugmentPair current : currentAugments) {
            if (this.conflicts.contains(current.augment)) return false;
        }
        if (this.requiresPrevLevel) {
            boolean containsAtAll = false;
            int foundLevel = -1;
            for (FamiliarAugmentPair pair : currentAugments) {
                if (pair.augment.equals(FamiliarAugment.this)) {
                    containsAtAll = true;
                    foundLevel = pair.level;
                }
            }
            if (!containsAtAll) {
                if (levelToSet != 1) return false;
            } else {
                if (foundLevel != (levelToSet - 1)) return false;
            }
        }
        for (FamiliarAugmentPrecondition precondition : this.preconditions) {
            if (precondition == null) continue;
            if (!precondition.isFulfilled(currentAugments, levelToSet)) return false;
        }
        return true;
    }

    static {
        FamiliarAugment.addNameEntry(FamiliarAugment.SHOCK, FamiliarAugment.SHOCK.unlocalizedName);
        FamiliarAugment.addNameEntry(FamiliarAugment.FIRE, FamiliarAugment.FIRE.unlocalizedName);
        FamiliarAugment.addNameEntry(FamiliarAugment.POISON, FamiliarAugment.POISON.unlocalizedName);
        FamiliarAugment.addNameEntry(FamiliarAugment.WEAKNESS, FamiliarAugment.WEAKNESS.unlocalizedName);

        FamiliarAugment.addNameEntry(FamiliarAugment.DAMAGE_INCREASE, FamiliarAugment.DAMAGE_INCREASE.unlocalizedName);
        FamiliarAugment.addNameEntry(FamiliarAugment.RANGE_INCREASE, FamiliarAugment.RANGE_INCREASE.unlocalizedName);
        FamiliarAugment.addNameEntry(FamiliarAugment.ATTACK_SPEED, FamiliarAugment.ATTACK_SPEED.unlocalizedName);
    }

    private static void addNameEntry(FamiliarAugment augment, String name) {
        FamiliarAugment.BY_NAME.put(name.toLowerCase(), augment);
    }

    private abstract static class FamiliarAugmentPrecondition {

        public abstract boolean isFulfilled(FamiliarAugmentList currentAugments, int levelToSet);
    }

    public static class PreconditionAny extends FamiliarAugmentPrecondition {

        private final FamiliarAugment[] anyPrevAugment;

        private PreconditionAny(FamiliarAugment... any) {
            this.anyPrevAugment = any;
        }

        @Override
        public boolean isFulfilled(FamiliarAugmentList currentAugments, int levelToSet) {
            if (this.anyPrevAugment == null) return true;
            for (FamiliarAugment augment : this.anyPrevAugment) {
                if (augment == null) continue;
                if (currentAugments.contains(augment)) return true;
            }
            return false;
        }
    }

    public static class PreconditionAll extends FamiliarAugmentPrecondition {

        private final FamiliarAugment[] prevAugments;

        private PreconditionAll(FamiliarAugment... previous) {
            this.prevAugments = previous;
        }

        @Override
        public boolean isFulfilled(FamiliarAugmentList currentAugments, int levelToSet) {
            if (this.prevAugments == null) return true;
            for (FamiliarAugment augment : this.prevAugments) {
                if (augment == null) continue;
                if (!currentAugments.contains(augment)) return false;
            }
            return true;
        }
    }

    public static class FamiliarAugmentList extends ArrayList<FamiliarAugmentPair> {

        public boolean contains(FamiliarAugment augment) {
            return this.getAugmentPair(augment) != null;
        }

        public FamiliarAugmentPair getAugmentPair(FamiliarAugment augment) {
            for (FamiliarAugmentPair pair : this) {
                if (pair.augment.equals(augment)) return pair;
            }
            return null;
        }

        public int getLevel(FamiliarAugment augment) {
            if (!this.contains(augment)) return -1;
            return this.getAugmentPair(augment).level;
        }
    }

    @Desugar
    public record FamiliarAugmentPair(FamiliarAugment augment, int level) {}
}
