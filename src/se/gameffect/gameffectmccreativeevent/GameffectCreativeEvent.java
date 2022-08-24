package se.gameffect.gameffectmccreativeevent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;

import net.zeeraa.novacore.commons.async.AsyncManager;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.commons.utils.JSONFileType;
import net.zeeraa.novacore.commons.utils.JSONFileUtils;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.multiverse.MultiverseManager;
import net.zeeraa.novacore.spigot.module.modules.multiverse.MultiverseWorld;
import net.zeeraa.novacore.spigot.module.modules.multiverse.PlayerUnloadOption;
import net.zeeraa.novacore.spigot.module.modules.multiverse.WorldOptions;
import net.zeeraa.novacore.spigot.module.modules.multiverse.WorldUnloadOption;

public class GameffectCreativeEvent extends JavaPlugin implements Listener {
	private List<String> usernames;
	private Map<String, MultiverseWorld> worlds;

	private File templateWorld;

	@Override
	public void onEnable() {
		getDataFolder().mkdirs();

		usernames = new ArrayList<String>();
		worlds = new HashMap<String, MultiverseWorld>();

		templateWorld = new File(getDataFolder().getAbsoluteFile() + File.separator + "world");
		File usersFile = new File(getDataFolder().getAbsolutePath() + File.separator + "usernames.json");
		if (!usersFile.exists()) {
			try {
				JSONFileUtils.createEmpty(usersFile, JSONFileType.JSONArray);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			JSONArray usernamesJSON = JSONFileUtils.readJSONArrayFromFile(usersFile);
			for (int i = 0; i < usernamesJSON.length(); i++) {
				String ign = usernamesJSON.getString(i);
				usernames.add(ign);
				Log.info("Registering user " + ign);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		ModuleManager.require(MultiverseManager.class);

		setupWorld("shared", true, () -> Log.info("Shared world loaded"));
		usernames.forEach(name -> setupWorld(name, false, () -> Log.info("Instance ready for user " + name)));
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll((Plugin) this);
	}

	public void setupWorld(final String name, boolean persistent, final Callback callback) {
		if (worlds.containsKey(name.toLowerCase())) {
			final Player player = Bukkit.getServer().getPlayer(name);
			if (player != null) {
				player.sendMessage(ChatColor.GOLD + "Resetting world. Please wait....");
				player.teleport(Bukkit.getServer().getWorlds().stream().findFirst().get().getSpawnLocation());
			}
			MultiverseWorld world = worlds.get(name.toLowerCase());
			Log.info("Unloading world " + world.getWorld().getName());
			MultiverseManager.getInstance().unload(world);
		}

		final String worldName = persistent ? name + "_p" : name.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);

		AsyncManager.runAsync(() -> {
			Log.debug("Begin async copy to " + worldName);
			try {
				FileUtils.copyDirectory(templateWorld, new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + File.separator + worldName));
				AsyncManager.runSync(() -> {
					Log.debug("Creating world with multiverse manager");
					final WorldOptions options = new WorldOptions(worldName);
					options.setPlayerUnloadOption(PlayerUnloadOption.SEND_TO_FIRST);
					final MultiverseWorld world = MultiverseManager.getInstance().createWorld(options);
					if (!persistent) {
						world.setUnloadOption(WorldUnloadOption.DELETE);
					}
					Log.debug("World loaded");
					worlds.put(name.toLowerCase(), world);
					callback.execute();
				});
			} catch (IOException e) {
				Log.error("Failed to copy world. " + e.getClass().getName() + " " + e.getMessage());
				e.printStackTrace();
			}
		});
	}
}