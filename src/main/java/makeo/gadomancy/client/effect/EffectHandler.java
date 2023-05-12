package makeo.gadomancy.client.effect;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import makeo.gadomancy.client.effect.fx.FXFlow;
import makeo.gadomancy.client.effect.fx.FXVortex;
import makeo.gadomancy.client.effect.fx.Orbital;
import makeo.gadomancy.common.utils.Vector3;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by HellFirePvP @ 17.11.2015 18:39
 */
public class EffectHandler {

    public static final EffectHandler instance = new EffectHandler();

    private static final CheckpointingSet<Orbital> orbitals = new CheckpointingSet<>();
    private static final CheckpointingSet<FXFlow> fxFlows = new CheckpointingSet<>();
    private static final CheckpointingSet<FXVortex> fxVortexes = new CheckpointingSet<>();

    public static EffectHandler getInstance() {
        return EffectHandler.instance;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        Tessellator tessellator = Tessellator.instance;

        FXFlow.FXFlowBase.sheduleRender(tessellator);
        Orbital.sheduleRenders(EffectHandler.orbitals, event.partialTicks);
        FXVortex.sheduleRender(EffectHandler.fxVortexes, tessellator, event.partialTicks);

        orbitals.update();
        fxFlows.update();
        fxVortexes.update();
    }

    public FXFlow effectFlow(World world, Vector3 origin, FXFlow.EntityFlowProperties properties) {
        FXFlow flow = new FXFlow(world);
        flow.applyProperties(properties).setPosition(origin);
        this.registerFlow(flow);
        return flow;
    }

    public void registerVortex(final FXVortex vortex) {
        EffectHandler.fxVortexes.add(vortex);
        vortex.registered = true;
    }

    public void unregisterVortex(final FXVortex vortex) {
        EffectHandler.fxVortexes.remove(vortex);
        vortex.registered = false;
    }

    public void registerFlow(final FXFlow flow) {
        EffectHandler.fxFlows.add(flow);
    }

    public void unregisterFlow(final FXFlow flow) {
        EffectHandler.fxFlows.remove(flow);
    }

    public void registerOrbital(final Orbital orbital) {
        EffectHandler.orbitals.add(orbital);
        orbital.registered = true;
    }

    public void unregisterOrbital(final Orbital orbital) {
        EffectHandler.orbitals.remove(orbital);
        orbital.registered = false;
    }

    public void tick() {
        Orbital.tickOrbitals(EffectHandler.orbitals);
        FXFlow.tickFlows(EffectHandler.fxFlows);
        FXVortex.tickVortexes(EffectHandler.fxVortexes);
    }

    public void clear() {
        EffectHandler.orbitals.clear();
        EffectHandler.fxFlows.clear();
        EffectHandler.fxVortexes.clear();
    }
}
