package com.feed_the_beast.mods.ftbessentials.net;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SelectTemperedJarRecipePacket
{
	private final BlockPos pos;
	private final ResourceLocation id;

	public SelectTemperedJarRecipePacket(BlockPos p, ResourceLocation r)
	{
		pos = p;
		id = r;
	}

	public SelectTemperedJarRecipePacket(PacketBuffer buf)
	{
		pos = buf.readBlockPos();
		id = buf.readResourceLocation();
	}

	public void write(PacketBuffer buf)
	{
		buf.writeBlockPos(pos);
		buf.writeResourceLocation(id);
	}

	public void handle(Supplier<NetworkEvent.Context> context)
	{
		final ServerPlayerEntity player = context.get().getSender();

		context.get().enqueueWork(() -> {
			/*
			TileEntity entity = player.world.getTileEntity(pos);

			if (entity instanceof TemperedJarBlockEntity)
			{
				player.world.getRecipeManager().getRecipe(id).ifPresent(r -> {
					if (r instanceof JarRecipe && ((JarRecipe) r).isAvailableFor(player))
					{
						((TemperedJarBlockEntity) entity).setRecipe(player, (JarRecipe) r);
						entity.markDirty();
					}
				});
			}
			 */
		});

		context.get().setPacketHandled(true);
	}
}