package se.gameffect.gameffectmccreativeevent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.commons.async.AsyncManager;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.commons.utils.JSONFileType;
import net.zeeraa.novacore.commons.utils.JSONFileUtils;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;
import net.zeeraa.novacore.spigot.command.CommandRegistry;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.multiverse.MultiverseManager;
import net.zeeraa.novacore.spigot.module.modules.multiverse.MultiverseWorld;
import net.zeeraa.novacore.spigot.module.modules.multiverse.PlayerUnloadOption;
import net.zeeraa.novacore.spigot.module.modules.multiverse.WorldOptions;
import net.zeeraa.novacore.spigot.module.modules.multiverse.WorldUnloadOption;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.utils.VectorArea;
import se.gameffect.gameffectmccreativeevent.command.ResetInstanceCommand;
import se.gameffect.gameffectmccreativeevent.command.ResetPublicInstanceCommand;

public class GameffectCreativeEvent extends JavaPlugin implements Listener {
	public static final String PUBLIC_INSTANCE_NAME = "shared_instance";

	private static GameffectCreativeEvent instance;

	private List<String> usernames;
	private Map<String, MultiverseWorld> worlds;

	private File templateWorld;

	private VectorArea publicPortal;
	private VectorArea privatePortal;

	private Task task;

	private List<Player> inPortal;

	public static GameffectCreativeEvent getInstance() {
		return instance;
	}

	public Map<String, MultiverseWorld> getWorlds() {
		return worlds;
	}

	@Override
	public void onEnable() {
		GameffectCreativeEvent.instance = this;

		getDataFolder().mkdirs();

		usernames = new ArrayList<String>();
		inPortal = new ArrayList<Player>();
		worlds = new HashMap<String, MultiverseWorld>();

		templateWorld = new File(getDataFolder().getAbsoluteFile() + File.separator + "world");
		File configFile = new File(getDataFolder().getAbsolutePath() + File.separator + "config.json");
		if (!configFile.exists()) {
			try {
				JSONFileUtils.createEmpty(configFile, JSONFileType.JSONObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			JSONObject config = JSONFileUtils.readJSONObjectFromFile(configFile);
			JSONArray usernamesJSON = config.getJSONArray("users");
			for (int i = 0; i < usernamesJSON.length(); i++) {
				String ign = usernamesJSON.getString(i);
				usernames.add(ign);
				Log.info("Registering user " + ign);
			}

			publicPortal = VectorArea.fromJSON(config.getJSONObject("public_portal"));
			privatePortal = VectorArea.fromJSON(config.getJSONObject("private_portal"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		ModuleManager.require(MultiverseManager.class);

		task = new SimpleTask(this, () -> {
			Bukkit.getServer().getOnlinePlayers().forEach(player -> {
				if (player.getWorld().equals(Bukkit.getServer().getWorlds().stream().findFirst().get())) {
					if (player.getGameMode() != GameMode.ADVENTURE) {
						if (!player.isOp()) {
							player.setGameMode(GameMode.ADVENTURE);
						}
					}

					PortalType type = PortalType.NONE;
					if (publicPortal.isInsideBlock(player.getLocation().toVector())) {
						type = PortalType.PUBLIC;
					}

					if (privatePortal.isInsideBlock(player.getLocation().toVector())) {
						type = PortalType.PRIVATE;
					}

					if (type == PortalType.NONE) {
						if (inPortal.contains(player)) {
							inPortal.remove(player);
						}
					} else {
						if (!inPortal.contains(player)) {
							Log.trace(player.getName() + " is in portal type " + type.name());

							inPortal.add(player);

							String instance = type == PortalType.PUBLIC ? GameffectCreativeEvent.PUBLIC_INSTANCE_NAME : player.getName().toLowerCase();
							if (worlds.containsKey(instance)) {
								player.teleport(worlds.get(instance).getWorld().getSpawnLocation());
								if (type == PortalType.PRIVATE) {
									player.sendMessage(ChatColor.GREEN + "För att återställa din privata instans kör kommandot " + ChatColor.AQUA + "/reset");
								}
							} else {
								player.sendMessage(ChatColor.RED + "Instansen är inte redo ännu. Vänligen vänta några sekunder och testa igen. Om felet fortsätter att uppstå kontakta personalen och visa dem detta meddelande. Error code: ERR:WORLD_NOT_LOADED");
							}
						}
					}
				} else {
					if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
						if (!player.isOp()) {
							player.setGameMode(GameMode.CREATIVE);
						}
					}
				}
			});
		}, 2L);
		Task.tryStartTask(task);

		Bukkit.getServer().getWorlds().stream().findFirst().get().setDifficulty(Difficulty.PEACEFUL);

		setupWorld(GameffectCreativeEvent.PUBLIC_INSTANCE_NAME, true, false, () -> Log.info("Shared world loaded"));
		usernames.forEach(name -> setupWorld(name, false, true, () -> Log.info("Instance ready for user " + name)));

		try {
			CommandRegistry.registerCommand(ResetInstanceCommand.class);
			CommandRegistry.registerCommand(ResetPublicInstanceCommand.class);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			Log.error("Failed to register commands");
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		Task.tryStopTask(task);
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll((Plugin) this);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		if(e.getEntity().getWorld().getName().equalsIgnoreCase(GameffectCreativeEvent.PUBLIC_INSTANCE_NAME)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		if(e.getBlock().getWorld().getName().equalsIgnoreCase(GameffectCreativeEvent.PUBLIC_INSTANCE_NAME)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		tpToSpawn(player);
		VersionIndependentUtils.get().sendTabList(player, ChatColor.GOLD + "Gameffect Minecraft Event", "");
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent e) {
		inPortal.remove(e.getPlayer());
	}

	public void tpToSpawn(Player player) {
		player.setGameMode(GameMode.ADVENTURE);
		player.teleport(Bukkit.getServer().getWorlds().stream().findFirst().get().getSpawnLocation());
	}

	public void setupWorld(final String name, boolean persistent, boolean deleteFile, final Callback callback) {
		if (worlds.containsKey(name.toLowerCase())) {
			MultiverseWorld world = worlds.get(name.toLowerCase());
			world.getWorld().getPlayers().forEach(player -> {
				player.sendMessage(ChatColor.AQUA + "Världen återställs. Vänligen vänta...");
				tpToSpawn(player);
			});
			Log.info("Unloading world " + world.getWorld().getName());
			MultiverseManager.getInstance().unload(world);
		}

		final String worldName = persistent ? name : name.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);

		File targetFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + File.separator + worldName);

		if (deleteFile) {
			if (targetFile.exists()) {
				targetFile.delete();
			}
		}

		AsyncManager.runAsync(() -> {
			Log.debug("Begin async copy to " + worldName);
			try {
				FileUtils.copyDirectory(templateWorld, targetFile);
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