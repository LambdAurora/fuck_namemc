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

import com.mojang.authlib.GameProfile;
import dev.lambdaurora.fuck_namemc.ClientConnectionShenanigans;
import dev.lambdaurora.fuck_namemc.FuckNameMC;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerQueryNetworkHandler.class)
public class ServerQueryNetworkHandlerMixin {
	@Shadow
	@Final
	private ClientConnection connection;

	@Shadow
	private boolean responseSent;

	@Inject(method = "onRequest", at = @At("HEAD"), cancellable = true)
	private void onRequest(QueryRequestC2SPacket packet, CallbackInfo ci) {
		if (((ClientConnectionShenanigans) this.connection).fuck_namemc$isNameMC() && !this.responseSent) {
			this.responseSent = true;
			var metadata = new ServerMetadata();
			metadata.setDescription(FuckNameMC.config.pickMotd());
			var players = new ServerMetadata.Players(0, 0);
			players.setSample(FuckNameMC.config.alternatePlayerList().toArray(GameProfile[]::new));
			metadata.setPlayers(players);
			metadata.setVersion(new ServerMetadata.Version("AntiNameMC", 0));
			this.connection.send(new QueryResponseS2CPacket(metadata));
			ci.cancel();
		}
	}
}
