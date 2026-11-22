package jp.yuka1201.luckyblock.block;

import jp.yuka1201.luckyblock.event.LuckyEventTable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LuckyBlock extends Block {
    public LuckyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            LuckyEventTable.triggerRandomEvent(serverLevel, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.1 + random.nextDouble() * 0.9;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(
                    random.nextBoolean() ? ParticleTypes.WAX_ON : ParticleTypes.END_ROD,
                    x, y, z, 0.0, 0.025, 0.0
            );
        }
    }
}
