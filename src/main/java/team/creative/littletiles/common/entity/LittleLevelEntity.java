package team.creative.littletiles.common.entity;

import java.util.Iterator;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.common.level.ISubLevel;
import team.creative.creativecore.common.util.math.collision.CollisionCoordinator;
import team.creative.creativecore.common.util.math.matrix.ChildVecOrigin;
import team.creative.creativecore.common.util.math.matrix.IVecOrigin;
import team.creative.creativecore.common.util.type.itr.FilterIterator;
import team.creative.littletiles.client.render.entity.LittleLevelRenderManager;
import team.creative.littletiles.common.entity.physic.LittleLevelEntityPhysic;
import team.creative.littletiles.common.item.ItemLittleWrench;
import team.creative.littletiles.common.level.handler.LittleAnimationHandlers;
import team.creative.littletiles.common.level.little.CreativeLevel;
import team.creative.littletiles.common.math.location.LocalStructureLocation;
import team.creative.littletiles.common.math.vec.LittleHitResult;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.connection.direct.StructureConnection;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.exception.NotYetConnectedException;
import team.creative.littletiles.common.structure.relative.StructureAbsolute;
import team.creative.littletiles.server.level.little.FakeServerLevel;
import team.creative.littletiles.server.level.little.SubServerLevel;

public abstract class LittleLevelEntity extends Entity implements OrientationAwareEntity, INoPushEntity {
    
    private Iterable<OrientationAwareEntity> childrenItr = new Iterable<OrientationAwareEntity>() {
        
        @Override
        public Iterator<OrientationAwareEntity> iterator() {
            return new FilterIterator<OrientationAwareEntity>(entities(), OrientationAwareEntity.class);
        }
        
    };
    
    private CreativeLevel fakeLevel;
    private StructureAbsolute center;
    private IVecOrigin origin;
    protected boolean hasOriginChanged = false;
    private StructureConnection structure;
    
    public final LittleLevelEntityPhysic physic = new LittleLevelEntityPhysic(this);
    
    public double initalOffX;
    public double initalOffY;
    public double initalOffZ;
    public double initalRotX;
    public double initalRotY;
    public double initalRotZ;
    
    @OnlyIn(Dist.CLIENT)
    public LittleLevelRenderManager renderManager;
    
    // ================Constructors================
    
    public LittleLevelEntity(EntityType<?> type, Level level) {
        super(type, level);
    }
    
    public LittleLevelEntity(EntityType<?> type, Level level, CreativeLevel fakeLevel, StructureAbsolute center, LocalStructureLocation location) {
        super(type, level);
        setFakeLevel(fakeLevel);
        setCenter(center);
        this.structure = new StructureConnection(fakeLevel, location);
        
        setPos(center.baseOffset.getX(), center.baseOffset.getY(), center.baseOffset.getZ());
        
        physic.ignoreCollision(() -> {
            initialTick();
            this.initalOffX = origin.offX();
            this.initalOffY = origin.offY();
            this.initalOffZ = origin.offZ();
            this.initalRotX = origin.rotX();
            this.initalRotY = origin.rotY();
            this.initalRotZ = origin.rotZ();
        });
        
        origin.tick();
        
        if (level.isClientSide)
            renderManager = new LittleLevelRenderManager(fakeLevel);
    }
    
    // ================Origin================
    
    @Override
    public void markOriginChange() {
        hasOriginChanged = true;
        for (OrientationAwareEntity child : children())
            child.markOriginChange();
    }
    
    public void resetOriginChange() {
        hasOriginChanged = false;
    }
    
    @Override
    protected void defineSynchedData() {}
    
    @Override
    public IVecOrigin getOrigin() {
        return origin;
    }
    
    public CreativeLevel getFakeLevel() {
        return fakeLevel;
    }
    
    public Level getRealLevel() {
        if (level instanceof ISubLevel)
            return ((ISubLevel) level).getRealLevel();
        return level;
    }
    
    public LittleLevelEntity getTopLevelEntity() {
        if (level instanceof ISubLevel)
            return ((LittleLevelEntity) ((ISubLevel) level).getHolder()).getTopLevelEntity();
        return this;
    }
    
