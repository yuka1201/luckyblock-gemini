package jp.yuka1201.luckyblock.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface LuckyEvent {
    void execute(ServerLevel level, BlockPos pos, Player player);
}
