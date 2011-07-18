package com.herocraftonline.dev.heroes.command.commands;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.command.BaseCommand;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.util.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class SuppressCommand extends BaseCommand {

    public SuppressCommand(Heroes plugin) {
        super(plugin);
        setName("Suppress");
        setDescription("Toggles the suppression of skill messages");
        setUsage("/hero stfu <skill>");
        setMinArgs(1);
        setMaxArgs(1);
        getIdentifiers().add("hero stfu");
        getIdentifiers().add("hero suppress");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Hero hero = plugin.getHeroManager().getHero(player);

            if (args.length == 0) {
                Set<String> suppressions = hero.getSuppressedSkills();
                if (suppressions.isEmpty()) {
                    Messaging.send(player, "No skills suppressed.");
                    return;
                }

                String list = "Suppressing ";
                for (String skill : suppressions) {
                    list += skill + ", ";
                }
                list = list.substring(0, list.length() - 2);

                Messaging.send(player, list);
            } else {
                BaseCommand cmd = plugin.getCommandManager().getCommand(args[0]);
                if (cmd == null || !(cmd instanceof Skill)) {
                    Messaging.send(player, "Skill not found.");
                    return;
                }
                Skill skill = (Skill) cmd;
                if (hero.isSuppressing(skill)) {
                    hero.setSuppressed(skill, false);
                    Messaging.send(player, "Messages from $1 are no longer suppressed.", skill.getName());
                } else {
                    hero.setSuppressed(skill, true);
                    Messaging.send(player, "Messages from $1 are now suppressed.", skill.getName());
                }
            }
        }
    }
}
