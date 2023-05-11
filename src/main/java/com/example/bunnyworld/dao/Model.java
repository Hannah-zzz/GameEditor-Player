package com.example.bunnyworld.dao;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;

import java.util.ArrayList;
import java.util.HashSet;

public class Model {
    public static final int SUCCESSFUL = 0;
    public static final int DUPLICATE_NAME = 1;
    public static final int INVALID_NAME = 2;
    static SQLiteDatabase db;
    private static Model ourInstance = new Model();

    public static Model getInstance() {
        return ourInstance;
    }

    private Model() {}


    public void setupDatabase(SQLiteDatabase db) {
        this.db = db;
        Cursor cursor = db.rawQuery(
                "SELECT * FROM sqlite_master WHERE type='table' AND name='gameList';", null);
        if (cursor.getCount() == 0) {
            db.execSQL("CREATE TABLE gameList" + " (game TEXT, indx INTEGER,  _id INTEGER"
                    + " PRIMARY KEY AUTOINCREMENT);");
        }
    }

    public int isInDB(String gameName){
        ArrayList <String> set = getGamesString();
        for(String str: set){
            if(str.equalsIgnoreCase(gameName)){
                return DUPLICATE_NAME;
            }
        }
        if(!checkValidGameName(gameName)){
            return INVALID_NAME;
        }
        return 0;
    }

    public static ArrayList<String> getGamesString() {
        ArrayList<String> gameNameList = new ArrayList<String>();
        Cursor cursor = db.rawQuery("SELECT game FROM gameList;", null);
        while (cursor.moveToNext()) {
            gameNameList.add(cursor.getString(0));
        }
        return gameNameList;
    }

    public static ArrayList<Game> getGames(){
        ArrayList<Game> gameList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT game, indx FROM gameList;", null);
        while (cursor.moveToNext()) {
            String gamename = cursor.getString(0);
            int indx = cursor.getInt(1);
            ArrayList<Page> pages = getAllPage(gamename);
            HashSet<String> pageN = new HashSet<>();
            HashSet<String> shapeN = new HashSet<>();
            for(Page p:pages){
                pageN.add(p.getName());
                for(Shape s : p.getShapeList()){
                    shapeN.add(s.getName());
                }
            }
            Game game = new Game(gamename,  getAllPage(gamename), indx, shapeN, pageN);
            gameList.add(game);
        }
        return gameList;
    }

    public Game getGame(String gamename){
        Cursor cursor0 = db.rawQuery("SELECT indx FROM gameList WHERE game ='" + gamename + "';", null);
        int indx = 0;
        while (cursor0.moveToNext()){
            indx = cursor0.getInt(0);
        }

        ArrayList<Page> pageList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT pagename FROM " + gamename + "pages" +";", null);
        while (cursor.moveToNext()){
            String pagename = cursor.getString(0);
            pageList.add(getOnePage(pagename, gamename));
        }
        HashSet<String> pageN = new HashSet<>();
        HashSet<String> shapeN = new HashSet<>();
        for(Page p:pageList){
            pageN.add(p.getName());
            for(Shape s : p.getShapeList()){
                shapeN.add(s.getName());
            }
        }
        Game game = new Game(gamename,  getAllPage(gamename), indx, shapeN, pageN);
        return new Game(gamename,getAllPage(gamename), indx, shapeN, pageN);
    }

    private static ArrayList<Page> getAllPage(String gamename){
        ArrayList<Page> pageList = new ArrayList<>();
        String pagedb = gamename + "pages";
        Cursor cursor = db.rawQuery("SELECT DISTINCT pagename FROM " + pagedb +";", null);
        while (cursor.moveToNext()){
            String pagename = cursor.getString(0);
            pageList.add(getOnePage(pagename, gamename));
        }
        return pageList;
    }

