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

package dev.lambdaurora.fuck_namemc;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UuidUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;

public record Config(boolean silent, List<InetAddress> addresses, List<MutableText> alternateMotd, List<GameProfile> alternatePlayerList) {
	public static final Path CONFIG_PATH = FileSystems.getDefault().getPath("config", "fuck_namemc.json");
	public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("silent").orElse(true).forGetter(Config::silent),
			Codec.STRING.listOf().fieldOf("namemc_addresses").orElseGet(List::of).xmap(
					Config::deserializeAddresses,
					addresses1 -> addresses1.stream().map(InetAddress::toString).map(raw -> raw.substring(1)).toList()
			).forGetter(Config::addresses),
			Codec.STRING.listOf().fieldOf("alternate_motd").orElseGet(List::of).xmap(
					raw -> raw.stream().map(Text.Serializer::fromLenientJson).toList(),
					pretty -> pretty.stream().map(Text.Serializer::toJson).toList()
			).forGetter(Config::alternateMotd),
			Codec.STRING.listOf().fieldOf("alternate_player_list").orElseGet(List::of).xmap(
					Config::deserializePlayerList,
					pretty -> pretty.stream().map(GameProfile::getName).toList()
			).forGetter(Config::alternatePlayerList)
	).apply(instance, Config::new));

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Random RANDOM = new Random();

	private static boolean createConfigDirectoryIfNeeded() {
		try {
			if (!Files.exists(CONFIG_PATH.getParent()))
				Files.createDirectory(CONFIG_PATH.getParent());
			return true;
		} catch (IOException e) {
			FuckNameMC.warn("Could not create missing \"config\" directory.", e);
			return false;
		}
	}

	public Text pickMotd() {
		var alternates = this.alternateMotd();
		if (alternates.isEmpty())
			return Text.literal("NameMC, please stop indexing this server.");
		int index = RANDOM.nextInt(alternates.size());
		return this.alternateMotd().get(index);
	}

	/**
	 * Saves the configuration to file.
	 *
	 * @return the current configuration
	 */
	public Config save() {
		FuckNameMC.info("Saving configuration...");
		if (!createConfigDirectoryIfNeeded())
			return this;

		var config = CODEC.encode(this, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).result();
		if (config.isEmpty()) {
			FuckNameMC.warn("Failed to serialize configuration.");
			return this;
		}
		try (var writer = Files.newBufferedWriter(CONFIG_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			var jsonWriter = GSON.newJsonWriter(writer);
			GSON.toJson(config.get().getAsJsonObject(), jsonWriter);
		} catch (IOException e) {
			FuckNameMC.warn("Failed to save configuration.", e);
		}
		return this;
	}

	public static Config load() {
		FuckNameMC.info("Loading configuration...");

		if (!Files.exists(CONFIG_PATH)) {
			if (!createConfigDirectoryIfNeeded())
				return defaultConfig();

			return defaultConfig().save();
		}

		try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
			var result = CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(reader)).map(Pair::getFirst);
			return result.result().orElseGet(() -> {
				FuckNameMC.warn("Could not load configuration, using default configuration instead.");
				return defaultConfig();
			});
		} catch (IOException e) {
			FuckNameMC.warn("Could not load configuration file.");
			e.printStackTrace();
			return defaultConfig();
		}
	}

	public static Config defaultConfig() {
		return new Config(true,
				Config.deserializeAddresses(List.of("2606:4700:20::681b:1272", "104.27.18.114", "51.222.110.150")),
				List.of(
						Text.literal("F*** you NameMC.").formatted(Formatting.RED),
						Text.literal("NameMC, stop indexing this server.").formatted(Formatting.RED),
						Text.literal("NameMC, please learn basic decency.").formatted(Formatting.RED),
						Text.literal("NameMC, give me a way to opt out.").formatted(Formatting.RED),
						Text.literal("NameMC, I hate you, please stop indexing this.").formatted(Formatting.RED)
				),
				Config.deserializePlayerList(List.of("No",
						"This",
						"Is",
						"Not",
						"Fucking",
						"Public")));
	}

	private static List<InetAddress> deserializeAddresses(List<String> raw) {
		return raw.stream().map(InetAddresses::forString).toList();
	}

	private static List<GameProfile> deserializePlayerList(List<String> raw) {
		return raw.stream().map(name -> name.substring(0, Math.min(32, name.length())))
				.map(name -> new GameProfile(UuidUtil.getOfflinePlayerUuid(name), name)).toList();
	}
}
