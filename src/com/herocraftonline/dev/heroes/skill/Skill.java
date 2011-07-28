package com.herocraftonline.dev.heroes.skill;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.command.BasicCommand;
import com.herocraftonline.dev.heroes.persistence.HeroManager;
import com.herocraftonline.dev.heroes.util.Messaging;

/**
 * The root class of the skill heirarchy. This class implements the basic functionality of every Heroes skill including
 * configuration handling, area-based player notifications and event registration. Because this class extends
 * {@link com.herocraftonline.dev.heroes.command.BaseCommand}, the constructor of every skill should define a name,
 * description, usage, min and max arguments and at least one identifier. Any registered events must provide an event
 * listener, usually created as an inner class.
 * </br>
 * </br>
 * <b>Skill Framework:</b>
 * <ul>
 * <li>{@link ActiveSkill}</li>
 * <ul>
 * <li>{@link TargettedSkill}</li>
 * </ul>
 * <li>{@link PassiveSkill}</li> <li>{@link OutsourcedSkill}</li> </ul>
 * <b>Note:</b> All skill identifiers <i>must</i> begin with <i>skill</i>, e.g. "skill fireball".
 */
public abstract class Skill extends BasicCommand {

    private final Heroes plugin;
    private SkillData data;

    /**
     * The constructor of every skill must define:
     * <ul>
     * <li><code>name</code></li>
     * <li><code>description</code></li>
     * <li><code>usage</code></li>
     * <li><code>minArgs</code></li>
     * <li><code>maxArgs</code></li>
     * <li><code>identifiers</code></li>
     * <li><code>notes</code> (optional)</li>
     * </ul>
     * 
     * @param plugin
     *            the active Heroes instance
     */
    public Skill(Heroes plugin, String name) {
        super(name);
        this.plugin = plugin;
    }

    public final Heroes getPlugin() {
        return plugin;
    }

    public void broadcast(Location source, String message, String user) {
        broadcast(source, message, user, null);
    }

    /**
     * Helper method that broadcasts a message to all players within 30 blocks of the specified source. These messages
     * can be suppressed by players on an individual basis.
     * 
     * @param source
     *            the <code>Location</code> to measure from
     * @param message
     *            the content of the message
     * @param args
     *            any text in the message of the format $<i>n</i> where <i>n</i>
     *            is an integer will be replaced with the <i>n</i>th element of
     *            this array
     */
    public void broadcast(Location source, String message, String user, String target) {
        if (message.isEmpty()) {
            return;
        }

        message = message.replace("%hero%", "$1").replace("%skill%", "$2").replace("%target%", "$3");
        
        Player[] players = plugin.getServer().getOnlinePlayers();
        HeroManager heroManager = plugin.getHeroManager();
        for (Player player : players) {
            if (heroManager.getHero(player).isSuppressing(this)) {
                continue;
            }
            Location location = player.getLocation();
            if (source.getWorld().equals(location.getWorld())) {
                if (location.distance(source) < 30) {
                    Messaging.send(player, message, user, getName(), target);
                }
            }
        }
    }

    /**
     * The end of the execution path of a skill, this method is called whenever a command with a registered identifier
     * is used.
     * 
     * @param sender
     *            the <code>CommandSender</code> issuing the command
     * @param args
     *            the arguments provided with the command
     */
    @Override
    public abstract boolean execute(CommandSender sender, String identifier, String[] args);

    /**
     * An initialization method called after all configuration data is loaded.
     */
    public abstract void init();

    /**
     * Sets the configuration containing all settings related to the skill. This should only be used by the skill loader
     * in most cases.
     * 
     * @param config
     *            the new skill configuration
     */
    public void setData(SkillData data) {
        this.data = data;
    }

    public SkillData getData() {
        return data;
    }

    /**
     * Helper method to make registering an event a little easier.
     * 
     * @param type
     *            the type of event
     * @param listener
     *            the listener used to handle the event
     * @param priority
     *            the priority given to the event handler
     */
    protected void registerEvent(Type type, Listener listener, Priority priority) {
        plugin.getServer().getPluginManager().registerEvent(type, listener, priority, plugin);
    }

    @Override
    public boolean isShownOnHelpMenu() {
        return false;
    }

}
