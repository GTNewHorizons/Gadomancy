package makeo.gadomancy.api;

import java.util.List;

import net.minecraft.item.ItemStack;

import makeo.gadomancy.api.golems.AdditionalGolemType;
import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.api.internal.IApiHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 26.07.2015 19:00
 */
public abstract class GadomancyApi {

    private static IApiHandler handler;

    public static void setInstance(IApiHandler handler) {
        GadomancyApi.handler = handler;
    }

    @Deprecated
    public static boolean registerAdditionalGolemType(String name, String modId, AdditionalGolemType newType) {
        return GadomancyApi.handler.registerAdditionalGolemType(name, modId, newType);
    }

    public static AdditionalGolemType getAdditionalGolemType(String name) {
        return GadomancyApi.handler.getAdditionalGolemType(name);
    }

    public static AdditionalGolemType getAdditionalGolemType(EnumGolemType type) {
        return GadomancyApi.getAdditionalGolemType(type.name());
    }

    public static List<AdditionalGolemType> getAdditionalGolemTypes() {
        return GadomancyApi.handler.getAdditionalGolemTypes();
    }

    public static boolean isAdditionalGolemType(String name) {
        return GadomancyApi.getAdditionalGolemType(name) != null;
    }

    public static boolean isAdditionalGolemType(EnumGolemType type) {
        return GadomancyApi.isAdditionalGolemType(type.name());
    }

    public static AdditionalGolemCore getAdditionalGolemCore(EntityGolemBase golem) {
        return GadomancyApi.handler.getAdditionalGolemCore(golem);
    }

    public static AdditionalGolemCore getAdditionalGolemCore(ItemStack placer) {
        return GadomancyApi.handler.getAdditionalGolemCore(placer);
    }

    public static void setAdditionalGolemCore(EntityGolemBase golem, AdditionalGolemCore core) {
        GadomancyApi.handler.setAdditionalGolemCore(golem, core);
    }

    public static boolean registerAdditionalGolemCore(String name, AdditionalGolemCore core) {
        return GadomancyApi.handler.registerAdditionalGolemCore(name, core);
    }

    public static void registerAdditionalAuraEffect(Aspect aspect, AuraEffect effect) {
        GadomancyApi.handler.registerAdditionalAuraEffect(aspect, effect);
    }

    public static List<AdditionalGolemCore> getAdditionalGolemCores() {
        return GadomancyApi.handler.getAdditionalGolemCores();
    }

    public static void registerClawClickBehavior(ClickBehavior clickBehavior) {
        GadomancyApi.handler.registerClawClickBehavior(clickBehavior);
    }
}
