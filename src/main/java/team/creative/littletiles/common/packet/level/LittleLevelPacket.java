package team.creative.littletiles.common.packet.level;

import java.util.UUID;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.littletiles.common.entity.level.LittleLevelEntity;
import team.creative.littletiles.common.level.handler.LittleAnimationHandlers;
import team.creative.littletiles.common.level.little.LittleLevel;

public class LittleLevelPacket extends CreativePacket {
    
    public UUID uuid;
    public Packet packet;
    
    public LittleLevelPacket() {}
    
    public LittleLevelPacket(LittleLevel level, Packet packet) {
        this.uuid = level.key();
        this.packet = packet;
    }
    
    public LittleLevelPacket(UUID uuid, Packet packet) {
        this.uuid = uuid;
        this.packet = packet;
    }
    
    @Override
    public void execute(Player player) {
        LittleLevelEntity entity = LittleAnimationHandlers.find(player.level.isClientSide, uuid);
        if (entity == null)
            return;
        
        PacketListener listener = ((LittleLevel) entity.getSubLevel()).getPacketListener();
        packet.handle(listener);
    }
    
    @Override
    public void executeClient(Player player) {}
    
    @Override
    public void executeServer(ServerPlayer player) {}
    
}
