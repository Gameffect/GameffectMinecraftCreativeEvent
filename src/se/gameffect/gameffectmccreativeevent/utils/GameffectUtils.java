package se.gameffect.gameffectmccreativeevent.utils;

import net.md_5.bungee.api.ChatColor;
import net.zeeraa.novacore.commons.utils.TextUtils;

public class GameffectUtils {
	public static final String pingColor(int ping) {
		ChatColor color = ChatColor.DARK_RED;

		if (ping < 200) {
			color = ChatColor.GREEN;
		} else if (ping < 400) {
			color = ChatColor.DARK_GREEN;
		} else if (ping < 600) {
			color = ChatColor.YELLOW;
		} else if (ping < 800) {
			color = ChatColor.RED;
		}

		return color + "" + ping;
	}

	public static final String tpsColor(double tps) {
		return ((tps > 18.0) ? ChatColor.GREEN : (tps > 16.0) ? ChatColor.YELLOW : ChatColor.RED).toString() + ((tps > 20.0) ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0);
	}

	public static final String formatTPS(double tps) {
		return ChatColor.GOLD + "TPS: " + GameffectUtils.tpsColor(tps) + (tps < 18 ? " " + ChatColor.RED + TextUtils.ICON_WARNING : "");
	}

	public static final String formatPing(int ping) {
		return ChatColor.GOLD + "Ping: " + GameffectUtils.pingColor(ping) + "ms " + (ping > 800 ? ChatColor.YELLOW + TextUtils.ICON_WARNING : "");
	}
}