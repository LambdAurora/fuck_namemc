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

package dev.lambdaurora.fuck_namemc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.Channel;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.login.*;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class FuckNameMC implements DedicatedServerModInitializer {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ServerMetadata.Version.class, new ServerMetadata.Version.Serializer())
			.registerTypeAdapter(ServerMetadata.Players.class, new ServerMetadata.Players.Deserializer())
			.registerTypeAdapter(ServerMetadata.class, new ServerMetadata.Deserializer())
			.registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
			.registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
			.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
			.create();
	private static final Logger LOGGER = LogManager.getLogger();
	public static Config config = Config.defaultConfig();

	@Override
	public void onInitializeServer() {
		info("Initializing \"Fuck NameMC\", blocking NameMC indexing since you cannot opt-out from having your data collected.");
		config = Config.load();
	}

	public static void info(String message, Object... params) {
		LOGGER.info(message, params);
	}

	public static void warn(String message, Object... params) {
		LOGGER.warn(message, params);
	}

	public static void warn(String message, Throwable throwable) {
		LOGGER.warn(message, throwable);
	}

	public static void error(String message, Throwable throwable) {
		LOGGER.error(message, throwable);
	}

	public static <T extends PacketListener> String describePacket(NetworkSide side, Packet<T> packet) {
		var state = NetworkState.getPacketHandlerState(packet);

		if (state == null)
			return describeNetworkSide(side) + " ; UNKNOWN PACKET (" + packet + ")";

		Integer packetId;
		try {
			packetId = state.getPacketId(side, packet);
		} catch (NullPointerException e) {
			packetId = state.getPacketId(side.getOpposite(), packet);
		}

		if (packetId == null)
			return describeNetworkSide(side) + " ; UNKNOWN PACKET (" + packet + ")";

		return describePacket(side, state, packetId, packet);
	}

	public static <T extends PacketListener> String describePacket(NetworkSide side, NetworkState state,
	                                                               int packetId, Packet<T> packet) {
		return describeNetworkSide(side) + " ;; " + packetId + " ;; " + getPacketName(state, packet) + '\n'
				+ describePacketFields(packet);
	}

	public static <T extends PacketListener> String describePacketFields(Packet<T> packet) {
		var builder = new StringBuilder();

		var fields = packet.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			var field = fields[i];
			field.setAccessible(true);
			if (describePacketField(builder, packet, field) && i != fields.length - 1) {
				builder.append('\n');
			}
		}

		return builder.toString();
	}

	public static <T extends PacketListener> boolean describePacketField(StringBuilder builder, Packet<T> packet, Field field) {
		if (field.getType() == Gson.class || (field.getModifiers() & Modifier.STATIC) != 0)
			return false;

		builder.append("  - ").append(field.getName())
				.append(" (").append(field.getType())
				.append(") = ");

		try {
			var value = field.get(packet);
			String stringValue = value == null ? "null" : value.toString();

			if (value instanceof ServerMetadata) {
				stringValue = GSON.toJson(value);
			}

			builder.append(stringValue);
		} catch (IllegalAccessException e) {
			builder.append("[COULD NOT GET FIELD VALUE]");
		}

		return true;
	}

	public static <T extends PacketListener> String getPacketName(NetworkState state, Packet<T> packet) {
		var clazz = packet.getClass();
		if (clazz == HandshakeC2SPacket.class)
			return "Handshake";
		else if (clazz == QueryPingC2SPacket.class)
			return "Query Ping";
		else if (clazz == QueryPongS2CPacket.class)
			return "Query Pong";
		else if (clazz == QueryRequestC2SPacket.class)
			return "Query Request";
		else if (clazz == QueryResponseS2CPacket.class)
			return "Query Response";
		else if (clazz == LoginHelloC2SPacket.class || clazz == LoginHelloS2CPacket.class)
			return "Login Hello";
		else if (clazz == LoginQueryRequestS2CPacket.class)
			return "Login Query Request";
		else if (clazz == LoginQueryResponseC2SPacket.class)
			return "Login Query Response";
		else if (clazz == LoginKeyC2SPacket.class)
			return "Login Key";
		else if (clazz == LoginCompressionS2CPacket.class)
			return "Login Compression";
		else if (clazz == LoginSuccessS2CPacket.class)
			return "Login Success";
		else if (clazz == DisconnectS2CPacket.class || clazz == LoginDisconnectS2CPacket.class)
			return state.name() + " Disconnect";
		else return "Unknown " + state.name() + " " + clazz;
	}

	public static String describeNetworkSide(NetworkSide side) {
		return switch (side) {
			case CLIENTBOUND -> "C <- S";
			case SERVERBOUND -> "C -> S";
		};
	}

	public static boolean isNameMCAddress(SocketAddress address) {
		if (address instanceof InetSocketAddress socketAddress) {
			return FuckNameMC.config.addresses().stream().anyMatch(addr -> socketAddress.getAddress().equals(addr));
		}
		return false;
	}

	public static void handleHandshake(ClientConnection connection, Channel channel, HandshakeC2SPacket packet, CallbackInfo ci) {
		if (isNameMCAddress(connection.getAddress())) {
			if (FuckNameMC.config.silent() && packet.getIntendedState() == NetworkState.STATUS) {
				FuckNameMC.info("Blocked a banned IP address.");
				channel.close().awaitUninterruptibly();
				ci.cancel();
			} else {
				FuckNameMC.info("Found a banned IP address, preparing for dummy status packet.");
			}

			((ClientConnectionShenanigans) connection).fuck_namemc$markAsNameMC();
		}
	}
}
