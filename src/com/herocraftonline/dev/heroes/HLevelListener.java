package com.herocraftonline.dev.heroes;

import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

import com.herocraftonline.dev.heroes.api.LevelEvent;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class HLevelListener extends CustomEventListener {

    private Heroes plugin;

    public HLevelListener(Heroes heroes) {
        this.plugin = heroes;
    }

    @Override
    public void onCustomEvent(Event event) {
        if (event instanceof LevelEvent) {
            LevelEvent subEvent = (LevelEvent) event;
            Hero hero = subEvent.getHero();
            HeroClass heroClass = hero.getHeroClass();
            hero.syncHealth();

            int level = subEvent.getTo();
            for (Skill skill : plugin.getSkillManager().getSkills()) {
                if (heroClass.hasSkill(skill.getName())) {
                    int levelRequired = skill.getData().getValue("level", 1);
                    if (levelRequired == level) {
                        Messaging.send(subEvent.getHero().getPlayer(), "You have learned $1.", skill.getName());
                    }
                }
            }
        }
    }
}
