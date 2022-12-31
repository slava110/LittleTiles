package team.creative.littletiles.common.gui.tool.recipe;

import java.util.Collections;
import java.util.Map.Entry;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiChildControl;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.VAlign;
import team.creative.creativecore.common.gui.controls.collection.GuiComboBoxMapped;
import team.creative.creativecore.common.gui.controls.parent.GuiLeftRightBox;
import team.creative.creativecore.common.gui.controls.simple.GuiButton;
import team.creative.creativecore.common.gui.controls.simple.GuiIconButton;
import team.creative.creativecore.common.gui.controls.tree.GuiTree;
import team.creative.creativecore.common.gui.controls.tree.GuiTreeItem;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.flow.GuiSizeRule.GuiSizeRatioRules;
import team.creative.creativecore.common.gui.flow.GuiSizeRule.GuiSizeRules;
import team.creative.creativecore.common.gui.style.GuiIcon;
import team.creative.creativecore.common.gui.sync.GuiSyncLocal;
import team.creative.creativecore.common.util.inventory.ContainerSlotView;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.littletiles.common.animation.preview.AnimationPreview;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.gui.controls.GuiAnimationViewer;
import team.creative.littletiles.common.gui.controls.IAnimationControl;
import team.creative.littletiles.common.gui.tool.GuiConfigure;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.registry.gui.LittleStructureGui;
import team.creative.littletiles.common.structure.registry.gui.LittleStructureGuiRegistry;

public class GuiRecipe extends GuiConfigure implements IAnimationControl {
    
    public final GuiSyncLocal<StringTag> CLEAR_CONTENT = getSyncHolder().register("clear_content", tag -> {
        //GuiCreator.openGui("recipeadvanced", new CompoundTag(), getPlayer());
    });
    
    private GuiTreeItemStructure loaded;
    private GuiTree tree;
    
    public GuiRecipe(ContainerSlotView view) {
        super("recipe", view);
        flow = GuiFlow.STACK_X;
        valign = VAlign.STRETCH;
        setDim(new GuiSizeRules().minWidth(500).minHeight(300));
    }
    
    @Override
    public void onLoaded(AnimationPreview preview) {
        callOnLoaded(preview, this);
    }
    
    private void callOnLoaded(AnimationPreview preview, Iterable<GuiChildControl> controls) {
        for (GuiChildControl child : controls) {
            if (child.control instanceof IAnimationControl a)
                a.onLoaded(preview);
            if (child.control instanceof GuiParent p)
                callOnLoaded(preview, p);
        }
    }
    
    @Override
    public CompoundTag saveConfiguration(CompoundTag nbt) {
        return null;
    }
    
    private void buildStructureTree(GuiTree tree, GuiTreeItem parent, LittleGroup group, int index) {
        if (group.isEmpty()) {
            if (!group.children.hasChildren())
                return;
            for (LittleGroup child : group.children.children()) {
                buildStructureTree(tree, parent, child, index);
                index++;
            }
            return;
        }
        
        String name = group.getStructureName();
        boolean hasStructureName = true;
        if (name == null) {
            hasStructureName = false;
            LittleStructureType type = group.getStructureType();
            if (type != null)
                name = type.id + " " + index;
            else
                name = "none " + index;
        }
        LittleGroup copy = new LittleGroup(group.hasStructure() ? group.getStructureTag().copy() : null, group.getGrid(), Collections.EMPTY_LIST);
        for (LittleTile tile : group)
            copy.addDirectly(tile.copy());
        for (Entry<String, LittleGroup> extension : group.children.extensionEntries())
            copy.children.addExtension(extension.getKey(), extension.getValue().copy());
        GuiTreeItemStructure item = new GuiTreeItemStructure(name, this, tree, copy);
        parent.addItem(item.setTitle(Component.literal(hasStructureName ? name : (ChatFormatting.ITALIC + "" + name))));
        
        int i = 0;
        for (LittleGroup child : group.children.children()) {
            buildStructureTree(tree, item, child, i);
            i++;
        }
    }
    
    @Override
    public void create() {
        if (!isClient())
            return;
        
        // Load recipe content
        LittleGroup group = LittleGroup.load(tool.get().getOrCreateTagElement("content"));
        
        GuiParent topBottom = new GuiParent(GuiFlow.STACK_Y).setAlign(Align.STRETCH);
        add(topBottom.setExpandable());
        
        GuiParent top = new GuiParent(GuiFlow.STACK_X);
        topBottom.add(top.setExpandableY());
        
        tree = new GuiTree("overview", false).setRootVisibility(false);
        buildStructureTree(tree, tree.root(), group, 0);
        tree.root().setTitle(Component.literal("root"));
        tree.updateTree();
        
        top.add(tree.setDim(new GuiSizeRatioRules().widthRatio(0.2F).maxWidth(100)).setExpandableY());
        
        GuiParent topCenter = new GuiParent(GuiFlow.STACK_Y).setAlign(Align.STRETCH);
        top.add(topCenter.setDim(new GuiSizeRatioRules().widthRatio(0.4F).maxWidth(300)).setExpandableY());
        
        // Actual recipe configuration
        GuiComboBoxMapped<LittleStructureGui> types = new GuiComboBoxMapped<>("type", new TextMapBuilder<LittleStructureGui>()
                .addComponent(LittleStructureGuiRegistry.registered(), x -> x.translatable()));
        topCenter.add(types);
        
        top.add(new GuiAnimationViewer("viewer").setExpandable());
        
        GuiParent bottom = new GuiParent(GuiFlow.STACK_X).setVAlign(VAlign.STRETCH);
        topBottom.add(bottom);
        
        GuiParent leftBottom = new GuiParent(GuiFlow.STACK_X).setAlign(Align.CENTER);
        bottom.add(leftBottom.setDim(new GuiSizeRatioRules().widthRatio(0.2F).maxWidth(100)));
        leftBottom.add(new GuiIconButton("up", GuiIcon.ARROW_UP, x -> tree.moveUp()));
        leftBottom.add(new GuiIconButton("down", GuiIcon.ARROW_DOWN, x -> tree.moveDown()));
        
        GuiLeftRightBox rightBottom = new GuiLeftRightBox();
        bottom.add(rightBottom.setVAlign(VAlign.CENTER).setExpandableX());
        rightBottom.addLeft(new GuiButton("cancel", x -> {}).setTranslate("gui.cancel"));
        rightBottom.addLeft(new GuiButton("clear", x -> {}).setTranslate("gui.recipe.clear"));
        rightBottom.addLeft(new GuiButton("selection", x -> {}).setTranslate("gui.recipe.selection"));
        rightBottom.addRight(new GuiButton("check", x -> {}).setTranslate("gui.check"));
        rightBottom.addRight(new GuiButton("save", x -> {}).setTranslate("gui.save"));
    }
    
    public void load(GuiTreeItemStructure item) {
        this.loaded = item;
        this.tree.select(loaded);
        AnimationPreview preview = item.getAnimationPreview();
        if (preview != null)
            onLoaded(preview);
    }
    
}