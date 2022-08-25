package se.gameffect.gameffectmccreativeevent.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;
import se.gameffect.gameffectmccreativeevent.GameffectCreativeEvent;

public class HubCommand extends NovaCommand {
	public HubCommand() {
		super("hub", GameffectCreativeEvent.getInstance());

		setAliases(NovaCommand.generateAliasList("menu", "lobby"));

		setAllowedSenders(AllowedSenders.PLAYERS);
		setRequireOp(false);
		setEmptyTabMode(true);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		Player player = (Player) sender;
		GameffectCreativeEvent.getInstance().tpToSpawn(player);
		return true;
	}
}