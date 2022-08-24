package se.gameffect.gameffectmccreativeevent.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;
import se.gameffect.gameffectmccreativeevent.GameffectCreativeEvent;

public class ResetInstanceCommand extends NovaCommand {
	public ResetInstanceCommand() {
		super("resetinstance", GameffectCreativeEvent.getInstance());

		setAllowedSenders(AllowedSenders.PLAYERS);
		setRequireOp(false);
		setEmptyTabMode(true);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		Player player = (Player) sender;
		if (GameffectCreativeEvent.getInstance().getWorlds().containsKey(player.getName().toLowerCase())) {
			GameffectCreativeEvent.getInstance().setupWorld(player.getName().toLowerCase(), false, true, () -> player.sendMessage(ChatColor.GREEN + "Din privata instans har återställts"));
		} else {
			player.sendMessage(ChatColor.RED + "Du har ingen aktiv instans att återställa");
		}
		return true;
	}
}