package com.herocraftonline.dev.heroes.skill;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.classes.HeroClass.ExperienceType;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.util.Messaging;

/**
 * A skill that performs an action in direct response to a user command. All skill identifiers <i>must</i>
 * begin with <i>skill</i>, e.g. "skill fireball", in order to be recognized. ActiveSkills define four default settings:
 * mana, cooldown, experience and usage text. Mana is deducted and a cooldown is activated when the
 * {@link #use(Hero, String[]) use} method returns <code>true</code>. The {@link #execute(CommandSender, String[])
 * execute} automatically handles class, level, mana and cooldown checks on a player attempting to use a skill and
 * should not be overridden. If all of these checks pass, the <code>use</code> method is called, which should contain
 * the heart of the skill's behavior that is unique to each skill.
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
public abstract class ActiveSkill extends Skill {

    private boolean awardExpOnCast = true;

    /**
     * When defining your own constructor, be sure to assign the name, description, usage, argument bounds and
     * identifier fields as defined in {@link com.herocraftonline.dev.heroes.command.BaseCommand}. Remember that each
     * identifier must begin with <i>skill</i>.
     * 
     * @param plugin
     *            the active Heroes instance
     */
    public ActiveSkill(Heroes plugin, String name) {
        super(plugin, name);
    }

    /**
     * Called whenever a command with an identifier registered to this skill is used. This implementation performs all
     * necessary class, level, mana and cooldown checks. This method should <i>not</i> be overridden unless you really
     * know what you're doing. If all checks pass, this method calls {@link #use(Hero, String[]) use}. If
     * <code>use</code> returns <code>true</code>, this method automatically deducts mana, awards experience and sets a
     * cooldown.
     * 
     * @param sender
     *            the <code>CommandSender</code> issuing the command
     * @param args
     *            the arguments provided with the command
     */
    @Override
    public boolean execute(CommandSender sender, String identifier, String[] args) {
        if (!(sender instanceof Player)) return false;

        String name = this.getName();
        Player player = (Player) sender;
        Hero hero = getPlugin().getHeroManager().getHero(player);
        if (hero == null) {
            Messaging.send(player, "You are not a hero.");
            return false;
        }
        HeroClass heroClass = hero.getHeroClass();
        if (!heroClass.hasSkill(name) && !heroClass.hasSkill("*")) {
            Messaging.send(player, "$1s cannot use $2.", heroClass.getName(), name);
            return false;
        }
        int level = heroClass.getSkillData(this, SkillData.LEVEL, 1);
        if (hero.getLevel() < level) {
            Messaging.send(player, "You must be level $1 to use $2.", String.valueOf(level), name);
            return false;
        }
        int manaCost = heroClass.getSkillData(this, SkillData.MANA, 0);
        if (manaCost > hero.getMana()) {
            Messaging.send(player, "Not enough mana!");
            return false;
        }
        Map<String, Long> cooldowns = hero.getCooldowns();
        long time = System.currentTimeMillis();
        int cooldown = heroClass.getSkillData(this, SkillData.COOLDOWN, 0);
        if (cooldown > 0) {
            Long expiry = cooldowns.get(name);
            if (expiry != null) {
                if (time < expiry) {
                    long remaining = expiry - time;
                    Messaging.send(hero.getPlayer(), "Sorry, $1 still has $2 seconds left on cooldown!", name, remaining / 1000);
                    return false;
                }
            }
        }
        if (use(hero, args)) {
            if (cooldown > 0) {
                cooldowns.put(name, time + cooldown);
            }

            if (this.awardExpOnCast) {
                this.awardExp(hero);
            }

            hero.setMana(hero.getMana() - manaCost);
            if (hero.isVerbose() && manaCost > 0) {
                Messaging.send(hero.getPlayer(), ChatColor.BLUE + "MANA " + Messaging.createManaBar(hero.getMana()));
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * The heart of any ActiveSkill, this method defines what actually happens when the skill is used. See
     * {@link #execute(CommandSender, String[]) execute} for a brief explanation of the execution process.
     * 
     * @param hero
     *            the {@link Hero} using the skill
     * @param args
     *            the arguments provided with the command
     * @return <code>true</code> if the skill executed properly, <code>false</code> otherwise
     */
    public abstract boolean use(Hero hero, String[] args);

    private void awardExp(Hero hero) {
        HeroClass heroClass = hero.getHeroClass();
        if (heroClass.getExperienceSources().contains(ExperienceType.SKILL)) {
            hero.gainExp(heroClass.getSkillData(this, SkillData.EXP, 0), ExperienceType.SKILL);
        }
    }

    protected void broadcastExecuteText(Hero hero) {
        Player player = hero.getPlayer();
        String message = hero.getHeroClass().getSkillData(this, SkillData.USETEXT, "%hero% used %skill%!");
        broadcast(player.getLocation(), message, player.getDisplayName());
    }

}
