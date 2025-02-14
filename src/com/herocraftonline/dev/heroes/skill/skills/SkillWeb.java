package com.herocraftonline.dev.heroes.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillWeb extends TargettedSkill {

    private String applyText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillWeb(Heroes plugin) {
        super(plugin, "Web");
        setDescription("Catches your target in a web");
        setUsage("/skill web [target]");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill web" });

        registerEvent(Type.BLOCK_BREAK, new WebBlockListener(), Priority.Highest);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty(Setting.DURATION.node(), 5000); // in milliseconds
        node.setProperty(Setting.APPLY_TEXT.node(), "%hero% conjured a web at %target%'s feet!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%hero% conjured a web at %target%'s feet!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (target.equals(player)) {
            Messaging.send(player, "You need a target!");
            return false;
        }
        String name = "";

        if (target instanceof Player) {
            // Party check before allowing the cast
            if (hero.getParty() != null) {
                if (hero.getParty().isPartyMember(getPlugin().getHeroManager().getHero((Player) target))) {
                    Messaging.send(player, "You need a target!");
                    return false;
                }
            }
            name = ((Player) target).getDisplayName();
        } else if (target instanceof Creature) {
            name = Messaging.getCreatureName((Creature) target).toLowerCase();
        }

        // Damage check
        EntityDamageByEntityEvent damageCheck = new EntityDamageByEntityEvent(player, target, DamageCause.CUSTOM, 0);
        getPlugin().getServer().getPluginManager().callEvent(damageCheck);
        if (damageCheck.isCancelled()) {
            Messaging.send(player, "You can't use that skill here!");
            return false;
        }

        broadcast(player.getLocation(), applyText, new Object[] { player.getDisplayName(), name });
        int duration = getSetting(hero.getHeroClass(), Setting.DURATION.node(), 5000);
        WebEffect wEffect = new WebEffect(this, duration, target.getLocation().getBlock().getLocation());
        // Hero can only have one web effect active at a time - prevents issues with blocks never turning back.
        if (hero.hasEffect("Web")) {
            hero.removeEffect(hero.getEffect("Web"));
        }
        hero.addEffect(wEffect);
        return true;
    }

    public class WebEffect extends ExpirableEffect {

        private List<Location> locations = new ArrayList<Location>();
        private Location loc;

        public WebEffect(Skill skill, long duration, Location location) {
            super(skill, "Web", duration);
            this.loc = location;
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            changeBlock(loc, hero);
            for (BlockFace face : BlockFace.values()) {
                if (face.toString().contains("_") || face == BlockFace.UP || face == BlockFace.DOWN)
                    continue;
                Location blockLoc = loc.getBlock().getRelative(face).getLocation();
                changeBlock(blockLoc, hero);
            }
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }
            locations.clear();
        }

        public Location getLocation() {
            return this.loc;
        }

        private void changeBlock(Location location, Hero hero) {
            Block block = location.getBlock();

            if (block.getType() == Material.WATER || block.getType() == Material.LAVA || block.getType() == Material.SNOW || block.getType() == Material.AIR) {
                changedBlocks.add(location);
                locations.add(location);
                location.getBlock().setType(Material.WEB);
            }
        }
    }

    public class WebBlockListener extends BlockListener {

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled())
                return;

            // Check out mappings to see if this block was a changed block, if so lets deny breaking it.
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}
