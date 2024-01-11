/*
 * Copyright (c) 2021-2022 LambdAurora <email@lambdaurora.dev>
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

import io.netty.channel.Channel;
import net.minecraft.server.network.ServerNetworkIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerNetworkIo.class)
public abstract class ServerNetworkIoMixin {
	@Mixin(targets = {"net.minecraft.server.network.ServerNetworkIo$C_nbluewha"})
	public static abstract class ChannelInitializerMixin {
		@Inject(
				method = "initChannel",
				at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getRateLimit()I")
		)
		private void onInitChannel(Channel channel, CallbackInfo ci) {
			channel.pipeline().remove("legacy_query");
		}
	}
}
