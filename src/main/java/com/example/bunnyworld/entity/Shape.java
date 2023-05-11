package com.example.bunnyworld.entity;

import java.io.Serializable;
import java.util.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

enum Event {
    CLICK,
    ENTER,
    DROP;
}

@Entity
public class Shape implements Serializable {

    @NonNull
    @PrimaryKey
    private String name;
    private String imageName;
    private boolean isOriginalImageSize = false;
    private String text;
    private float fontSize;
    private boolean visible;
    private boolean movable;
    private List<Integer> rect;   
    // Construct a Shape either using all parameters above or only two points on the diagonal (name is optional)

    private String script;
    private boolean selected;
    private HashMap<Event, List<ScriptAction>> scriptMap;
    private static int count = 1;

    private String pagename;

    // The copy constructor
    public Shape(Shape source) {
        this(source.name, source.imageName, source.text, source.fontSize, source.visible, source.movable,
                source.rect.get(0), source.rect.get(1), source.rect.get(2), source.rect.get(3),
                source.selected, source.script, source.pagename, true);
        this.setScript(source.script);
    }

    public Shape(int x1, int y1, int x2, int y2) {
    	this("shape" + Shape.count, "", "", (float) 50, true, false, Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
    
    public Shape(String name, int x1, int y1, int x2, int y2) {
    	this(name, "", "", (float) 50, true, false, Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public Shape(String imageName, String text, float fontSize,
                 boolean visible, boolean movable, int left, int top, int width, int height) {
        this("shape" + Shape.count, imageName, text, fontSize, visible, movable, left, top, width, height);
    }

    public Shape(String name, String imageName, String text, float fontSize,
                 boolean visible, boolean movable, int left, int top, int width, int height) {
        this(name, imageName, text, fontSize, visible, movable, left, top, width, height, false, "", "");
    }

    public Shape(String name, String imageName, String text, float fontSize,
                 boolean visible, boolean movable, int left, int top, int width, int height,
                 boolean selected, String script, String pagename) {
        this(name, imageName, text, fontSize, visible, movable, left, top, width, height, selected, script, pagename, false);
    }


    public Shape(String name, String imageName, String text, float fontSize,
                 boolean visible, boolean movable, int left, int top, int width, int height,
                 boolean selected, String script, String pagename, boolean noInc) {
        this.name = name;
        this.imageName = imageName;
        this.text = text;
        this.fontSize = fontSize;
        this.visible = visible;
        this.movable = movable;
        this.rect = new ArrayList<Integer>(Arrays.asList(left, top, width, height));
        this.selected = selected;
        setScript(script);
        this.pagename = pagename;
        if (!noInc) Shape.count++;
    }
    

    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {return imageName;}

    public void setImage(String imageName) {this.imageName = imageName;}

    public String getText() {return text;}

    public void setText(String text) {this.text = text;}

    public float getFontSize() {return fontSize;}

    public void setFontSize(float fontSize) {this.fontSize = fontSize;}

    public boolean getVisible() {return visible;}

    // Use this method to perform the hide and show actions.
    public void setVisible(boolean visible) {this.visible = visible;}

    public boolean getMovable() {return movable;}

    public void setMovable(boolean movable) {this.movable = movable;}

    //returns a list of length 4 in the order: left, top, width, height
    public List<Integer> getRect() {return rect;}
    
    // set top-left corner and width and height
    public void setRect(int left, int top, int width, int height) {
        rect.set(0, left);
        rect.set(1, top);
        rect.set(2, width);
        rect.set(3, height);
    }
    
    // set two points on the diagonal
    public void setDiagonal(int x1, int y1, int x2, int y2) {
        setRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
    
    public boolean getSelected() {return selected;}
    
    public void setSelected(boolean selected) {this.selected = selected;}

    public String getPageName(){
        return pagename;
    }
    
    public String getScript() {return script;}
    
    // @input script: A string with clauses separated by ;. Each clause must start with "on click/enter/drop".
    //                    No ; and space allowed in shape/page names.
    public void setScript(String script) {
    	setScriptHelper(script);
    }

    private void setScriptHelper(String script) {
        this.script = script;
        scriptMap = new HashMap<Event, List<ScriptAction>>();
        String[] clauses = script.split(";");
        for (int i = 0; i < clauses.length - 1; i++) {
            String clause = clauses[i];
            String[] words = clause.split(" ");
            Event curr;
            int start = 2;
            String dropName = "";
            if (words[1].equalsIgnoreCase("click")) curr = Event.CLICK;
            else if (words[1].equalsIgnoreCase("enter")) curr = Event.ENTER;
            else {
                curr = Event.DROP;
                dropName = words[2];
                start = 3;
            }
            if (!scriptMap.containsKey(curr)) {scriptMap.put(curr, new ArrayList<ScriptAction>());}
            List<ScriptAction> currList = scriptMap.get(curr);
            for (int j = start; j < words.length; j+=2) {
                currList.add(new ScriptAction(words[j], words[j+1], dropName));
            }
            scriptMap.put(curr, currList);
        }
    }
    
    // @input event: One of the three events click, enter and drop from the enum.
    // @input dropName: Specifies which shape is dropped onto this. It needs to be the empty string for click and enter.
    // @returns: A list of ScriptAction's that need to be completed. Returns an empty list if no action.
    public List<ScriptAction> handleEvent(Event event, String dropName) {
    	List<ScriptAction> res = new ArrayList<ScriptAction>();
    	if (!scriptMap.containsKey(event)) return res;
    	List<ScriptAction> actionList = scriptMap.get(event);
    	for (ScriptAction a : actionList) {
    		if (dropName.equalsIgnoreCase(a.getDropName())) res.add(a);
    	}
    	return res;
    }


    public List<ScriptAction> handleDrop(String dropName) {
        List<ScriptAction> res = new ArrayList<ScriptAction>();
        if (!scriptMap.containsKey(Event.DROP)) return res;
        List<ScriptAction> actionList = scriptMap.get(Event.DROP);
        for (ScriptAction a : actionList) {
            if (dropName.equalsIgnoreCase(a.getDropName())) res.add(a);
        }
        return res;
    }





    // @input canvas: The canvas it is drawn on.
    // @input strokePaint: The paint for the boarder.
    // @input fillingPaint: The paint for the filling.
    // @returns: void. Draws itself on the canvas.
    public void drawShape(Canvas canvas, Paint strokePaint, Paint fillingPaint, BitmapDrawable drawable) {
    	
        if (!text.isEmpty()) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(fontSize);
            canvas.drawText(text, (float) rect.get(0), (float) rect.get(1) + fontSize, textPaint);
        } else if (!imageName.isEmpty() && drawable != null) {
            canvas.drawBitmap(drawable.getBitmap(), null, new Rect(rect.get(0), rect.get(1), rect.get(0) + rect.get(2),
                    rect.get(1) + rect.get(3)), null);

        } else {
            canvas.drawRect((float) rect.get(0), (float) rect.get(1), (float) rect.get(0) + (float) rect.get(2),
                    (float) rect.get(1) + (float) rect.get(3), strokePaint);
            canvas.drawRect((float) rect.get(0), (float) rect.get(1), (float) rect.get(0) + (float) rect.get(2),
                    (float) rect.get(1) + (float) rect.get(3), fillingPaint);
        }
        if (selected) canvas.drawRect((float) rect.get(0), (float) rect.get(1), (float) rect.get(0) + (float) rect.get(2),
                (float) rect.get(1) + (float) rect.get(3), strokePaint);
    }


    public void outlineShape(Canvas canvas, Paint strokePaint) {
        canvas.drawRect((float) rect.get(0), (float) rect.get(1), (float) rect.get(0) + (float) rect.get(2),
                (float) rect.get(1) + (float) rect.get(3), strokePaint);
    }
    
    
    
    // @input dropName: The name of the shape that is currently being dragged.
    // @returns: True only if this shape can be the target for the shape dropName.
    public boolean isDropTarget(String dropName) {
    	if (!scriptMap.containsKey(Event.DROP)) return false;
    	List<ScriptAction> dropList = scriptMap.get(Event.DROP);
    	for (ScriptAction a : dropList) {
    		if (a.getDropName().equalsIgnoreCase(dropName)) return true;
    	}
    	return false;
    }

    // This method will be used to perform error checking
    // @returns: [set_of_shapes, set_of_pages] of names that must exist in the game
    // TODO: Should I also check sound and image names?
    public List<Set<String>> mustExist() {
        List<Set<String>> res = new ArrayList<Set<String>>();
        res.add(new HashSet<String>());
        res.add(new HashSet<String>());
        if (scriptMap.containsKey(Event.ENTER)) {
            for (ScriptAction a : scriptMap.get(Event.ENTER)) {
                if ("goto".equalsIgnoreCase(a.action)) res.get(1).add(a.name);
                if ("hide".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
                if ("show".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
            }
        }
        if (scriptMap.containsKey(Event.CLICK)) {
            for (ScriptAction a : scriptMap.get(Event.CLICK)) {
                if ("goto".equalsIgnoreCase(a.action)) res.get(1).add(a.name);
                if ("hide".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
                if ("show".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
            }
        }
        if (scriptMap.containsKey(Event.DROP)) {
            for (ScriptAction a : scriptMap.get(Event.DROP)) {
                res.get(0).add(a.getDropName());
                if ("goto".equalsIgnoreCase(a.action)) res.get(1).add(a.name);
                if ("hide".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
                if ("show".equalsIgnoreCase(a.action)) res.get(0).add(a.name);
            }
        }
        return res;
    }

    // Update the script when the name of a shape/page has been changed.
    // @input src: The original name.
    // @input tgt: The new name.
    public void updateScript(String src, String tgt) {
        String updatedScript = script.replaceAll(src, tgt);
        setScript(updatedScript);
    }


    public boolean isOriginalImageSize() {
        return isOriginalImageSize;
    }

    public void setOriginalImageSize(boolean originalImageSize) {
        isOriginalImageSize = originalImageSize;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getLeftCoord() {return rect.get(0);}

    public int getTopCoord() {return rect.get(1);}

    public int getWidth() {return rect.get(2);}

    public int getHeight() {return rect.get(3);}
}