    protected void setFakeLevel(CreativeLevel fakeLevel) {
        this.fakeLevel = fakeLevel;
        this.fakeLevel.setHolder(this);
        this.fakeLevel.registerLevelBoundListener(physic);
    }
    
    public StructureAbsolute getCenter() {
        return center;
    }
    
    public void setCenter(StructureAbsolute center) {
        this.center = center;
        this.fakeLevel.setOrigin(center.rotationCenter);
        this.origin = this.fakeLevel.getOrigin();
        for (OrientationAwareEntity entity : children())
            entity.parentVecOriginChange(origin);
    }
    
    @Override
    public void parentVecOriginChange(IVecOrigin origin) {
        ((ChildVecOrigin) origin).parent = origin;
    }
    
    public LittleStructure getStructure() throws CorruptedConnectionException, NotYetConnectedException {
        return structure.getStructure();
    }
    
    public void markRemoved() {
        // TODO THING ABOUT WHAT TO DO WITH THIS METHOD
    }
    
    @Override
    public void moveAndRotateAnimation(CollisionCoordinator coordinator) {
        physic.moveAndRotateAnimation(coordinator);
    }
    
    public AABB getRealBB() {
        if (level instanceof ISubLevel or)
            return or.getOrigin().getAxisAlignedBox(getBoundingBox());
        return getBoundingBox();
    }
    
    public Vec3 getRealCenter() {
        if (level instanceof ISubLevel or)
            return or.getOrigin().transformPointToWorld(position());
        return position();
    }
    
    // ================Children================
    
    public Iterable<Entity> entities() {
        return fakeLevel.entities();
    }
    
    public Iterable<OrientationAwareEntity> children() {
        return childrenItr;
    }
    
    // ================Rendering================
    
    /*@OnlyIn(Dist.CLIENT)
    public LittleRenderChunkSuppilier getRenderChunkSuppilier() {
        return (LittleRenderChunkSuppilier) ((CreativeClientLevel) fakeLevel).renderChunkSupplier;
    }*/
    
    // ================Ticking================
    
    public abstract void initialTick();
    
    public abstract void onTick();
    
    @Override
    public void performTick() {
        origin.tick();
        
        if (level instanceof ISubLevel) {
            if (!level.isClientSide)
                this.setSharedFlag(6, this.isCurrentlyGlowing());
            super.baseTick();
        } else
            super.tick();
        
        children().forEach(x -> x.performTick());
        onTick();
        
        physic.updateBoundingBox();
        fakeLevel.tickBlockEntities();
        
        setPosRaw(center.baseOffset.getX() + origin.offXLast(), center.baseOffset.getY() + origin.offYLast(), center.baseOffset.getZ() + origin.offZLast());
        setOldPosAndRot();
        setPosRaw(center.baseOffset.getX() + origin.offX(), center.baseOffset.getY() + origin.offY(), center.baseOffset.getZ() + origin.offZ());
    }
    
    // ================Save&Load================
    
    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        // TODO TAKE CARE OF EXISTING LOADED ENTITIES
        
        setFakeLevel(nbt.getBoolean("subworld") ? SubServerLevel.createSubLevel(level) : FakeServerLevel.createFakeLevel(getStringUUID(), level.isClientSide));
        
        this.initalOffX = nbt.getDouble("initOffX");
        this.initalOffY = nbt.getDouble("initOffY");
        this.initalOffZ = nbt.getDouble("initOffZ");
        this.initalRotX = nbt.getDouble("initRotX");
        this.initalRotY = nbt.getDouble("initRotY");
        this.initalRotZ = nbt.getDouble("initRotZ");
        
        fakeLevel.preventNeighborUpdate = true;
        
        setCenter(new StructureAbsolute("center", nbt));
        
        // TODO REWORK SAVING OF WORLD, SAVE BLOCKS AND ENTITIES
        
        this.structure = new StructureConnection(fakeLevel, nbt.getCompound("structure"));
        
        fakeLevel.preventNeighborUpdate = false;
        
        loadLevelEntity(nbt);
        
