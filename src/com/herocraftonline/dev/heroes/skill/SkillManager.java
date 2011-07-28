package com.herocraftonline.dev.heroes.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SkillManager {

    private LinkedHashMap<String, Skill> skills;

    public SkillManager() {
        skills = new LinkedHashMap<String, Skill>();
    }

    public void addSkill(Skill command) {
        skills.put(command.getName().toLowerCase(), command);
    }

    public void removeSkill(Skill command) {
        skills.remove(command);
    }

    public Skill getSkill(String name) {
        return skills.get(name.toLowerCase());
    }
    
    public List<Skill> getSkills() {
        return new ArrayList<Skill>(skills.values());
    }
    
}
