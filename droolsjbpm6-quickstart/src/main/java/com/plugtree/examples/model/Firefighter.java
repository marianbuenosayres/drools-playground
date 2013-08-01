package com.plugtree.examples.model;

public class Firefighter {

    private String name;

    public Firefighter() {
    }

    public Firefighter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return "Firefighter[name= '"+name+"']";
    }
}

