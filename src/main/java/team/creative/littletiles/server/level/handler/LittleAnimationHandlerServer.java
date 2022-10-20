package team.creative.littletiles.server.level.handler;

import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import team.creative.littletiles.common.entity.LittleLevelEntity;
import team.creative.littletiles.common.level.handler.LittleAnimationHandler;
import team.creative.littletiles.common.level.little.CreativeLevel;

public class LittleAnimationHandlerServer extends LittleAnimationHandler {
    
    public LittleAnimationHandlerServer(Level level) {
        super(level);
    }
    
    public void tickServer(LevelTickEvent event) {
        if (event.phase == Phase.END && level == event.level) {
            tick();
            
            for (LittleLevelEntity entity : entities) {
                if (entity.level != level || entity.level instanceof CreativeLevel)
                    continue;
                entity.performTick();
            }
        }
    }
    
}