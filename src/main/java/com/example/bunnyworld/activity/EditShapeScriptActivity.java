package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;
import static com.example.bunnyworld.utility.StoragePermissionUtil.verifyStoragePermissions;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.bunnyworld.R;
import com.example.bunnyworld.databinding.ActivityEditShapeScriptBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;
import com.example.bunnyworld.utility.PathUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditShapeScriptActivity extends AppCompatActivity {

    private String[] pageNameArrayWithoutCurPage;
    private String[] shapeNameArray;
    private String[] shapeNameArrayWithoutCurShape;
    private String shapeName;
    private String script;

    private ActivityEditShapeScriptBinding binding;

    // field to store sound spinner items and the mapping relationship
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

        binding = ActivityEditShapeScriptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // tricky: get storage access permission
        verifyStoragePermissions(this);

        // get data from parent activity
        Intent intent = getIntent();

        // pass the name or the arrays,  not the object
        pageNameArrayWithoutCurPage = intent.getStringArrayExtra("Page_Name_Array_Without_Current_Page");
        shapeNameArray = intent.getStringArrayExtra("Shape_Name_Array");
        shapeNameArrayWithoutCurShape = intent.getStringArrayExtra("Shape_Name_Array_Without_Selected_Shape");
        shapeName = intent.getStringExtra("Shape_Name");
        script = intent.getStringExtra("Script");

        this.setTitle("Script Editor for " + shapeName);

        if (script == null || script.isEmpty()) {
            script = "";
        } else {
            binding.scriptDisplayTextView.setText(script);
        }

        // set listeners for the views
        setTriggerSpinnerListener();
        setActionSpinnerListener();
        setAddScriptListener();
        setClearAllListener();
        setBrowseListener();
        setSoundSpinnerListener();

        // init internal sound ID map
        initInternalSoundIDMap();
    }

    // helper function to set different adapters on page list input source
    private void setAdapterForSpinner(String[] nameArray, Spinner spinner) {
        // sanity check
        if (nameArray == null || nameArray.length == 0) return;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                EditShapeScriptActivity.this, R.layout.spinner_item, nameArray);
        spinner.setAdapter(spinnerAdapter);
    }

    private void setTriggerSpinnerListener() {

        String[] trigger_clause = getResources().getStringArray(R.array.triggerSpinnerClass);

        binding.triggerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                binding.triggerExtendedSpinner.setVisibility(View.INVISIBLE);

                if (position == 2) { // on drop
                    binding.triggerExtendedSpinner.setVisibility(View.VISIBLE);

                    // set adapter
                    setAdapterForSpinner(shapeNameArrayWithoutCurShape, binding.triggerExtendedSpinner);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setActionSpinnerListener() {
        String[] action_clause = getResources().getStringArray(R.array.actionSpinnerClass);

        binding.actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    // goto page
                    case 0:
                        binding.browseSoundButton.setVisibility(View.INVISIBLE);

                        setAdapterForSpinner(pageNameArrayWithoutCurPage, binding.actionTargetSpinner);
                        break;
                    // play sound
                    case 1:
                        // add sound to be the content
                        String[] soundNameArray = new String[SOUND_NAME_LIST.size()];
                        for (int i = 0; i < soundNameArray.length; i++) {
                            soundNameArray[i] = SOUND_NAME_LIST.get(i);
                        }
                        setAdapterForSpinner(soundNameArray, binding.actionTargetSpinner);
                        // enable browse
                        binding.browseSoundButton.setVisibility(View.VISIBLE);
                        break;
                    // hide & show shape
                    case 2:
                    case 3:
                        binding.browseSoundButton.setVisibility(View.INVISIBLE);

                        setAdapterForSpinner(shapeNameArray, binding.actionTargetSpinner);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setAddScriptListener() {

        binding.addScriptButton.setOnClickListener(v -> {
            // get what user has selected
            String trigger = binding.triggerSpinner.getSelectedItem().toString();

            // should deal with if nothing can be selected
            boolean scriptIsInvalid = false;
            String triggerExtension = "";
            // visible == on drop && if null, means no shape I can choose after on drop
            if (binding.triggerExtendedSpinner.getVisibility() == View.VISIBLE &&
                    binding.triggerExtendedSpinner.getSelectedItem() == null) {
                scriptIsInvalid = true;
            } else {
                triggerExtension =
                        binding.triggerExtendedSpinner.getVisibility() == View.INVISIBLE ?
                                "" : binding.triggerExtendedSpinner.getSelectedItem().toString();
            }

            String action = binding.actionSpinner.getSelectedItem().toString();

            // should deal with if nothing can be selected
            String actionTarget = "";
            if (binding.actionTargetSpinner.getSelectedItem() == null) {
                scriptIsInvalid = true;
            } else {
                // sound action target is special
                actionTarget = binding.actionTargetSpinner.getSelectedItem().toString();;
                if (binding.actionSpinner.getSelectedItem().toString().equals("play")) {
                    // check if is external sound
                    String soundRealPath = SOUND_NAME_PATH_MAP.get(actionTarget);
                    if (soundRealPath != null) {
                        // external sound
                        actionTarget = soundRealPath;
                    }
                }
            }

            // check if null is selected
            if (scriptIsInvalid) {
                Toast.makeText(this, R.string.script_invalid_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            String scriptPiece =
                    trigger + " "
                    + (triggerExtension.isEmpty() ? "" : triggerExtension + " ")
                    + action + " "
                    + actionTarget
                    + ";";

            Log.d("scriptPiece", scriptPiece);

            script += scriptPiece + "\n";

            // set text view
            binding.scriptDisplayTextView.setText(script);

            Toast.makeText(EditShapeScriptActivity.this,
                    "New script added.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setClearAllListener() {
        binding.clearAllButton.setOnClickListener(v -> {
            // use a dialog to warn the user
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Warning:")
                    .setMessage(R.string.delete_all_scripts_dialog)
                    .setNegativeButton(R.string.cancel_dialog_button, (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton(R.string.confirm_dialog_button, (dialog12, which) -> {

                        // delete all scripts
                        script = "";

                        // update text view
                        binding.scriptDisplayTextView.setText("No scripts added.");

                        Toast.makeText(EditShapeScriptActivity.this,
                                "All scripts deleted.", Toast.LENGTH_SHORT).show();
                        dialog12.dismiss();
                    }).create();
            dialog.show();
        });
    }

    private void setBrowseListener() {
        binding.browseSoundButton.setOnClickListener(v -> {
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            startActivityForResult(intent, 1);
        });
    }

    private void setSoundSpinnerListener() {
        binding.actionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // must be choosing the sound
                if (!binding.actionSpinner.getSelectedItem().toString().equals("play")) return;
                // play the sound - should distinguish internal and external
                String soundName = binding.actionTargetSpinner.getSelectedItem().toString();
                MediaPlayer mp;
                if (!SOUND_NAME_PATH_MAP.containsKey(soundName)) {
                    // internal
                    mp = MediaPlayer.create(
                            EditShapeScriptActivity.this, INTERNAL_SOUND_ID_MAP.get(soundName));
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

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initInternalSoundIDMap() {
        int[]IDArray = {R.raw.carrotcarrotcarrot, R.raw.evillaugh, R.raw.fire,
                R.raw.hooray, R.raw.munch, R.raw.munching, R.raw.woof};
        for (int i = 0; i < INTERNAL_SOUND_COUNT; i++) {
            INTERNAL_SOUND_ID_MAP.put(SOUND_NAME_LIST.get(i), IDArray[i]);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("script", script);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    if (uri == null) return;

                    // get real path from uri
                    String realPath = "";
                    try {
                        realPath = PathUtil.getPath(this, uri);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }

                    String soundName = realPath.substring(realPath.lastIndexOf("/") + 1);

                    // add sound item
                    // check if already exists first
                    if (!SOUND_NAME_PATH_MAP.containsKey(soundName)) {
                        SOUND_NAME_LIST.add(soundName);
                        SOUND_NAME_PATH_MAP.put(soundName, realPath);
                    }

                    // update spinner item
                    String[] soundNameArray = new String[SOUND_NAME_LIST.size()];
                    for (int i = 0; i < soundNameArray.length; i++) {
                        soundNameArray[i] = SOUND_NAME_LIST.get(i);
                    }
                    setAdapterForSpinner(soundNameArray, binding.actionTargetSpinner);

                    // set selected item - may not be the last in the list
                    int index;
                    for (index = 0; index < SOUND_NAME_LIST.size(); index++) {
                        if (SOUND_NAME_LIST.get(index).equals(soundName)) break;
                    }
                    binding.actionTargetSpinner.setSelection(index);

                    Toast.makeText(this, R.string.external_sound_read_successful_toast,
                            Toast.LENGTH_SHORT).show();

                }
        }
    }
}