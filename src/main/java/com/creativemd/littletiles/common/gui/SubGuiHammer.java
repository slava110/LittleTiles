package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.gui.SubGui;
import com.creativemd.creativecore.common.gui.controls.GuiButton;
import com.creativemd.creativecore.common.gui.controls.GuiControl;
import com.creativemd.creativecore.common.gui.controls.GuiTextfield;
import com.creativemd.creativecore.common.gui.event.ControlClickEvent;
import com.creativemd.creativecore.common.packet.GuiControlPacket;
import com.creativemd.creativecore.common.packet.GuiUpdatePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

public class SubGuiHammer extends SubGui {
	
	public SubGuiHammer()
	{
		
	}
	
	public int sizeX = 1;
	public int sizeY = 1;
	public int sizeZ = 1;
	
	@Override
	public void createControls() {
		controls.add(new GuiButton("<", 45, 10, 10, 20, 0));
		controls.add(new GuiButton(">", 75, 10, 10, 20, 1));
		controls.add(new GuiButton("<", 45, 30, 10, 20, 2));
		controls.add(new GuiButton(">", 75, 30, 10, 20, 3));
		controls.add(new GuiButton("<", 45, 50, 10, 20, 4));
		controls.add(new GuiButton(">", 75, 50, 10, 20, 5));
		controls.add(new GuiButton("HAMMER IT", 100, 30, 60, 20, 6));
	}

	@Override
	public void drawOverlay(FontRenderer fontRenderer) {
		fontRenderer.drawString("" + sizeX, 62, 14, 0);
		fontRenderer.drawString("" + sizeY, 62, 34, 0);
		fontRenderer.drawString("" + sizeZ, 62, 54, 0);
	}
	
	@CustomEventSubscribe
	public void onClicked(ControlClickEvent event)
	{
		if(event.source instanceof GuiButton)
		{
			GuiButton button = (GuiButton) event.source;
			switch (button.id) {
			case 0:
				sizeX--;
				break;
			case 1:
				sizeX++;
				break;
			case 2:
				sizeY--;
				break;
			case 3:
				sizeY++;
				break;
			case 4:
				sizeZ--;
				break;
			case 5:
				sizeZ++;
				break;
			}
			if(sizeX < 1)
				sizeX = 16;
			if(sizeX > 16)
				sizeX = 1;
			if(sizeY < 1)
				sizeY = 16;
			if(sizeY > 16)
				sizeY = 1;
			if(sizeZ < 1)
				sizeZ = 16;
			if(sizeZ > 16)
				sizeZ = 1;
			
			if(button.id == 6)
			{
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setByte("sizeX", (byte) sizeX);
				nbt.setByte("sizeY", (byte) sizeY);
				nbt.setByte("sizeZ", (byte) sizeZ);
				sendPacketToServer(button.id, nbt);
			}
		}
	}
}
