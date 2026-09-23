package dev.guildbound.world;

import dev.guildbound.network.RpgNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Temporary registration point, placed in creative mode until guild world generation is implemented. */
public final class GuildDeskBlock extends Block {
    public GuildDeskBlock(Properties properties) { super(properties); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) RpgNetwork.openRegistration(serverPlayer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
