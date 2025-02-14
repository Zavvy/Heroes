package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillLayhands extends TargettedSkill {

    public SkillLayhands(Heroes plugin) {
        super(plugin, "Layhands");
        setDescription("Heals the target to full");
        setUsage("/skill layhands [target]");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill layhands" });
    }

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            Messaging.send(hero.getPlayer(), "You need a target!");
            return false;
        }

        Hero targetHero = getPlugin().getHeroManager().getHero((Player) target);

        targetHero.setHealth(targetHero.getMaxHealth());
        targetHero.syncHealth();

        broadcastExecuteText(hero, target);
        return true;
    }
}
