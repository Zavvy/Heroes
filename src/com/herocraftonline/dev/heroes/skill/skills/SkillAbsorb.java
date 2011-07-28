package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillData;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillAbsorb extends ActiveSkill {

    public SkillAbsorb(Heroes plugin) {
        super(plugin, "Absorb");
        setDescription("Converts all damage into mana");
        setUsage("/skill absorb");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill absorb" });
    }

    @Override
    public void init() {
        registerEvent(Type.ENTITY_DAMAGE, new SkillEntityListener(), Priority.Normal);
    }

    @Override
    public boolean use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        hero.addEffect(new AbsorbEffect(this));
        return true;
    }

    public class AbsorbEffect extends Effect {

        public AbsorbEffect(Skill skill) {
            super(skill, "Absorb");
        }

        @Override
        public void apply(Hero hero) {
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getName(), SkillData.APPLYTEXT, "%target% is absorbing damage!");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            Player player = hero.getPlayer();
            String message = hero.getHeroClass().getSkillData(getName(), SkillData.UNAPPLYTEXT, "Absorb faded from %target%");
            broadcast(player.getLocation(), message, player.getDisplayName());
        }

    }

    public class SkillEntityListener extends EntityListener {

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled())
                return;

            Entity defender = event.getEntity();
            if (defender instanceof Player) {
                Player player = (Player) defender;
                Hero hero = getPlugin().getHeroManager().getHero(player);
                if (hero.hasEffect("Absorb")) {
                    int absorbAmount = hero.getHeroClass().getSkillData(getName(), "mana-amount", 20); 
                    event.setDamage((int) (event.getDamage() * 0.50));
                    if (hero.getMana() + absorbAmount > 100) {
                        hero.removeEffect(hero.getEffect("Absorb"));
                    } else {
                        hero.setMana(hero.getMana() + absorbAmount);
                        if (hero.isVerbose()) {
                            Messaging.send(hero.getPlayer(), ChatColor.BLUE + "MANA " + Messaging.createManaBar(hero.getMana()));
                        }
                    }
                }
            }
        }
    }
}
