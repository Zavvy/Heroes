package com.herocraftonline.dev.heroes.skill.skills;

import java.util.Set;

import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.Beneficial;
import com.herocraftonline.dev.heroes.effects.Dispellable;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.Harmful;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillDispel extends TargettedSkill {

    public SkillDispel(Heroes plugin) {
        super(plugin, "Dispel");
        setDescription("Removes all magical effects from your target");
        setUsage("/skill dispel");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill dispel" });
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("max-removals", 3);
        return node;
    }

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        boolean removed = false;
        int maxRemovals = getSetting(hero.getHeroClass(), "max-removals", 3);
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            // if player is targetting itself
            if (targetPlayer.equals(player)) {
                for (Effect effect : hero.getEffects()) {
                    if (effect instanceof Dispellable && effect instanceof Harmful) {
                        hero.removeEffect(effect);
                        removed = true;
                        maxRemovals--;
                        if (maxRemovals == 0)
                            break;
                    }
                }
            } else {
                Hero targetHero = getPlugin().getHeroManager().getHero(targetPlayer);
                boolean removeHarmful = false;
                if (hero.hasParty()) {
                    // If the target is a partymember lets make sure we only remove harmful effects
                    if (hero.getParty().isPartyMember(targetHero)) {
                        removeHarmful = true;
                    }
                }
                for (Effect effect : targetHero.getEffects()) {
                    if (effect instanceof Dispellable) {
                        if (removeHarmful && effect instanceof Harmful) {
                            targetHero.removeEffect(effect);
                            removed = true;
                            maxRemovals--;
                            if (maxRemovals == 0)
                                break;
                        } else if (!removeHarmful && effect instanceof Beneficial) {
                            targetHero.removeEffect(effect);
                            removed = true;
                            maxRemovals--;
                            if (maxRemovals == 0)
                                break;
                        }
                    }
                }
            }
        } else if (target instanceof Creature) {
            Set<Effect> cEffects = getPlugin().getHeroManager().getCreatureEffects((Creature) target);
            if (cEffects != null) {
                boolean removeHarmful = false;
                if (hero.getSummons().contains(target)) {
                    removeHarmful = true;
                }
                for (Effect effect : cEffects) {
                    if (effect instanceof Dispellable) {
                        if (removeHarmful && effect instanceof Harmful) {
                            getPlugin().getHeroManager().removeCreatureEffect((Creature) target, effect);
                            removed = true;
                            maxRemovals--;
                            if (maxRemovals == 0)
                                break;
                        } else if (!removeHarmful && effect instanceof Beneficial) {
                            getPlugin().getHeroManager().removeCreatureEffect((Creature) target, effect);
                            removed = true;
                            maxRemovals--;
                            if (maxRemovals == 0)
                                break;
                        }
                    }
                }
            }
        } else {
            Messaging.send(player, "That is not a valid target!");
            return false;
        }

        if (removed) {
            broadcastExecuteText(hero, target);
            return true;
        }
        Messaging.send(player, "The target has nothing to dispel!");
        return false;
    }

}
