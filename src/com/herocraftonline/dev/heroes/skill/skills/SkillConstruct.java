package com.herocraftonline.dev.heroes.skill.skills;

import java.util.ArrayList;
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

public class SkillConstruct extends ActiveSkill {

    public SkillConstruct(Heroes plugin) {
        super(plugin, "Construct");
        setDescription("Constructs an object from materials.");
        setUsage("/skill deconstruct [item|list]");
        setArgumentRange(1, 2);
        setIdentifiers(new String[] { "skill construct" });
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        String root = "IRON_AXE";
        node.setProperty("require-workbench", true);
        node.setProperty(root + "." + Setting.AMOUNT.node(), 1);
        node.setProperty(root + "." + Setting.LEVEL.node(), 1);
        node.setProperty(root + "." + Setting.EXP.node(), 0);
        node.setProperty(root + ".IRON_INGOT", 1);
        node.setProperty(root + ".STICK", 1);
        node.setProperty(Setting.USE_TEXT.node(), "%hero% has constructed a %item%");
        return node;
    }

    @Override
    public void init() {
        super.init();
        setUseText(getSetting(null, Setting.USE_TEXT.node(), "%hero% has constructed a %item%").replace("%hero%", "$1").replace("%item%", "$2"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        //List all items this hero can make with construct
        if (args[0].toLowerCase().equals("list")) {
            Set<String> items = new HashSet<String>(getSettingKeys(hero.getHeroClass()));
            items.remove("require-workbench");
            for (Setting set : Setting.values()) {
                items.remove(set.node());
            }
            Messaging.send(player, "You can craft these items: " + items.toString());
            return false;
        } else if (args[0].toLowerCase().equals("info")) {
            //TODO: List construction information for this item
            return false;
        }
        
        if (player.getTargetBlock(null, 3).getType() != Material.WORKBENCH && getSetting(hero.getHeroClass(), "require-workbench", true)) {
            Messaging.send(player, "You must have a workbench targetted to construct an item!");
            return false;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            Messaging.send(player, "You need at least 1 free inventory spot to construct an item!");
            return false;
        }

        String matName = args[0];
        if (!getSettingKeys(hero.getHeroClass()).contains(matName)) {
            Messaging.send(player, "Found Keys: " + getSettingKeys(hero.getHeroClass()).toString());
            Messaging.send(player, "You can't construct that item!");
            return false;
        }

        int level = getSetting(hero.getHeroClass(), matName + "." + Setting.LEVEL.node(), 1);
        if (level > hero.getLevel()) {
            Messaging.send(player, "You must be level " + level + " to construct that item!");
            return false;
        }

        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            throw new IllegalArgumentException("Invalid Material definition for skill construct: " + matName);
        }

        List<String> returned = getSettingKeys(hero.getHeroClass(), matName);
        if (returned == null) {
            Messaging.send(player, "Unable to construct that item!");
            return false;
        }

        List<ItemStack> items = new ArrayList<ItemStack>();
        for (String s : returned) {
            if (s.equals(Setting.LEVEL.node()) || s.equals(Setting.EXP.node()) || s.equals(Setting.AMOUNT.node()))
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
            if (!hasReagentCost(player, stack)) {
                Messaging.send(player, "You don't have all the materials to construct that! Missing " + amount + " " + s);
                return false;
            }
            items.add(stack);
        }
        //Remove the item costs from the player
        player.getInventory().removeItem(items.toArray(new ItemStack[0]));
        int amount = getSetting(hero.getHeroClass(), matName + "." + Setting.AMOUNT.node(), 1);
        Map<Integer, ItemStack> leftOvers = player.getInventory().addItem(normalizeItemStack(mat, amount));
        
        //Drop any leftovers we couldn't add to the players inventory
        for (ItemStack leftOver : leftOvers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
        }
        player.updateInventory();
        
        //Grant the hero experience
        int xp = getSetting(hero.getHeroClass(), matName + "." + Setting.EXP.node(), 0);
        if ( xp > 0) {
            hero.gainExp(xp, ExperienceType.CRAFTING);
        }
        
        broadcast(player.getLocation(), getUseText(), new Object[] { player.getDisplayName(), matName.toLowerCase().replace("_", " ") });
        return true;
    }

    private ItemStack[] normalizeItemStack(Material mat, int amount) {
        List<ItemStack> items = new ArrayList<ItemStack>(); 
        while (amount > 0) {
            if (amount > mat.getMaxStackSize()) {
                items.add(new ItemStack(mat, mat.getMaxStackSize()));
                amount -= mat.getMaxStackSize();
            } else {
                items.add(new ItemStack(mat, amount));
                amount = 0;
            }
        }
        return items.toArray(new ItemStack[0]);
    }
}
