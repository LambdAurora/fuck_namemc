/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.fuck_namemc.mixin;

import dev.lambdaurora.fuck_namemc.ClientConnectionShenanigans;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements ClientConnectionShenanigans {
	@Unique
	private boolean fuck_namemc$isNamemc = false;

	// Temp packet logger stuff for debug.
	/*@Inject(
			method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;)V")
	)
	private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet,
	                            CallbackInfo ci) {
		try {
			FuckNameMC.info("PACKET FROM " + this.channel.remoteAddress().toString() + " CONTENT " + FuckNameMC.describePacket(this.side, packet));
		} catch (Throwable e) {
			FuckNameMC.error("Could not log packet", e);
		}
	}

	@Inject(method = "sendInternal", at = @At("HEAD"))
	private void onSend(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, NetworkState networkState, NetworkState networkState2, CallbackInfo ci) {
		try {
			FuckNameMC.info("PACKET " + FuckNameMC.describePacket(this.side.getOpposite(), packet));
		} catch (Throwable e) {
			FuckNameMC.error("Could not log packet", e);
		}
	}*/

	@Override
	public boolean fuck_namemc$isNameMC() {
		return this.fuck_namemc$isNamemc;
	}

	@Override
	public void fuck_namemc$markAsNameMC() {
		this.fuck_namemc$isNamemc = true;
	}
}
