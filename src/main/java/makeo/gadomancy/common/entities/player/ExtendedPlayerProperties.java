package makeo.gadomancy.common.entities.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import makeo.gadomancy.common.utils.NBTHelper;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 05.07.2015 12:39
 */
public class ExtendedPlayerProperties implements IExtendedEntityProperties {

    private static final int TRANSFORM_INDEX = 26;

    private static final ExtendedPlayerProperties DEFAULTS = new ExtendedPlayerProperties(null);

    private final EntityPlayer player;

    private boolean isTransformed;

    public ExtendedPlayerProperties(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound modData = NBTHelper.getPersistentData(compound);

        modData.setBoolean("isTransformed", this.isTransformed);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        NBTTagCompound modData = NBTHelper.getPersistentData(compound);

        this.isTransformed = NBTHelper
                .getBoolean(modData, "isTransformed", ExtendedPlayerProperties.DEFAULTS.isTransformed);
        this.player.getDataWatcher().updateObject(ExtendedPlayerProperties.TRANSFORM_INDEX, this.isTransformed);
    }

    @Override
    public void init(Entity entity, World world) {
        this.player.getDataWatcher().addObject(ExtendedPlayerProperties.TRANSFORM_INDEX, this.isTransformed);
    }

    public static boolean isTransformed(EntityPlayer player) {
        return false;
    }

    public void setTransformed(boolean isTransformed) {
        this.isTransformed = isTransformed;
    }
}