    private static Page getOnePage(String pagename, String gamename) {
        ArrayList<Shape> shapeList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + gamename + " WHERE " + "pagename = '" + pagename +"';", null);
        if(cursor.getCount() == 0){
            return new Page(pagename);
        }else {
            while (cursor.moveToNext()) {
                int left = cursor.getInt(0);
                int top = cursor.getInt(1);
                int w = cursor.getInt(2);
                int h = cursor.getInt(3);
                String name = cursor.getString(4);
                String imageName = cursor.getString(5);
                String script = cursor.getString(6);
                String text = cursor.getString(7);
                float fontSize = cursor.getFloat(8);
                boolean movable = cursor.getInt(9) == 1;
                boolean visible = cursor.getInt(10) == 1;
                boolean selected = cursor.getInt(11) == 1;
                shapeList.add(new Shape(name, imageName, text, fontSize,
                        visible, movable, left, top, w, h, selected, script, pagename));
            }
            return new Page(pagename, shapeList);
        }
    }

    private boolean checkValidGameName(String game){
        ArrayList<String> games = getGamesString();
        String gName = game;
        games.remove(gName);
        for(String g:games){
            if(g.equalsIgnoreCase(gName)){
                return false;
            }
        }
        int count = 0;
        if(Character.isDigit(gName.charAt(0))){
            return false;
        }
        for(char ch : gName.toCharArray()){
            if(Character.isDigit(ch)){
                count++;
            }
        }
        if(count == gName.length()){
            return false;
        }
        return true;
    }

    public int addOrUpdateGame(Game game) {


        if (getGamesString().contains(game.getName())) {
            db.execSQL("DROP TABLE IF EXISTS " + game.getName() + ";");
            db.execSQL("DROP TABLE IF EXISTS " + game.getName() + "pages" + ";");
            db.execSQL("DELETE FROM gameList WHERE game = '" + game.getName() + "';");
        }



        db.execSQL("INSERT INTO gameList VALUES ('" + game.getName() + "'," + game.getIndex()  + ",NULL);");

        db.execSQL("CREATE TABLE " + game.getName() + " (left INT, top INT, w INT, h INT, " +
                "name TEXT, imageName TEXT, script TEXT, text TEXT, fontSize FLOAT, movable INTEGER, " +
                "visible INTEGER, selected INTEGER, pagename TEXT, _id INTEGER" +
                " PRIMARY KEY AUTOINCREMENT);");
        String pagedb = game.getName() + "pages";
        db.execSQL("CREATE TABLE " + pagedb + " ( " +
                " pagename TEXT, _id INTEGER" +
                " PRIMARY KEY AUTOINCREMENT);");

        for (Page p : game.getPageList()) {
            db.execSQL("INSERT INTO " + pagedb + " VALUES ('" + p.getName()  + "',NULL);");
            for (Shape s : p.getShapeList()) {
                int movable = s.getMovable() ? 1 : 0;
                int visible = s.getVisible() ? 1 : 0;
                int selected = s.getSelected() ? 1 : 0;
                db.execSQL("INSERT INTO " + game.getName() + " VALUES (" +
                        s.getRect().get(0) + ", " + s.getRect().get(1) + ", "
                        + s.getRect().get(2) + ", " + s.getRect().get(3) + ", '" + s.getName()
                        + "', '" + s.getImage()
                        + "', '" + s.getScript() + "', '" + s.getText()
                        + "', " + s.getFontSize() + ", " + movable
                        + ", " + visible + ", " + selected + ", '" + p.getName() + "', NULL);");
            }

        }
        return SUCCESSFUL;
    }

    public void deleteGame(Game game) {
        if (getGamesString().contains(game.getName())) {
            db.execSQL("DELETE FROM gameList WHERE game = '" + game.getName() + "';");
            db.execSQL("DROP TABLE IF EXISTS " + game.getName() + ";");
            db.execSQL("DROP TABLE IF EXISTS " + game.getName() + "pages" + ";");
        }
    }


}