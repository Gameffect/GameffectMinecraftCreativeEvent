package se.gameffect.gameffectmccreativeevent.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;
import se.gameffect.gameffectmccreativeevent.GameffectCreativeEvent;

public class ResetPublicInstanceCommand extends NovaCommand {
	public ResetPublicInstanceCommand() {
		super("resetpublicinstance", GameffectCreativeEvent.getInstance());

		setAllowedSenders(AllowedSenders.CONSOLE);
		setEmptyTabMode(true);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (GameffectCreativeEvent.getInstance().getWorlds().containsKey(GameffectCreativeEvent.PUBLIC_INSTANCE_NAME)) {
			GameffectCreativeEvent.getInstance().setupWorld(GameffectCreativeEvent.PUBLIC_INSTANCE_NAME, true, true, () -> Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Public world has been resetted"));
		} else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Public world not loaded");
		}
		return true;
	}
}