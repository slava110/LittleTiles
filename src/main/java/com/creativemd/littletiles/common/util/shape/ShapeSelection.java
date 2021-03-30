package com.creativemd.littletiles.common.util.shape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.creativemd.creativecore.common.gui.container.SubGui;
import com.creativemd.creativecore.common.utils.math.Rotation;
import com.creativemd.creativecore.common.utils.math.VectorUtils;
import com.creativemd.creativecore.common.utils.mc.TickUtils;
import com.creativemd.littletiles.client.gui.SubGuiMarkShapeSelection;
import com.creativemd.littletiles.common.api.ILittleTool;
import com.creativemd.littletiles.common.block.BlockTile;
import com.creativemd.littletiles.common.tile.math.box.LittleBox;
import com.creativemd.littletiles.common.tile.math.box.LittleBoxes;
import com.creativemd.littletiles.common.tile.math.vec.LittleVec;
import com.creativemd.littletiles.common.util.grid.IGridBased;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.creativemd.littletiles.common.util.place.IMarkMode;
import com.creativemd.littletiles.common.util.place.PlacementPosition;
import com.creativemd.littletiles.common.util.shape.ShapeSelection.ShapeSelectPos;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ShapeSelection implements Iterable<ShapeSelectPos>, IGridBased, IMarkMode {
    
    public final ItemStack stack;
    public final ILittleTool tool;
    public final NBTTagCompound nbt;
    private final List<ShapeSelectPos> positions = new ArrayList<>();
    protected final LittleShape shape;
    public final boolean inside;
    
    private ShapeSelectPos last;
    
    protected LittleBoxes cachedBoxesLowRes;
    protected LittleBoxes cachedBoxes;
    
    protected BlockPos pos;
    protected LittleGridContext context = LittleGridContext.getMin();
    protected LittleBox overallBox;
    
    private boolean marked;
    private int markedPosition;
    public boolean allowLowResolution = true;
    
    public ShapeSelection(ItemStack stack, boolean inside) {
        this.inside = inside;
        this.nbt = stack.getTagCompound();
        this.tool = (ILittleTool) stack.getItem();
        this.stack = stack;
        this.shape = ShapeRegistry.getShape(nbt.getString("shape"));
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    @Override
    public LittleGridContext getContext() {
        return context;
    }
    
    public LittleBox getOverallBox() {
        return overallBox;
    }
    
    public LittleBoxes getBoxes(boolean allowLowResolution) {
        if (marked) {
            if (this.allowLowResolution)
                return cachedBoxesLowRes;
        } else if (allowLowResolution)
            return cachedBoxesLowRes;
        if (cachedBoxes == null)
            forceHighResCache();
        return cachedBoxes;
    }
    
    private void forceHighResCache() {
        cachedBoxes = shape.getBoxes(this, false);
    }
    
    private void rebuildShapeCache() {
        LittleGridContext context = tool.getPositionContext(stack);
        convertToAtMinimum(context);
        
        cachedBoxes = null;
        cachedBoxesLowRes = null;
        
        LittleBox[] pointBoxes = new LittleBox[positions.size() + (marked ? 0 : 1)];
        int i = 0;
        for (ShapeSelectPos pos : this) {
            pointBoxes[i] = new LittleBox(pos.pos.getRelative(this.pos));
            i++;
        }
        
        overallBox = new LittleBox(pointBoxes);
        cachedBoxesLowRes = shape.getBoxes(this, true);
    }
    
    @SideOnly(Side.CLIENT)
    public boolean addAndCheckIfPlace(EntityPlayer player, PlacementPosition position, RayTraceResult result) {
        ShapeSelectPos pos = new ShapeSelectPos(player, position, result);
        if ((shape.pointsBeforePlacing < positions.size() + 1 || GuiScreen.isCtrlKeyDown()) && (shape.maxAllowed() == -1 || shape.maxAllowed() < positions.size() + 1)) {
            positions.add(pos);
            ensureSameContext(pos);
            rebuildShapeCache();
            return false;
        }
        last = pos;
        ensureSameContext(last);
        rebuildShapeCache();
        return true;
    }
    
    @SideOnly(Side.CLIENT)
    public void setLast(EntityPlayer player, PlacementPosition position, RayTraceResult result) {
        if (positions.isEmpty())
            pos = position.getPos();
        last = new ShapeSelectPos(player, position, result);
        ensureSameContext(last);
        rebuildShapeCache();
    }
    
    private void ensureSameContext(ShapeSelectPos pos) {
        if (context.size > pos.getContext().size)
            pos.convertTo(context);
        else if (context.size < pos.getContext().size)
            convertTo(pos.getContext());
    }
    
    public void toggleMark() {
        if (marked) {
            while (shape.maxAllowed() != -1 && positions.size() >= shape.maxAllowed())
                positions.remove(positions.size() - 1);
            marked = false;
        } else {
            markedPosition = positions.size();
            positions.add(last);
            marked = true;
        }
    }
    
    @Override
    public boolean allowLowResolution() {
        return allowLowResolution;
    }
    
    @Override
    public PlacementPosition getPosition() {
        return positions.get(markedPosition).pos.copy();
    }
    
    @Override
    public SubGui getConfigurationGui() {
        return new SubGuiMarkShapeSelection(this);
    }
    
    @Override
    public void move(LittleGridContext context, EnumFacing facing) {
        LittleVec vec = new LittleVec(facing);
        vec.scale(GuiScreen.isCtrlKeyDown() ? context.size : 1);
        positions.get(markedPosition).pos.subVec(vec);
    }
    
    @Override
    public void done() {
        toggleMark();
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void render(double x, double y, double z) {
        if (marked) {
            for (int i = 0; i < positions.size(); i++)
                positions.get(i).render(x, y, z, markedPosition == i);
            
        }
    }
    
    public void rotate(EntityPlayer player, ItemStack stack, Rotation rotation) {
        shape.rotate(nbt, rotation);
        rebuildShapeCache();
    }
    
    public void flip(EntityPlayer player, ItemStack stack, Axis axis) {
        shape.flip(nbt, axis);
        rebuildShapeCache();
    }
    
    @SideOnly(Side.CLIENT)
    public void click(EntityPlayer player) {
        int index = -1;
        double distance = Double.MAX_VALUE;
        float partialTickTime = TickUtils.getPartialTickTime();
        Vec3d pos = player.getPositionEyes(partialTickTime);
        double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
        Vec3d look = player.getLook(partialTickTime);
        Vec3d vec32 = pos.addVector(look.x * d0, look.y * d0, look.z * d0);
        for (int i = 0; i < positions.size(); i++) {
            RayTraceResult result = positions.get(i).box.calculateIntercept(pos, vec32);
            if (result != null) {
                double tempDistance = pos.squareDistanceTo(result.hitVec);
                if (tempDistance < distance) {
                    index = i;
                    distance = tempDistance;
                }
            }
        }
        if (index != -1)
            markedPosition = index;
    }
    
    @Override
    public Iterator<ShapeSelectPos> iterator() {
        if (marked)
            return positions.iterator();
        return new Iterator<ShapeSelection.ShapeSelectPos>() {
            
            private Iterator<ShapeSelectPos> iter = positions.iterator();
            private boolean last = false;
            
            @Override
            public boolean hasNext() {
                return iter.hasNext() || !last;
            }
            
            @Override
            public ShapeSelectPos next() {
                if (iter.hasNext())
                    return iter.next();
                else if (!last) {
                    last = false;
                    return ShapeSelection.this.last;
                }
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public ShapeSelectPos getFirst() {
        if (positions.size() > 0)
            return positions.get(0);
        return last;
    }
    
    public ShapeSelectPos getLast() {
        return last;
    }
    
    @Override
    public void convertTo(LittleGridContext to) {
        for (ShapeSelectPos other : positions)
            other.convertTo(to);
        context = to;
    }
    
    @Override
    public void convertToSmallest() {
        int smallest = LittleGridContext.getMin().size;
        for (int i = 0; i < positions.size(); i++)
            smallest = Math.max(smallest, positions.get(i).getSmallestContext());
        convertTo(LittleGridContext.get(smallest));
    }
    
    public class ShapeSelectPos implements IGridBased {
        
        public final PlacementPosition pos;
        public final RayTraceResult ray;
        public final BlockTile.TEResult result;
        public final AxisAlignedBB box;
        
        public ShapeSelectPos(EntityPlayer player, PlacementPosition position, RayTraceResult result) {
            this.pos = position;
            this.ray = result;
            this.result = BlockTile.loadTeAndTile(player.world, result.getBlockPos(), player);
            this.box = pos.getBox().grow(0.002);
            if (inside && result.sideHit.getAxisDirection() == AxisDirection.POSITIVE && context.isAtEdge(VectorUtils.get(result.sideHit.getAxis(), result.hitVec)))
                pos.getVec().sub(result.sideHit);
        }
        
        @SideOnly(Side.CLIENT)
        public void render(double x, double y, double z, boolean selected) {
            GlStateManager.enableBlend();
            GlStateManager
                .tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            AxisAlignedBB box = this.box.offset(-x, -y, -z);
            
            if (selected)
                GlStateManager.color(1, 0, 0);
            else
                GlStateManager.color(0, 0, 0);
            GlStateManager.glLineWidth(4.0F);
            RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 1F);
            
            GlStateManager.disableDepth();
            GlStateManager.glLineWidth(1.0F);
            RenderGlobal.drawSelectionBoundingBox(box, 1F, 0.3F, 0.0F, 1F);
            GlStateManager.enableDepth();
            
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
        
        @Override
        public LittleGridContext getContext() {
            return pos.getContext();
        }
        
        @Override
        public void convertTo(LittleGridContext to) {
            pos.convertTo(to);
        }
        
        @Override
        public void convertToSmallest() {
            pos.convertToSmallest();
        }
        
        public int getSmallestContext() {
            return pos.getSmallestContext();
        }
    }
    
}
