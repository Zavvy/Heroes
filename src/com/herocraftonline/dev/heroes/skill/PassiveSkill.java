package com.herocraftonline.dev.heroes.skill;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.ClassChangeEvent;
import com.herocraftonline.dev.heroes.api.LevelEvent;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.persistence.Hero;

/**
 * A skill that provides a passive bonus to a {@link Hero}. The skill's effects are automatically applied when a Hero of
 * the appropriate class reaches the level specified in classes.yml. Because this skill is passive, there is no need to
 * override the {@link #execute(CommandSender, String[]) execute} nor
 * {@link com.herocraftonline.dev.heroes.command.BaseCommand#setUsage(String) use}. Messages displayed when the passive
 * effect is applied or removed are automatically pulled from the configs. By default, the effect applied is simply the
 * name of the skill. This can be changed by overriding {@link #apply(Hero) apply} and {@link #unapply(Hero) unapply}.
 * </br>
 * </br>
 * <b>Skill Framework:</b>
 * <ul>
 * <li>{@link ActiveSkill}</li>
 * <ul>
 * <li>{@link ActiveEffectSkill}</li>
 * <li>{@link TargettedSkill}</li>
 * </ul>
 * <li>{@link PassiveSkill}</li> <li>{@link OutsourcedSkill}</li> </ul>
 */
public abstract class PassiveSkill extends Skill {

    /**
     * Typical skill constructor, except that it automatically sets the usage text to <i>Passive Skill</i>, which should
     * not be changed for normal use. There should be no identifiers defined as a passive skill is not meant to be
     * executed.
     * 
     * @param plugin
     *            the active Heroes instance
     */
    public PassiveSkill(Heroes plugin, String name) {
        super(plugin, name);
        setUsage("Passive Skill");

        registerEvent(Type.CUSTOM_EVENT, new SkillCustomEventListener(), Priority.Monitor);
    }

    /**
     * Serves no purpose for a passive skill.
     */
    @Override
    public boolean execute(CommandSender sender, String identifier, String[] args) {
        return true;
    }

    /**
     * Attempts to apply this skill's effect to the provided {@link Hero} if the it is the correct class and level.
     * 
     * @param hero
     *            the Hero to try applying the effect to
     */
    public void tryApplying(Hero hero) {
        HeroClass heroClass = hero.getHeroClass();
        if (!heroClass.hasSkill(getName())) return;
        if (hero.getLevel() >= heroClass.getSkillData(this, SkillData.LEVEL, 1)) {
            apply(hero);
        } else {
            unapply(hero);
        }
    }

    /**
     * Applies the effect to the provided {@link Hero}.
     * 
     * @param hero
     *            the Hero to apply the effect to
     */
    protected void apply(Hero hero) {
        Effect effect = new Effect(this, getName());
        effect.setPersistent(true);
        hero.addEffect(effect);
        Player player = hero.getPlayer();

        String message = hero.getHeroClass().getSkillData(this, SkillData.APPLYTEXT, "%hero% gained %skill%!");
        broadcast(player.getLocation(), message, player.getDisplayName());
    }

    /**
     * Removes the effect from the provided {@link Hero}.
     * 
     * @param hero
     *            the Hero to remove the effect from
     */
    protected void unapply(Hero hero) {
        if (hero.hasEffect(getName())) {
            hero.removeEffect(hero.getEffect(getName()));
            Player player = hero.getPlayer();

            String message = hero.getHeroClass().getSkillData(this, SkillData.UNAPPLYTEXT, "%hero% lost %skill%!");
            message = message.replace("%hero%", "$1").replace("%skill%", "$2");
            broadcast(player.getLocation(), message, player.getDisplayName(), getName());
        }
    }

    /**
     * Monitors level and class change events and tries to apply or remove the skill's effect when appropriate.
     */
    public class SkillCustomEventListener extends CustomEventListener {

        @Override
        public void onCustomEvent(Event event) {
            if (event instanceof LevelEvent) {
                LevelEvent subEvent = (LevelEvent) event;
                if (!subEvent.isCancelled()) {
                    tryApplying(subEvent.getHero());
                }
            } else if (event instanceof ClassChangeEvent) {
                ClassChangeEvent subEvent = (ClassChangeEvent) event;
                if (subEvent.isCancelled()) {
                    tryApplying(subEvent.getHero());
                }
            }
        }
    }

}
