package com.herocraftonline.dev.heroes.skill.skills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;

public class SkillBlaze extends ActiveSkill {

    public SkillBlaze(Heroes plugin) {
        super(plugin, "Blaze");
        setDescription("Sets everyone around you on fire");
        setUsage("/skill blaze");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill blaze" });
    }

    @Override
    public boolean use(Hero hero, String[] args) {
        int range = hero.getHeroClass().getSkillData(this, "range", 5);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(range, range, range);
        int fireTicks = hero.getHeroClass().getSkillData(this, "fire-length", 3000);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity livingEntity = (LivingEntity) entity;
            EntityDamageEvent damageEvent = new EntityDamageEvent(hero.getPlayer(), DamageCause.ENTITY_ATTACK, 0);
            Bukkit.getServer().getPluginManager().callEvent(damageEvent);
            if (damageEvent.isCancelled()) return false;
            livingEntity.setFireTicks(fireTicks);
        }
        broadcastExecuteText(hero);
        return true;
    }

    @Override
    public void init() {}
}
