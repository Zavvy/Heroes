package com.herocraftonline.dev.heroes.damage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillUseInfo;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.util.Properties;

// import org.bukkit.entity.Projectile;
import com.herocraftonline.dev.heroes.damage.DamageManager.ProjectileType;

public class HeroesDamageListener extends EntityListener {

    private Heroes plugin;
    private DamageManager damageManager;

    private static final Map<Material, Integer> armorPoints;

    static {
        Map<Material, Integer> aMap = new HashMap<Material, Integer>();
        aMap.put(Material.LEATHER_HELMET, 3);
        aMap.put(Material.LEATHER_CHESTPLATE, 8);
        aMap.put(Material.LEATHER_LEGGINGS, 6);
        aMap.put(Material.LEATHER_BOOTS, 3);

        aMap.put(Material.GOLD_HELMET, 3);
        aMap.put(Material.GOLD_CHESTPLATE, 8);
        aMap.put(Material.GOLD_LEGGINGS, 6);
        aMap.put(Material.GOLD_BOOTS, 3);

        aMap.put(Material.CHAINMAIL_HELMET, 3);
        aMap.put(Material.CHAINMAIL_CHESTPLATE, 8);
        aMap.put(Material.CHAINMAIL_LEGGINGS, 6);
        aMap.put(Material.CHAINMAIL_BOOTS, 3);

        aMap.put(Material.IRON_HELMET, 3);
        aMap.put(Material.IRON_CHESTPLATE, 8);
        aMap.put(Material.IRON_LEGGINGS, 6);
        aMap.put(Material.IRON_BOOTS, 3);

        aMap.put(Material.DIAMOND_HELMET, 3);
        aMap.put(Material.DIAMOND_CHESTPLATE, 8);
        aMap.put(Material.DIAMOND_LEGGINGS, 6);
        aMap.put(Material.DIAMOND_BOOTS, 3);
        armorPoints = Collections.unmodifiableMap(aMap);
    }

    public HeroesDamageListener(Heroes plugin, DamageManager damageManager) {
        this.plugin = plugin;
        this.damageManager = damageManager;
    }

