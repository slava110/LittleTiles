package com.creativemd.littletiles.common.structure.type.door;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleDoorPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.connection.IStructureChildConnector;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.tiles.LittleTile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public abstract class LittleDoor extends LittleStructure {
	
	public LittleDoor(LittleStructureType type) {
		super(type);
	}
	
	public boolean activateParent = false;
	public PairList<Integer, Integer> childActivation;
	
	@Override
	protected void loadFromNBTExtra(NBTTagCompound nbt) {
		if (nbt.hasKey("activator")) {
			int[] array = nbt.getIntArray("activator");
			childActivation = new PairList<>();
			for (int i = 0; i < array.length; i += 2) {
				childActivation.add(array[i], array[i + 1]);
			}
		} else
			childActivation = new PairList<>();
		activateParent = nbt.getBoolean("activateParent");
	}
	
	@Override
	protected void writeToNBTExtra(NBTTagCompound nbt) {
		if (childActivation != null) {
			int[] array = new int[childActivation.size() * 2];
			int i = 0;
			for (Pair<Integer, Integer> pair : childActivation) {
				array[i * 2] = pair.key;
				array[i * 2 + 1] = pair.value;
				i++;
			}
			nbt.setIntArray("activator", array);
		}
		if (activateParent)
			nbt.setBoolean("activateParent", activateParent);
	}
	
	public boolean doesActivateChild(int id) {
		return childActivation.containsKey(id);
	}
	
	public boolean activate(@Nullable EntityPlayer player, BlockPos pos, @Nullable LittleTile tile) {
		if (!hasLoaded() || !loadChildren() || !loadParent()) {
			player.sendStatusMessage(new TextComponentTranslation("Cannot interact with door! Not all tiles are loaded!"), true);
			return false;
		}
		
		if (activateParent) {
			LittleStructure parentStructure = parent.getStructureWithoutLoading();
			if (parentStructure instanceof LittleDoor)
				return ((LittleDoor) parentStructure).activate(player, pos, null);
			return false;
		}
		
		DoorOpeningResult result = canOpenDoor(player);
		if (result == null)
			return false;
		
		UUIDSupplier uuid = new UUIDSupplier();
		
		if (getWorld().isRemote)
			PacketHandler.sendPacketToServer(new LittleDoorPacket(getMainTile(), uuid.uuid, result));
		
		openDoor(player, uuid, result);
		return true;
	}
	
	@Override
	public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ, LittleActionActivated action) {
		if (world.isRemote) {
			activate(player, pos, tile);
			action.preventInteraction = true;
		}
		return true;
	}
	
	public List<LittleDoor> collectDoorChildren() {
		List<LittleDoor> doors = new ArrayList<>();
		for (Pair<Integer, Integer> pair : childActivation) {
			IStructureChildConnector child = children.get(pair.key);
			if (child == null)
				return null;
			
			LittleStructure childStructure = child.getStructure(getWorld());
			if (childStructure == null || !(childStructure instanceof LittleDoor))
				return null;
			
			doors.add((LittleDoor) childStructure);
		}
		
		return doors;
	}
	
	public DoorOpeningResult canOpenDoor(@Nullable EntityPlayer player) {
		if (inMotion)
			return null;
		
		List<LittleDoor> doors = collectDoorChildren();
		
		if (doors == null)
			return null;
		
		NBTTagCompound nbt = null;
		
		for (LittleDoor door : doors) {
			DoorOpeningResult subResult = door.canOpenDoor(player);
			
			if (subResult == null)
				return null;
			
			if (!subResult.isEmpty()) {
				if (nbt == null)
					nbt = new NBTTagCompound();
				nbt.setTag("c" + door.parent.getChildID(), subResult.nbt);
			}
		}
		
		if (nbt == null)
			return EMPTY_OPENING_RESULT;
		return new DoorOpeningResult(nbt);
	}
	
	public EntityAnimation openDoor(@Nullable EntityPlayer player, UUIDSupplier uuid, DoorOpeningResult result) {
		for (LittleDoor door : collectDoorChildren()) {
			door.openDoor(player, uuid, result);
		}
		return null;
	}
	
	public boolean inMotion;
	
	public void setInMotion(boolean value) {
		this.inMotion = value;
	}
	
	public boolean isInMotion() {
		return inMotion;
	}
	
	public static final DoorOpeningResult EMPTY_OPENING_RESULT = new DoorOpeningResult(null);
	
	public static class DoorOpeningResult {
		
		public final NBTTagCompound nbt;
		
		public DoorOpeningResult(NBTTagCompound nbt) {
			this.nbt = nbt;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DoorOpeningResult)
				return nbt != null ? nbt.equals(((DoorOpeningResult) obj).nbt) : ((DoorOpeningResult) obj).nbt == null;
			return false;
		}
		
		public boolean isEmpty() {
			return nbt == null;
		}
		
		@Override
		public int hashCode() {
			return nbt != null ? nbt.hashCode() : super.hashCode();
		}
		
		@Override
		public String toString() {
			return "" + nbt;
		}
	}
}
