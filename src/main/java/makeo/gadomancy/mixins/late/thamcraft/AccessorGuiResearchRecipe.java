package makeo.gadomancy.mixins.late.thamcraft;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.lib.TCFontRenderer;

@Mixin(value = GuiResearchRecipe.class, remap = false)
public interface AccessorGuiResearchRecipe {

    @Accessor
    ResearchItem getResearch();

    @Accessor
    ResearchPage[] getPages();

    @Accessor
    int getPage();

    @Accessor
    double getGuiMapX();

    @Accessor
    double getGuiMapY();

    @Accessor
    void setPages(ResearchPage[] pages);

    @Accessor
    int getMaxPages();

    @Accessor
    void setMaxPages(int maxPages);

    @Accessor(value = "fr")
    TCFontRenderer getFontRenderer();

    @Accessor
    ArrayList<List> getReference();

    @Accessor
    void setReference(ArrayList<List> ref);

    @Accessor
    void setTooltip(Object[] tooltip);

    @Invoker()
    void callDrawPage(ResearchPage pageParm, int side, int x, int y, int mx, int my);

}
