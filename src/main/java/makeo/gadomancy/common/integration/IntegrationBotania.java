package makeo.gadomancy.common.integration;

import net.minecraft.block.Block;

import makeo.gadomancy.common.blocks.tiles.TileKnowledgeBook;

public class IntegrationBotania extends IntegrationMod {

    @Override
    public String getModId() {
        return "Botania";
    }

    @Override
    protected void doInit() {
        // Handle pylons enchanting power
        Block pylon = Block.getBlockFromName("Botania:pylon");
        if (pylon != null) {
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(pylon, 0), 15);
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(pylon, 1), 15);
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(pylon, 2), 15);
        }
    }
}
