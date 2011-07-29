package com.herocraftonline.dev.heroes.skill.skills;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
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

public class SkillConfuse extends TargettedSkill {

    private static final Random random = new Random();

    public SkillConfuse(Heroes plugin) {
        super(plugin, "Confuse");
        setDescription("Confuses your target");
        setUsage("/skill confuse <target>");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill confuse" });
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

        broadcastExecuteText(hero, target);

        HeroClass heroClass = hero.getHeroClass();
        long duration = heroClass.getSkillData(this, "duration", 10000);
        long period = heroClass.getSkillData(this, "period", 2000);
        float maxDrift = (float) heroClass.getSkillData(this, "max-drift", 0.35f);
        targetHero.addEffect(new ConfuseEffect(this, duration, period, maxDrift));
        return true;
    }

    public class ConfuseEffect extends PeriodicEffect implements Periodic, Expirable {

        private final float maxDrift;

        public ConfuseEffect(Skill skill, long duration, long period, float maxDrift) {
            super(skill, "Confuse", period, duration);
            this.maxDrift = maxDrift;
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getSkill(), SkillData.APPLYTEXT, "%hero% is confused!");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getSkill(), SkillData.UNAPPLYTEXT, "%hero% has regained his wit!");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);

            Player player = hero.getPlayer();
            Vector velocity = player.getVelocity();

            float angle = random.nextFloat() * 2 * 3.14159f;
            float xAdjustment = maxDrift * net.minecraft.server.MathHelper.cos(angle);
            float zAdjustment = maxDrift * net.minecraft.server.MathHelper.sin(angle);

            velocity.add(new Vector(xAdjustment, 0f, zAdjustment));
            velocity.setY(0);
            player.setVelocity(velocity);
        }
    }
}
