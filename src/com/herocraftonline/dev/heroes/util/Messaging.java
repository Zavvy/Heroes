package com.herocraftonline.dev.heroes.util;

import com.herocraftonline.dev.heroes.Heroes;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messaging {

    public static void broadcast(Heroes plugin, String msg, Object... params) {
        plugin.getServer().broadcastMessage(parameterizeMessage(msg, params));
    }

    public static String createManaBar(int mana) {
        String manaBar = ChatColor.RED + "[" + ChatColor.BLUE;
        int progress = (int) (mana / 100.0 * 50);
        for (int i = 0; i < progress; i++) {
            manaBar += "|";
        }
        manaBar += ChatColor.DARK_RED;
        for (int i = 0; i < 50 - progress; i++) {
            manaBar += "|";
        }
        manaBar += ChatColor.RED + "]";
        return manaBar + " - " + ChatColor.BLUE + mana + "%";
    }

    public static void send(CommandSender player, String msg, Object... params) {
        player.sendMessage(parameterizeMessage(msg, params));
    }

    private static String parameterizeMessage(String msg, Object... params) {
        msg = ChatColor.BLUE + "Heroes: " + ChatColor.RED + msg;
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                msg = msg.replace("$" + (i + 1), ChatColor.WHITE + params[i].toString() + ChatColor.RED);
            }
        }
        return msg;
    }

}
