package makeo.gadomancy.common.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;

import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.registration.RegisteredManipulations;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 28.10.2015 11:43
 */
public class NodeManipulatorResultHandler {

    private static List<NodeManipulatorResult> possibleResults = new ArrayList<NodeManipulatorResult>();

    private NodeManipulatorResultHandler() {}

    public static NodeManipulatorResult getRandomResult(World world, INode affectedNode, int percChance) {
        return NodeManipulatorResultHandler.getRandomResult(world, world.rand, affectedNode, percChance);
    }

    public static NodeManipulatorResult getRandomResult(World world, Random random, INode affectedNode,
            int percChance) {
        int resultPositiveChance = Math.round(((float) percChance) / 5F);
        List<NodeManipulatorResult> localResults = new ArrayList<NodeManipulatorResult>();
        for (NodeManipulatorResult result : NodeManipulatorResultHandler.possibleResults) {
            if (result.canAffect(world, affectedNode)) {
                ResultType type = result.getResultType();
                if (type == ResultType.NEGATIVE) {
                    if (random.nextInt(100) < resultPositiveChance) continue;
                }

                localResults.add(result);
            }
        }
        if (localResults.isEmpty()) return null;
        return (NodeManipulatorResult) WeightedRandom.getRandomItem(random, localResults);
    }

    public static void combine(AspectList containingList, Aspect a, Aspect b, int addition) {
        if (!NodeManipulatorResultHandler.canCombine(a, b)) return;
        Aspect combination = NodeManipulatorResultHandler.getCombination(a, b);
        int lowerAmount;
        if (containingList.getAmount(a) < containingList.getAmount(b)) {
            lowerAmount = containingList.getAmount(a);
        } else {
            lowerAmount = containingList.getAmount(b);
        }
        containingList.remove(a, lowerAmount);
        containingList.remove(b, lowerAmount);
        containingList.add(combination, lowerAmount + addition);
    }

    public static boolean canCombine(Aspect a, Aspect b) {
        return NodeManipulatorResultHandler.getCombination(a, b) != null;
    }

    public static Aspect getCombination(Aspect a, Aspect b) {
        for (Aspect aspect : Aspect.getCompoundAspects()) {
            Aspect[] components = aspect.getComponents();
            if ((components[0] == a && components[1] == b) || components[0] == b && components[1] == a) {
                return aspect;
            }
        }
        return null;
    }

    static {
        // Aspect combination pair
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultBreakCompounds);
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultCombineAspects);

        // Modifier pair
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultIncreaseModifier);
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultDecreaseModifier);

        // Switch node Type
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultSwitchType);

        if (ModConfig.enableAdditionalNodeTypes) {
            // Almighty Growing
            NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultApplyGrowing);
        }

        // Let the node gain primals
        NodeManipulatorResultHandler.possibleResults.add(RegisteredManipulations.resultGainPrimal);
    }

    public enum ResultType {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
