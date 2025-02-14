package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainHealthEvent;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillGroupHeal extends ActiveSkill {

    public SkillGroupHeal(Heroes plugin) {
        super(plugin, "GroupHeal");
        setDescription("Heals all players around you");
        setUsage("/skill groupheal");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill groupheal", "skill gheal" });
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("heal-amount", 2);
        node.setProperty(Setting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public boolean use(Hero hero, String[] args) {
        int healAmount = getSetting(hero.getHeroClass(), "heal-amount", 2);
        if (hero.getParty() == null) {
            // Heal just the caster if he's not in a party
            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healAmount, this);
            getPlugin().getServer().getPluginManager().callEvent(hrhEvent);
            if (hrhEvent.isCancelled()) {
                Messaging.send(hero.getPlayer(), "Unable to heal the target at this time!");
                return false;
            }
            hero.setHealth(hero.getHealth() + hrhEvent.getAmount());
            hero.syncHealth();
        } else {
            int radiusSquared = (int) Math.pow(getSetting(hero.getHeroClass(), Setting.RADIUS.node(), 5), 2);
            Location heroLoc = hero.getPlayer().getLocation();
            // Heal party members near the caster
            for (Hero partyHero : hero.getParty().getMembers()) {
                if (!hero.getPlayer().getWorld().equals(partyHero.getPlayer().getWorld()))
                    continue;
                if (partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared) {
                    HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(partyHero, healAmount, this);
                    getPlugin().getServer().getPluginManager().callEvent(hrhEvent);
                    if (hrhEvent.isCancelled()) {
                        Messaging.send(hero.getPlayer(), "Unable to heal the target at this time!");
                        return false;
                    }
                    partyHero.setHealth(partyHero.getHealth() + hrhEvent.getAmount());
                    partyHero.syncHealth();
                }
            }
        }

        broadcastExecuteText(hero);
        return true;
    }
}
