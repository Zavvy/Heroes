package com.herocraftonline.dev.heroes.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.herocraftonline.dev.heroes.Heroes;

public class SkillData {

    public static final String LEVEL = "level";
    public static final String MANA = "mana";
    public static final String COOLDOWN = "cooldown";
    public static final String EXP = "exp";
    public static final String USETEXT = "use-text";
    public static final String APPLYTEXT = "apply-text";
    public static final String UNAPPLYTEXT = "unapply-text";
    public static final String MAXDISTANCE = "max-distance";
    
    private final String skillName;
    private Map<String, Object> data = new HashMap<String, Object>();

    public SkillData(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, T defaultValue) {
        if (data.containsKey(key)) {
            try {
                return (T) data.get(key);
            } catch (ClassCastException e) {
                Heroes.log(Level.SEVERE, "Invalid skill data found. (Skill: " + skillName + ", Entry: " + key + ")");
                e.printStackTrace();
            }
        }
        return defaultValue;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void putValue(String key, Object value) {
        data.put(key, value);
    }

    public void putAll(SkillData other) {
        data.putAll(other.data);
    }
    
    public void putAll(Map<String, Object> other) {
        data.putAll(other);
    }
    
    public SkillData clone() {
        SkillData clone = new SkillData(skillName);
        clone.putAll(this);
        return clone;
    }

}