package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;
import static com.example.bunnyworld.utility.StoragePermissionUtil.verifyStoragePermissions;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import com.example.bunnyworld.activity.EditShapeScriptActivity;
import com.example.bunnyworld.R;
import com.example.bunnyworld.databinding.ActivityPlayGameBinding;
//import com.example.bunnyworld.entity.Event;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.ScriptAction;
import com.example.bunnyworld.entity.Shape;
import com.example.bunnyworld.view.PageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayGameActivity extends AppCompatActivity {
    private Game game;
    private Page page;
    private Shape selectedShape; //maybe don't need
    private ActivityPlayGameBinding binding;

    private Context context;
    public static final int INTERNAL_SOUND_COUNT = 7;
    public static final List<String> SOUND_NAME_LIST = new ArrayList<>(Arrays.asList(
            "carrotcarrotcarrot.mp3",
            "evillaugh.mp3",
            "fire.mp3",
            "hooray.mp3",
            "munch.mp3",
            "munching.mp3",
            "woof.mp3")
    );
    public static final Map<String, String> SOUND_NAME_PATH_MAP = new HashMap<>();
    public static final Map<String, Integer> INTERNAL_SOUND_ID_MAP = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_play_game);

        binding = ActivityPlayGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // tricky: get storage access permission
        verifyStoragePermissions(this);

        // get game name from main activity
        String gameName = getIntent().getStringExtra("Game_Name");

        //String pageName = getIntent().getStringExtra("Page_Name");
        this.game = model.getGame(gameName);
        List<Shape> inventory = game.getInventory();
        List<Page> pageList = game.getPageList();

        //page = this.game.getPage("Page1");
        page = pageList.get(0);
        // set label for the current activity based on page name
        if (page != null) {
            this.setTitle(page.getName());
        }
        // initialize selected shape to null
        setSelectedShape(null, false);
        initInternalSoundIDMap();
        // set binding and page for the page view
        binding.pageView.setGame(game);
        binding.pageView.setPage(page);
        binding.pageView.setInventory();
        binding.pageView.setBinding(binding);

    }
    private void eventHandle(Shape curShape, String script){
        String[] words = script.split(" ");
        //System.out.println("here");
        if(words.length != 1) {
            String trigger = words[1];
            //String action = words[2];
            //String destinationOrSoundOrName = words[3];
            if (trigger.compareToIgnoreCase("click") == 0) {
                String action = words[2];
                String destinationOrSoundOrName = words[3];
                onClick = true;
                if(action.compareToIgnoreCase("play") == 0){
                    MediaPlayer mp;
                    String soundName = destinationOrSoundOrName;
                    if (!SOUND_NAME_PATH_MAP.containsKey(soundName)) {
                        // internal
                        mp = MediaPlayer.create(
                                PlayGameActivity.this, INTERNAL_SOUND_ID_MAP.get(soundName));
                    } else {
                        // external
                        mp = new MediaPlayer();
                        try {
                            mp.setDataSource(SOUND_NAME_PATH_MAP.get(soundName));
                            mp.prepare();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    mp.start();

                }
                if (action.compareToIgnoreCase("goto") == 0) {
                    page = game.getPage(destinationOrSoundOrName);
                    //game.set
                    binding.pageView.setPage(page);
                    this.setTitle(page.getName());
                    // page.setName(destinationOrSoundOrName);
                }
                if (action.compareToIgnoreCase("hide") == 0) {
                    for(Page p: game.getPageList()) {
                        for (Shape check : p.getShapeList()) {
                            if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                                check.setVisible(false);
                                break;
                            }
                        }
                    }

                    for(Shape check: game.getInventory()){
                        System.out.println("check 1");

                        if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                            System.out.println("check 2");
                            check.setVisible(false);
                            break;
                        }
                    }
                }
                if (action.compareToIgnoreCase("show") == 0) {
                    for(Shape check: page.getShapeList()){
                        System.out.println(check);
                        System.out.println(destinationOrSoundOrName);
                        if(destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0){
                            check.setVisible(true);
                            break;
                        }
                    }
                    for(Shape check:game.getInventory()){
                        if(destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0){
                            check.setVisible(true);
                            break;
                        }
                    }
                }
            }
        }

    }
    public boolean onClick = false;


    //maybe don't need
    public void setSelectedShape(Shape shape, boolean needCopy) {

        //binding = ActivityPlayGameBinding.inflate(getLayoutInflater());
        //setContentView(binding.getRoot());

        if (shape != null) {
            //look through shapes on page
            for (Shape curShape : page.getShapeList()) {
                onClick = false;
                if (curShape.getName().equals(shape.getName())) {
                    curShape.setSelected(true);
                    String [] scripts = curShape.getScript().split(";");
                    for (String s:scripts) {
                        String [] words = s.split(" ");

                        if(curShape.getVisible() == true && !game.getInventory().contains(curShape) && onClick == false)  eventHandle(curShape, s);
                    }

                } else {
                    curShape.setSelected(false);
                }
            }
            //look through inventory
            for (Shape curShape : game.getInventory()) {
                if (curShape.getName().equals(shape.getName())) {
                    curShape.setSelected(true);
                } else {
                    curShape.setSelected(false);
                }
            }

            this.selectedShape = shape;
        } else {
            for (Shape curShape : page.getShapeList()) {
                curShape.setSelected(false);
            }
            for (Shape curShape : game.getInventory()) {
                curShape.setSelected(false);
            }
            this.selectedShape = null;
        }
        //binding.pageView.invalidate();
    }

    private void initInternalSoundIDMap() {
        int[]IDArray = {R.raw.carrotcarrotcarrot, R.raw.evillaugh, R.raw.fire,
                R.raw.hooray, R.raw.munch, R.raw.munching, R.raw.woof};
        for (int i = 0; i < INTERNAL_SOUND_COUNT; i++) {
            INTERNAL_SOUND_ID_MAP.put(SOUND_NAME_LIST.get(i), IDArray[i]);
        }
    }

}