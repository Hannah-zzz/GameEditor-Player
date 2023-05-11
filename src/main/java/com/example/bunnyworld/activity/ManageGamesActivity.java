package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bunnyworld.BunnyWorldEditorApplication;
import com.example.bunnyworld.R;
import com.example.bunnyworld.adapter.GameListAdapter;
import com.example.bunnyworld.dao.Model;
import com.example.bunnyworld.databinding.ActivityManageGamesBinding;
import com.example.bunnyworld.entity.Game;

import java.util.List;

public class ManageGamesActivity extends AppCompatActivity {

    private ActivityManageGamesBinding binding;
    private GameListAdapter listAdapter;
    private List<Game> gameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityManageGamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(R.string.manage_games);

        initializeListView();

        setCreateButtonListener();
        setEditButtonListener();
        setDeleteButtonListener();
        setSelectAllCheckBoxListener();
    }

    private void initializeListView() {

        gameList = model.getGames();
        // check the internal state for all games
        for (Game game : gameList) {
            game.checkInternalState();
        }

        // initialize adapter for listView
        listAdapter = new GameListAdapter(this);
        listAdapter.setGameList(gameList);
        binding.gameListView.setAdapter(listAdapter);
    }

    private void setCreateButtonListener() {

        binding.createButton.setOnClickListener(v -> {

            View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
            final EditText editText = (EditText) view.findViewById(R.id.dialogEditText);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Enter new game name:")
                    .setView(view)
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Confirm", (dialog2, which) -> {
                        String content = editText.getText().toString();

                        // check duplication of input name first
                        if (model.isInDB(content) == Model.DUPLICATE_NAME) {
                            Toast.makeText(ManageGamesActivity.this,
                                    R.string.game_name_duplicate_toast, Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                            return;
                        } else if(model.isInDB(content) == Model.INVALID_NAME) {
                            Toast.makeText(ManageGamesActivity.this,
                                    R.string.game_name_invalid_toast, Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                            return;
                        } else if(model.isInDB(content) == 0) {

                            Game newGame = new Game(content);

                            // add newGame to dataBase
                            // internal will check if has white space and semi-column
                            model.addOrUpdateGame(newGame);

                            // and then update listView, make newly added one to be on the top
                            gameList.add(newGame);
                            listAdapter.setGameList(gameList);

                            String toastString = "New game \"" + content + "\" created.";
                            Toast.makeText(ManageGamesActivity.this, toastString, Toast.LENGTH_SHORT).show();
                        }
                        dialog2.dismiss();
                    }).create();
            dialog.show();

        });

    }


    private final boolean[] editMode = {false};
    private void setEditButtonListener() {

        binding.editButton.setOnClickListener(v -> {
            // shift between two states
            if (!editMode[0]) {
                shiftToEditMode();
            } else {
                backFromEditMode();
            }
        });
    }

    private void shiftToEditMode() {
        // change the checkboxes to visible
        listAdapter.setCheckBoxVisibility(true);
        binding.selectAllCheckBox.setVisibility(View.VISIBLE);

        // control the visibility of some buttons
        binding.createButton.setVisibility(View.INVISIBLE);
        binding.deleteGameButton.setVisibility(View.VISIBLE);
        binding.deleteGameButton.setEnabled(false);

        // shift edit button text
        binding.editButton.setText(R.string.cancel_button);

        editMode[0] = true;

    }

    private void backFromEditMode() {
        // change the checkboxes to invisible
        listAdapter.setCheckBoxVisibility(false);
        binding.selectAllCheckBox.setVisibility(View.INVISIBLE);
        binding.selectAllCheckBox.setChecked(false);

        binding.createButton.setVisibility(View.VISIBLE);
        binding.deleteGameButton.setVisibility(View.INVISIBLE);
        binding.editButton.setText(R.string.edit_button);

        editMode[0] = false;
    }

    private void setDeleteButtonListener() {
        binding.deleteGameButton.setOnClickListener(v -> {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Warning:")
                    .setMessage("Are you sure you want to delete the selected games?")
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Confirm", (dialog12, which) -> {

                        // delete all checked games
                        for (Game game : gameList) {
                            if (game.isChecked()) {
                                model.deleteGame(game);
                            }
                        }

                        // update gameList
                        gameList = model.getGames();

                        // notify change
                        listAdapter.setGameList(gameList);

                        // go back to normal mode
                        backFromEditMode();

                        Toast.makeText(ManageGamesActivity.this,
                                "Selected Game deleted.", Toast.LENGTH_SHORT).show();
                        dialog12.dismiss();
                    }).create();
            dialog.show();
        });
    }

    private void setSelectAllCheckBoxListener() {
        binding.selectAllCheckBox.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox) v;
            if (checkBox.isChecked()) {
                for (Game game : gameList) {
                    game.setChecked(true);
                }
            } else {
                for (Game game : gameList) {
                    game.setChecked(false);
                }
            }
            listAdapter.setGameList(gameList);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // when return from the page list, we should read from the db again to update the list
            case 1:
                this.gameList = model.getGames();
                initializeListView();
        }
    }
}