package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.effects.Expirable;
import com.herocraftonline.dev.heroes.effects.Periodic;
import com.herocraftonline.dev.heroes.effects.PeriodicEffect;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillData;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillBleed extends TargettedSkill {
    public SkillBleed(Heroes plugin) {
        super(plugin, "Bleed");
        setDescription("Causes your target to bleed");
        setUsage("/skill bleed <target>");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill bleed" });
    }

    @Override
    public void init() {}

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            Messaging.send(player, "You need a target!");
            return false;
        }

        Player targetPlayer = (Player) target;
        Hero targetHero = getPlugin().getHeroManager().getHero(targetPlayer);
        if (targetHero.equals(hero)) {
            Messaging.send(player, "You need a target!");
            return false;
        }

        HeroClass heroClass = hero.getHeroClass();
        long duration = heroClass.getSkillData(this, "duration", 10000);
        long period = heroClass.getSkillData(this, "period", 2000);
        int tickDamage = heroClass.getSkillData(this, "tick-damage", 1);
        targetHero.addEffect(new BleedEffect(this, duration, period, tickDamage, player));

        broadcastExecuteText(hero, target);
        return true;
    }

    public class BleedEffect extends PeriodicEffect implements Periodic, Expirable {

        private final Player applier;
        private final int tickDamage;

        public BleedEffect(Skill skill, long duration, long period, int tickDamage, Player applier) {
            super(skill, "Bleed", period, duration);
            this.tickDamage = tickDamage;
            this.applier = applier;
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getSkill(), SkillData.APPLYTEXT, "%hero% is bleeding!");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getSkill(), SkillData.UNAPPLYTEXT, "%hero% has stopped bleeding!");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            Player player = hero.getPlayer();
            getPlugin().getDamageManager().addSpellTarget((Entity) applier);
            player.damage(tickDamage, applier);
        }
    }
}
