package com.example.bunnyworld.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Game implements Serializable {
    private HashSet<String> shapeNames;

    private HashSet<String> pageNames;
    private String name;
    private List<Page> pageList;

    private int indX = 1;
    private boolean checked;

    private boolean internalValid = true;

    private List<Shape> inventory;

    public Game(String name) {
        this.name = name;
        this.checked = false;
        this.pageList = new ArrayList<>();
        Page startPage = new Page("Page1");
        //startPage.setStartPage();
        this.pageList.add(startPage);
        this.shapeNames = new HashSet<>();
        this.pageNames = new HashSet<>();
        this.inventory = new ArrayList<>(); //Jenny

    }

    public Game(String name, List<Page> pageList, int indX,HashSet<String> shapeNames,HashSet<String> pageNames){
        this.name = name;
        this.indX = indX;
        this.pageList = pageList;
        this.shapeNames = shapeNames;
        this.pageNames = pageNames;
        this.inventory = new ArrayList<>(); //Jenny

    }
    public Game(String name, List<Page> pageList, HashSet<String> shapeNames, HashSet<String> pageNames){
        this.name = name;
        //this.indX = indX;
        this.pageList = pageList;
        this.shapeNames = shapeNames;
        this.pageNames = pageNames;
        this.inventory = new ArrayList<>(); //Jenny

    }

    public Game(String name, boolean checked, List<Page> pageList, int indX){
        this.name = name;
        this.checked = checked;
        this.pageList = pageList;
        this.indX = indX;
        shapeNames = new HashSet<>();
        pageNames = new HashSet<>();
    }

    public void addBlankPage(){
        int newIndex = indX + 1;
        Page newBlankPage = new Page("Page" + newIndex);
        while (hasDuplicate(newBlankPage.getName())) {
            newIndex++;
            newBlankPage.setName("Page" + newIndex);
        }
        pageList.add(newBlankPage);
        pageNames.add(newBlankPage.getName());
        this.indX = newIndex;
    }

    // This API can only be called when pasting page to game
    public void addPageForPaste(Page page) {
        // generate page name first
        int i = 1;
        String baseName = "Page";
        while (hasDuplicate(baseName + i)) {
            i++;
        }
        page.setName(baseName + i);

        pageList.add(page);
        pageNames.add(baseName + i);

    }

    // This API will update the page
    public void updatePage(Page tgtPage) {
        for (int i = 0; i < pageList.size(); i++) {
            if (pageList.get(i).getName().equals(tgtPage.getName())) {
                pageList.set(i, tgtPage);
                break;
            }
        }
    }

    public String getName() {
        return name;
    }

    public boolean setName(String name) {
            this.name = name;
            return true;
    }

    private boolean hasDuplicate(String name){
        for(String s:shapeNames){
            if(s.equalsIgnoreCase(name)){
                return true;
            }
        }
        for (String p: pageNames){
            if(p.equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }

    public boolean addPageToGame(Page page){
        if(hasDuplicate(page.getName())){
            return false;
        }
        this.indX++;
        pageList.add(page);
        pageNames.add(page.getName());
        return true;
    }

    public void addShapeToPage(Shape shape, Page page){
        if(hasDuplicate(shape.getName()) || !validName(shape.getName())){
            // give this shape a valid new name
            int i = 1;
            shape.setName("shape" + i);
            while (hasDuplicate(shape.getName())) {
                i++;
                shape.setName("shape" + i);
            }
        }
        // can be sure that shape name valid and not duplicate
        page.addShape(shape);
        shapeNames.add(shape.getName());
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean isChecked) {
        checked = isChecked;
    }

    public List<Page> getPageList() {
        return pageList;
    }

    public void deletePage(String name) {
        for (Page s : pageList) {
            if (Objects.equals(s.getName(), name)) {
                pageList.remove(s);
            }
        }
    }

    public void deleteCheckedPages(){
        int i = 0;
        while (i < pageList.size()){
            if(pageList.get(i).isChecked()){
                Page page = pageList.remove(i);
                // should also remove name from set
                pageNames.remove(page.getName());
            }else{
                i++;
            }
        }
    }

    public boolean setNameForPage(String newName, Page page){
        String prevName = page.getName();
        page.setName(newName);
        if(!hasDuplicate(page.getName()) && validName(newName)) {
            pageNames.add(newName);
            // should remove previous name from set
            pageNames.remove(prevName);
            return true;
        }else{
            page.setName(prevName);
            return false;
        }
    }

    public boolean setNameForShape(String newName, Shape shape){
        String prevName = shape.getName();
        shape.setName(newName);
        if(!hasDuplicate(shape.getName()) && validName(newName)) {
            shapeNames.add(newName);
            // should remove previous name from set
            shapeNames.remove(prevName);
            return true;
        }else{
            shape.setName(prevName);
            return false;
        }
    }

    public void resetNameForAllShapesInPageAtPaste(Page page) {
        for (Shape shape : page.getShapeList()) {
            int i = 1;
            String baseName = "shape";
            while (hasDuplicate(baseName + i)) {
                i++;
            }
            shape.setName(baseName + i);
            shapeNames.add(baseName + i);
        }
    }

    public void pasteShapeToPage(Shape shape, Page page) {
        // generate none duplicate shape name first
        int i = 1;
        String baseName = "shape";
        while (hasDuplicate(baseName + i)) {
            i++;
        }
        shape.setName(baseName + i);

        addShapeToPage(shape, page);
    }

    public void deleteShapeFromPage(Shape shape, Page page) {
        // remove shape
        List<Shape> shapeList = page.getShapeList();
        for (int i = 0; i < shapeList.size(); i++) {
            if (shapeList.get(i).getName().equals(shape.getName())) {
                shapeList.remove(i);
                break;
            }
        }
        // remove name from set
        shapeNames.remove(shape.getName());
    }

    private boolean validName(String name) {

        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == ' ' || name.charAt(i) == ';') {
                return false;
            }
        }

        return true;
    }

    public Page getPage(String pageName) {
        for (Page page : pageList) {
            if (page.getName().equals(pageName)) {
                return page;
            }
        }
        return null;
    }

    public String[] getPageNameArrayWithoutPage(Page page) {
        String[] pageNameArrayWithoutPage = new String[pageList.size() - 1];
        int k = 0;
        for (int i = 0; i < pageList.size(); i++) {
            String curPageName = pageList.get(i).getName();
            if (curPageName.equals(page.getName())) continue;
            pageNameArrayWithoutPage[k++] = curPageName;
        }
        return pageNameArrayWithoutPage;
    }

    // This function returns all names of the shapes if the inputs are all null
    // or the name array without the input shape at its page
    public String[] getShapeNameArrayWithoutShapeAtPage(Shape shape, Page page) {
        List<String> list = new ArrayList<>();
        for (Page curPage : pageList) {
            String[] curArray;
            if (shape != null && page != null && curPage.getName().equals(page.getName())) {
                curArray = curPage.getShapeNameArrayWithoutShape(shape);
            } else {
                curArray = curPage.getShapeNameArrayWithoutShape(null);
            }
            for (String shapeName : curArray) {
                list.add(shapeName);
            }
        }
        // turn array to list
        String[] array = new String[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public boolean isInternalValid() {
        return internalValid;
    }

    public void checkInternalState() {
        for (Page p : pageList) {
            boolean pageState = checkPageInternalState(p);
            p.setInternalState(pageState);
            if (!pageState) internalValid = false;
        }
    }

    private boolean checkPageInternalState(Page p) {
        for (Shape s : p.getShapeList()) {
            List<Set<String>> names = s.mustExist();
            for (String shapeName : names.get(0)) {
                if (!shapeNamesContains(shapeName)) {
                    return false;
                }
            }
            for (String pageName : names.get(1)) {
                if (!pageNamesContains(pageName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean pageNamesContains(String pageName) {
        for (String pn : pageNames) {
            if (pn.equalsIgnoreCase(pageName)) return true;
        }
        return false;
    }

    private boolean shapeNamesContains(String shapeName) {
        for (String sn : shapeNames) {
            if (sn.equalsIgnoreCase(shapeName)) return true;
        }
        return false;
    }

    public void updateAllShapeScripts(String src, String tgt) {
        for (Page page : pageList) {
            for (Shape shape : page.getShapeList()) {
                shape.updateScript(src, tgt);
            }
        }
    }

    public int getIndex() {
        return indX;
    }

    // Moves a Shape s from Page p into the inventory at the location specified by the top-left corner
    // The shape should be movable and visible
    // The return value indicates whether the move was successful
    public boolean pageToInventory(Shape s, Page p, int left, int top) {
        if (!s.getMovable() || !s.getVisible()) return false;
        if (!p.getShapeList().contains(s)) return false;
        p.getShapeList().remove(s);
        inventory.add(s);
        s.setRect(left, top, s.getRect().get(2), s.getRect().get(3));
        return true;
    }

    // Moves a Shape s from inventory to Page p at the location specified by the top-left corner
    // The shape should be movable and visible
    // The return value indicates whether the move was successful
    public boolean inventoryToPage(Shape s, Page p) {
        if (!s.getMovable() || ! s.getVisible()) return false;
        if (!inventory.contains(s)) return false;
        p.getShapeList().add(s);
        inventory.remove(s);
        //s.setRect(left, top, s.getRect().get(2), s.getRect().get(3));
        return true;
    }

    public boolean addToInventory(Shape s, Page p) {
        if (!p.shapeList.contains(s)) return false;
        p.shapeList.remove(s);
        inventory.add(s);
        return true;
    }

    public List<Shape> getInventory() {
        return inventory;
    }



}