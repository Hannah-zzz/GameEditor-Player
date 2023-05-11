package com.example.bunnyworld.entity;



public class ScriptAction {
	// These are all case insensitive, always use String.equalsIgnoreCase() for comparisons.
    public String action; // goto, play, hide, show
    public String name;

    private String dropName; // This field is only for internal use in the Shape class. You should not be accessing it.

    public ScriptAction(String action, String name, String dropName) {
        this.action = action;
        this.name = name;
        this.dropName = dropName;
    }
    
    public String getDropName() {return dropName;}

	@Override
	public String toString() {
		return "action: " + action + " name: " + name + " dropName: " + dropName;
	}

    //Jenny
    public String getAction() {return this.action;}

    //Jenny
    public String getNameToDoActionOn() {return this.name;}
    
}