        physic.updateBoundingBox();
        if (level.isClientSide)
            renderManager = new LittleLevelRenderManager(fakeLevel);
    }
    
    public abstract void loadLevelEntity(CompoundTag nbt);
    
    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        center.save("center", nbt);
        
        nbt.putDouble("initOffX", initalOffX);
        nbt.putDouble("initOffY", initalOffY);
        nbt.putDouble("initOffZ", initalOffZ);
        nbt.putDouble("initRotX", initalRotX);
        nbt.putDouble("initRotY", initalRotY);
        nbt.putDouble("initRotZ", initalRotZ);
        
        nbt.putBoolean("subworld", fakeLevel instanceof ISubLevel);
        
        // TODO REWORK LOADING OF WORLD, LOAD BLOCKS AND ENTITIES
        
        nbt.put("structure", structure.write());
        
        saveLevelEntity(nbt);
    }
    
    public abstract void saveLevelEntity(CompoundTag nbt);
    
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        LittleAnimationHandlers.get(level).add(this);
    }
    
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        LittleAnimationHandlers.get(level).remove(this);
    }
    
    // ================MC Hooks================
    
    @Override
    public boolean isOnFire() {
        return false;
    }
    
    @Override
    public boolean fireImmune() {
        return true;
    }
    
    @Override
    public boolean displayFireAnimation() {
        return false;
    }
    
    public void destroyAnimation() {
        this.kill();
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
    
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
    
    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return InteractionResult.PASS;
    }
    
    @Override
    public Packet<?> getAddEntityPacket() {
        // TODO Auto-generated method stub
        return null;
    }
    
    // ================Hit Result================
    
    public LittleHitResult rayTrace(Vec3 pos, Vec3 look) {
        LittleHitResult result = null;
        double distance = 0;
        for (Entity entity : entities()) {
            if (entity instanceof LittleLevelEntity levelEntity) {
                Vec3 newPos = levelEntity.origin.transformPointToFakeWorld(pos);
                Vec3 newLook = levelEntity.origin.transformPointToFakeWorld(look);
                
                if (levelEntity.physic.getBB().intersects(newPos, newLook)) {
                    LittleHitResult tempResult = levelEntity.rayTrace(pos, look);
                    if (tempResult == null)
                        continue;
                    double tempDistance = newPos.distanceTo(tempResult.hit.getLocation());
                    if (result == null || tempDistance < distance) {
                        result = tempResult;
                        distance = tempDistance;
                    }
                }
            } else {
                Vec3 newPos = origin.transformPointToFakeWorld(pos);
                Vec3 newLook = origin.transformPointToFakeWorld(look);
                if (entity.getBoundingBox().intersects(newPos, newLook)) {
                    LittleHitResult tempResult = new LittleHitResult(new EntityHitResult(entity, entity.getBoundingBox().clip(newPos, newLook).get()), fakeLevel);
                    double tempDistance = newPos.distanceTo(tempResult.hit.getLocation());
                    if (result == null || tempDistance < distance) {
                        result = tempResult;
                        distance = tempDistance;
                    }
                }
            }
        }
        
        Vec3 newPos = origin.transformPointToFakeWorld(pos);
        Vec3 newLook = origin.transformPointToFakeWorld(look);
        HitResult tempResult = fakeLevel.clip(new ClipContext(newPos, newLook, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (tempResult == null || tempResult instanceof BlockHitResult)
            return result;
        if (result == null || pos.distanceTo(tempResult.getLocation()) < distance)
            return new LittleHitResult(tempResult, fakeLevel);
        return result;
    }
    
    public InteractionResult onRightClick(@Nullable Player player, HitResult result) {
        if (result == null || !(result instanceof BlockHitResult))
            return InteractionResult.PASS;
        
        if (player != null && player.getMainHandItem().getItem() instanceof ItemLittleWrench)
            return ((ItemLittleWrench) player.getMainHandItem().getItem()).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, (BlockHitResult) result));
        
        return fakeLevel.getBlockState(((BlockHitResult) result).getBlockPos()).use(fakeLevel, player, InteractionHand.MAIN_HAND, (BlockHitResult) result);
    }
    
    // ================CLIENT================
    
    @Override
    public boolean shouldRender(double x, double y, double z) {
        Vec3 center = getRealCenter();
        double d0 = center.x - x;
        double d1 = center.y - y;
        double d2 = center.z - z;
        return this.shouldRenderAtSqrDistance(d0 * d0 + d1 * d1 + d2 * d2);
    }
}