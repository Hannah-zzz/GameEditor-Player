package com.example.bunnyworld.entity;
import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Page implements Serializable {

    private String name;
    ArrayList<Shape> shapeList;
    private boolean checked;
    private Game parent;

    private boolean internalState = true;

    public Page(String name) {
        this.name = name;
        this.checked = false;
        this.shapeList = new ArrayList<>();

    }

    public Page(String name, ArrayList<Shape> list){
        this.name = name;
        this.shapeList = list;

    }

    // Do the deep copy for page
    public Page(Page page) {
        this.name = page.name;
        this.checked = false; // all copied pages should not be checked
        this.parent = page.parent;

        ArrayList<Shape> copiedShapeList = new ArrayList<>();
        for (Shape shape : page.getShapeList()) {
            copiedShapeList.add(new Shape(shape));
        }
        this.shapeList = copiedShapeList;
    }

    public String getName() {
        return name;
    }

    public boolean setName(String name) {
        this.name = name;
        return true;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean isChecked) {
        checked = isChecked;
    }

    public void addShape(Shape shape){
        this.shapeList.add(shape);
    }

    public void deleteShape(Shape shape) {
        for (int i = 0; i < shapeList.size(); i++) {
            if (shapeList.get(i).getName().equals(shape.getName())) {
                shapeList.remove(i);
                break;
            }
        }
    }

    public List<Shape> getShapeList(){
        return shapeList;
    }

    public boolean setNameForShape(String newName, Shape shape){
        for (Shape s :shapeList){
            if(s.getName() == newName){
                return false;
            }
        }
        shape.setName(newName);
        return true;
    }
    private boolean validName(String name){
        for(int i = 0; i < name.length(); i++){
            if(name.substring(i, i+1) == " "){
                return false;
            }
        }
        for(Shape s :shapeList){
            if(s.getName() == name){
                return false;
            }
        }
        return true;
    }

    public Shape getShape(String shapeName) {
        for (Shape shape : shapeList) {
            if (shape.getName().equals(shapeName)) {
                return shape;
            }
        }
        return null;
    }

    // If shapeName == null, will return all names
    public String[] getShapeNameArrayWithoutShape(Shape shape) {
        String[] shapeNameArrayWithoutShape =
                new String[shape == null ? shapeList.size() : shapeList.size() - 1];
        int k = 0;
        for (int i = 0; i < shapeList.size(); i++) {
            String curShapeName = shapeList.get(i).getName();
            if (shape != null && curShapeName.equals(shape.getName())) continue;
            shapeNameArrayWithoutShape[k++] = curShapeName;
        }
        return shapeNameArrayWithoutShape;
    }

    protected void setInternalState(boolean internalState) {
        this.internalState = internalState;
    }

    public boolean getInternalState() {
        return internalState;
    }

}