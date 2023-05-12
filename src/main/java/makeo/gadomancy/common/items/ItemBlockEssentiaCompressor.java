package makeo.gadomancy.common.items;

import net.minecraft.block.Block;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import makeo.gadomancy.common.registration.RegisteredItems;

/**
 * HellFirePvP@Admin Date: 22.04.2016 / 21:58 on Gadomancy ItemBlockEssentiaCompressor
 */
public class ItemBlockEssentiaCompressor extends ItemBlock {

    public ItemBlockEssentiaCompressor(Block block) {
        super(block);
        this.setMaxDamage(0);
        this.setCreativeTab(RegisteredItems.creativeTab);
    }

    @Override
    public EnumRarity getRarity(ItemStack p_77613_1_) {
        return RegisteredItems.raritySacred;
    }
}
