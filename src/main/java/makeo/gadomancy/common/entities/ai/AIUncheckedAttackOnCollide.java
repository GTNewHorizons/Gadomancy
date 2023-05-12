package makeo.gadomancy.common.entities.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.PathEntity;

import makeo.gadomancy.common.utils.Injector;
import thaumcraft.common.entities.ai.combat.AIGolemAttackOnCollide;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 31.10.2015 19:11
 */
public class AIUncheckedAttackOnCollide extends AIGolemAttackOnCollide {

    private static final Injector INJECTOR = new Injector(AIGolemAttackOnCollide.class);
    private EntityGolemBase golem;

    public AIUncheckedAttackOnCollide(EntityGolemBase golem) {
        super(golem);
        this.golem = golem;
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = this.golem.getAttackTarget();
        if (target == null) {
            return false;
        }

        PathEntity pathEntity = this.golem.getNavigator().getPathToEntityLiving(target);
        if (pathEntity != null) {
            AIUncheckedAttackOnCollide.INJECTOR.setObject(this);
            AIUncheckedAttackOnCollide.INJECTOR.setField("entityPathEntity", pathEntity);
            AIUncheckedAttackOnCollide.INJECTOR.setField("entityTarget", target);
            return true;
        }

        return false;
    }
}