    @Override
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = (LivingEntity) event.getEntity();
        CreatureType type = event.getCreatureType();
        Integer maxHealth = damageManager.getCreatureHealth(type);
        if (maxHealth != null) {
            entity.setHealth(maxHealth);
        }
    }

    @Override
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity entity = event.getEntity();
        int amount = event.getAmount();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            Hero hero = plugin.getHeroManager().getHero(player);
            double newHeroHealth = hero.getHealth() + amount;
            int newHealth = (int) (newHeroHealth / hero.getMaxHealth() * 20);
            int newAmount = newHealth - player.getHealth();
            hero.setHealth(newHeroHealth);
            event.setAmount(newAmount);
        }
    }

    private int calculateArmorReduction(PlayerInventory inventory, int damage) {
        ItemStack[] armorContents = inventory.getArmorContents();

        int missingDurability = 0;
        int maxDurability = 0;
        int baseArmorPoints = 0;
        boolean hasArmor = false;

        for (ItemStack armor : armorContents) {
            Material armorType = armor.getType();
            if (armorType != Material.AIR) {
                short armorDurability = armor.getDurability();
                missingDurability += armorDurability;
                maxDurability += armorType.getMaxDurability();
                baseArmorPoints += armorPoints.get(armorType);
                hasArmor = true;
            }
        }

        if (!hasArmor) {
            return 0;
        }

        double armorPoints = (double) baseArmorPoints * (maxDurability - missingDurability) / maxDurability;
        double damageReduction = 0.04 * armorPoints;
        return (int) (damageReduction * damage);
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled())
            return;
        
        if (event.getCause() == DamageCause.SUICIDE) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                plugin.getHeroManager().getHero(player).setHealth(0D);
                return;
            }
        }

        Entity entity = event.getEntity();
        Entity attacker = null;
        DamageCause cause = event.getCause();
        int damage = event.getDamage();
        if (damageManager.getSpellTargets().containsKey(entity)) { // Start of skill -> listener communication
            SkillUseInfo skillInfo = damageManager.getSpellTargets().remove(entity);
            if (event instanceof EntityDamageByEntityEvent) {
                SkillDamageEvent spellDamageEvent = new SkillDamageEvent(damage, entity, skillInfo);
                plugin.getServer().getPluginManager().callEvent(spellDamageEvent);
                if (spellDamageEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                damage = spellDamageEvent.getDamage();
            }
        } else  {
            if (event instanceof EntityDamageByEntityEvent) {
                attacker = ((EntityDamageByEntityEvent) event).getDamager();
                if (attacker instanceof Player) {
                    // Get the damage this player should deal for the weapon they are using
                    damage = getPlayerDamage((Player) attacker, damage);
                } else if (attacker instanceof LivingEntity) {
                    CreatureType type = Properties.getCreatureFromEntity(attacker);
                    if (type != null) {
                        if (type == CreatureType.CREEPER && cause == DamageCause.ENTITY_ATTACK) {
                            // Ghetto fix for creepers throwing two damage events
                            damage = 0;
                            return;
                        } else {
                            Integer tmpDamage = damageManager.getCreatureDamage(type);
                            if (tmpDamage != null) {
                                damage = tmpDamage;
                            }
                        }
                    }
                } else if (attacker instanceof Projectile) {
                    Projectile projectile = (Projectile) attacker;
                    if (projectile.getShooter() instanceof Player) {
                        attacker = projectile.getShooter();
                        // Allow alteration of player damage
                        damage = getPlayerProjectileDamage((Player) projectile.getShooter(), damage, projectile);
                        
                        plugin.debugLog(Level.INFO, "Damage done by projectile from player" + projectile.getShooter() + " of " + damage + " with a " + ProjectileType.valueOf(projectile) );
                    } else {
                        attacker = projectile.getShooter();
                        CreatureType type = Properties.getCreatureFromEntity(projectile.getShooter());
                        if (type != null) {
                            Integer tmpDamage = damageManager.getCreatureDamage(type);
                            if (tmpDamage != null) {
                                damage = tmpDamage;
                            }
                        }
                    }
                }
                // Call the custom event to allow skills to adjust weapon damage
                WeaponDamageEvent weaponDamageEvent = new WeaponDamageEvent(damage, (EntityDamageByEntityEvent) event);
                plugin.getServer().getPluginManager().callEvent(weaponDamageEvent);
                if (weaponDamageEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                damage = weaponDamageEvent.getDamage();

            } else if (cause != DamageCause.CUSTOM) {
                Integer tmpDamage = damageManager.getEnvironmentalDamage(cause);
                if (tmpDamage != null) {
                    damage = tmpDamage;
                    if (cause == DamageCause.FALL) {
                        damage += damage / 3 * (event.getDamage() - 3);
                    }
                }
            }
        } // End of skill -> listener communication

        if (entity instanceof Player) {
            Player player = (Player) entity;
            if ((float) player.getNoDamageTicks() > (float) player.getMaximumNoDamageTicks() / 2.0f || player.isDead() || player.getHealth() <= 0) {
                event.setCancelled(true);
                return;
            }
            final Hero hero = plugin.getHeroManager().getHero(player);
            // Party damage test
            if (attacker instanceof Player) {
                if (hero.getParty() != null) {
                    if (hero.getParty().isPartyMember(plugin.getHeroManager().getHero((Player) attacker))) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            if (damage == 0)
                return;
            int damageReduction = calculateArmorReduction(player.getInventory(), damage);
            damage -= damageReduction;
            if (damage < 0) {
                damage = 0;
            }

            double iHeroHP = hero.getHealth();
            double fHeroHP = iHeroHP - damage;
            // Never set HP less than 0
            if (fHeroHP < 0)
                fHeroHP = 0;

            // Round up to get the number of remaining Hearts
            int fPlayerHP = (int) Math.ceil(fHeroHP / hero.getMaxHealth() * 20);
            plugin.debugLog(Level.INFO, "Damage done to " + player.getName() + " by " + cause + ": " + iHeroHP + " -> " + fHeroHP + "   |   " + player.getHealth() + " -> " + fPlayerHP);

            // TODO: Doing this completely Breaks any form of damage modification from passive/active skills
            hero.setHealth(fHeroHP);

            // If final HP is 0, make sure we kill the player
            if (fHeroHP == 0) {
                event.setDamage(200);
            } else {
                player.setHealth(fPlayerHP + damage);
                event.setDamage(damage + damageReduction);

                // Make sure health syncs on the next tick
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        hero.syncHealth();
                    }
                }, 1);
            }
        } else if (entity instanceof LivingEntity) {
            event.setDamage(damage);
        }
    }

    private int getPlayerProjectileDamage(Player attacker, int damage, Projectile projectile) {
        Integer tmpDamage = damageManager.getProjectileDamage(ProjectileType.valueOf(projectile), attacker);
        return (tmpDamage == null) ? damage : tmpDamage;
    }

    private int getPlayerDamage(Player attacker, int damage) {
        ItemStack weapon = attacker.getItemInHand();
        Material weaponType = weapon.getType();

        Integer tmpDamage = damageManager.getItemDamage(weaponType, attacker);
        return (tmpDamage == null) ? damage : tmpDamage;
    }
}
