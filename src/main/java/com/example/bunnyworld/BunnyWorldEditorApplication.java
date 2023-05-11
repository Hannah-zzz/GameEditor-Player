package com.example.bunnyworld;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;

import com.example.bunnyworld.dao.Model;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;

import java.util.List;

public class BunnyWorldEditorApplication extends Application {

    private SQLiteDatabase db;

    private static Shape copiedShape;
    private static List<Page> copiedPages;

    public static Model model;

    @Override
    public void onCreate() {
        super.onCreate();

        // set up database - db can only be created in activities
        db = openOrCreateDatabase("Games", MODE_PRIVATE, null);
        model = Model.getInstance();
        model.setupDatabase(db);
    }

    public static void setCopiedShape(Shape copiedShape) {
        BunnyWorldEditorApplication.copiedShape = copiedShape;
    }

    public static Shape getCopiedShape() {
        return copiedShape;
    }

    public static void setCopiedPages(List<Page> copiedPages) {
        BunnyWorldEditorApplication.copiedPages = copiedPages;
    }

    public static List<Page> getCopiedPages() {
        return copiedPages;
    }

}
