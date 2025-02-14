package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainManaEvent;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillReplenish extends ActiveSkill {

    public SkillReplenish(Heroes plugin) {
        super(plugin, "Replenish");
        setDescription("Brings your mana back to full");
        setUsage("/skill replenish");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill replenish" });
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("mana-bonus", 100);
        return node;
    }
    
    @Override
    public boolean use(Hero hero, String[] args) {
        int manaBonus = getSetting(hero.getHeroClass(), "mana-bonus", 100);
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaBonus, this);
        getPlugin().getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled())
            return false;
        
        hero.setMana(hrmEvent.getAmount() + hero.getMana());
        Messaging.send(hero.getPlayer(), "Your mana has been replenished!");
        if (hero.isVerbose()) {
            Messaging.send(hero.getPlayer(), Messaging.createManaBar(100));
        }
        broadcastExecuteText(hero);
        return true;
    }

}
