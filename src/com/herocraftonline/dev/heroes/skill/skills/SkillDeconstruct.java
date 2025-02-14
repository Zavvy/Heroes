package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass.ExperienceType;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillDeconstruct extends ActiveSkill {

    public SkillDeconstruct(Heroes plugin) {
        super(plugin, "Deconstruct");
        setDescription("Deconstructs the object you are holding.");
        setUsage("/skill deconstruct <list>");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill deconstruct", "skill dstruct" });
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        String root = "IRON_AXE";
        node.setProperty("require-workbench", true);
        node.setProperty(root + "." + Setting.LEVEL.node(), 1);
        node.setProperty(root + "." + Setting.EXP.node(), 0);
        node.setProperty(root + ".min-durability", .5); //Minimum durability percentage the item must have to deconstruct
        node.setProperty(root + ".IRON_INGOT", 1);
        node.setProperty(root + ".STICK", 1);
        node.setProperty(Setting.USE_TEXT.node(), "%hero% has deconstructed a %item%");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText(getSetting(null, Setting.USE_TEXT.node(), "%hero% has deconstructed a %item%").replace("%hero%", "$1").replace("%item%", "$2"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (args.length > 0) {
            if (args[0].toLowerCase().equals("list")) {
                Set<String> items = new HashSet<String>(getSettingKeys(hero.getHeroClass()));
                items.remove("require-workbench");
                for (Setting set : Setting.values()) {
                    items.remove(set.node());
                }
                Messaging.send(player, "You can deconstruct these items: " + items.toString());
                return false;
            } else {
                Messaging.send(player, getUsage());
                return false;
            }
        }

        if (player.getTargetBlock(null, 3).getType() != Material.WORKBENCH && getSetting(hero.getHeroClass(), "require-workbench", true)) {
            Messaging.send(player, "You must have a workbench targetted to deconstruct an item!");
            return false;
        }

        ItemStack item = player.getItemInHand();
        if (item.getType() == Material.AIR) {
            Messaging.send(player, "You must be holding the item you wish to deconstruct!");
            return false;
        }

        String matName = item.getType().name();
        if (!getSettingKeys(hero.getHeroClass()).contains(matName)) {
            Messaging.send(player, "Found Keys: " + getSettingKeys(hero.getHeroClass()).toString());
            Messaging.send(player, "You can't deconstruct that item!");
            return false;
        }

        int level = getSetting(hero.getHeroClass(), matName + "." + Setting.LEVEL.node(), 1);
        if (level > hero.getLevel()) {
            Messaging.send(player, "You must be level " + level + " to deconstruct that item!");
            return false;
        }

        if (item.getType().getMaxDurability() > 16) {
            double minDurability = item.getType().getMaxDurability() * (1D - getSetting(hero.getHeroClass(), matName + ".min-durability", .5));
            if (item.getDurability() > minDurability) {
                Messaging.send(player, "The item is too damaged to deconstruct!");
                return false;
            }
        }

        List<String> returned = getSettingKeys(hero.getHeroClass(), matName);
        if (returned == null) {
            Messaging.send(player, "Unable to deconstruct that item!");
            return false;
        }

        for (String s : returned) {
            if (s.equals("min-durability") || s.equals(Setting.LEVEL.node()) || s.equals(Setting.EXP.node()))
                continue;

            Material m = Material.matchMaterial(s);
            if (m == null) {
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad item definition " + s);
            }
            int amount = getSetting(hero.getHeroClass(), matName + "." + s, 1);
            if (amount < 1) {
                throw new IllegalArgumentException("Error with skill " + getName() + ": bad amount definition for " + s + ": " + amount);
            }

            ItemStack stack = new ItemStack(m, amount);
            Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(stack);
            //Just dump any leftover stacks onto the ground
            if (!leftOvers.isEmpty()) {
                for(ItemStack leftOver : leftOvers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
                }
            }
        }

        player.getInventory().removeItem(new ItemStack(player.getItemInHand().getType(), 1));
        player.updateInventory();
        
        //Grant the hero experience
        int xp = getSetting(hero.getHeroClass(), matName + "." + Setting.EXP.node(), 0);
        if ( xp > 0) {
            hero.gainExp(xp, ExperienceType.CRAFTING);
        }
        
        broadcast(player.getLocation(), getUseText(), new Object[] { player.getDisplayName(), matName.toLowerCase().replace("_", " ") });
        return true;
    }

}
