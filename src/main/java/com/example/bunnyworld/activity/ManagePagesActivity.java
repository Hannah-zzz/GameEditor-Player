package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.bunnyworld.BunnyWorldEditorApplication;
import com.example.bunnyworld.R;
import com.example.bunnyworld.adapter.PageListAdapter;
import com.example.bunnyworld.dao.Model;
import com.example.bunnyworld.databinding.ActivityManagePagesBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ManagePagesActivity extends AppCompatActivity {

    private ActivityManagePagesBinding binding;
    private PageListAdapter listAdapter;
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityManagePagesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get game name from main activity
        String gameName = getIntent().getStringExtra("Game_Name");
        // directly get game from db, not from the former activity
        game = model.getGame(gameName);
        setTitle(getResources().getString(R.string.manage_pages) + " For " + gameName);

        initializeListView();

        setCreateButtonListener();
        setEditButtonListener();
        setDeleteButtonListener();
        setSelectAllCheckBoxListener();

    }

    private void initializeListView() {

        // check internal state on enter the activity
        game.checkInternalState();

        // initialize adapter for listView
        listAdapter = new PageListAdapter(this, game);
        listAdapter.setPageList(game.getPageList());
        binding.pageListView.setAdapter(listAdapter);

    }

    private void setCreateButtonListener() {
        binding.pageCreateButton.setOnClickListener(v -> {
            // create a new blank page
            game.addBlankPage();
            listAdapter.setPageList(game.getPageList());

            Toast.makeText(this, "New page added.", Toast.LENGTH_SHORT).show();

            // notify the db that pageList has changed
            model.addOrUpdateGame(game);

        });
    }

    private final boolean[] editMode = {false};
    private void setEditButtonListener() {

        binding.pageEditButton.setOnClickListener(v -> {
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
        binding.pageSelectAllCheckBox.setVisibility(View.VISIBLE);

        binding.pageCreateButton.setVisibility(View.INVISIBLE);
        binding.pageDeleteButton.setVisibility(View.VISIBLE);
        binding.pageDeleteButton.setEnabled(false);
        binding.pageEditButton.setText(R.string.cancel_button);

        editMode[0] = true;

    }

    private void backFromEditMode() {
        // change the checkboxes to invisible
        listAdapter.setCheckBoxVisibility(false);
        binding.pageSelectAllCheckBox.setVisibility(View.INVISIBLE);
        binding.pageSelectAllCheckBox.setChecked(false);

        binding.pageCreateButton.setVisibility(View.VISIBLE);
        binding.pageDeleteButton.setVisibility(View.INVISIBLE);
        binding.pageEditButton.setText(R.string.edit_button);

        pageOperationMenu.findItem(R.id.copyPageMenuItem).setEnabled(false);
        pageOperationMenu.findItem(R.id.cutPageMenuItem).setEnabled(false);

        editMode[0] = false;
    }

    private void setDeleteButtonListener() {
        binding.pageDeleteButton.setOnClickListener(v -> {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Warning:")
                    .setMessage("Are you sure you want to delete the selected pages?")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // delete all checked pages
                            game.deleteCheckedPages();

                            // notify change
                            listAdapter.setPageList(game.getPageList());

                            // go back to normal mode
                            backFromEditMode();

                            // update the db that pageList has changed
                            model.addOrUpdateGame(game);

                            Toast.makeText(ManagePagesActivity.this, "Selected pages deleted.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        });
    }

    private void setSelectAllCheckBoxListener() {
        binding.pageSelectAllCheckBox.setOnClickListener(v -> {
            CheckBox checkBox = (CheckBox) v;
            List<Page> pageList = game.getPageList();
            if (checkBox.isChecked()) {
                // index = 0 is always the start page
                for (int i = 1; i < pageList.size(); i++) {
                    pageList.get(i).setChecked(true);
                }
            } else {
                for (Page page : pageList) {
                    page.setChecked(false);
                }
            }
            // DO NOT update the db!
            listAdapter.setPageList(pageList);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // when return from the shape editor, we should read from the db again to update the list
            case 1:
                this.game = model.getGame(game.getName());
                initializeListView();
        }
    }

    // The menu items will support copy, cut, and paste operation on pages
    private Menu pageOperationMenu;
    public Menu getPageOperationMenu() {
        return pageOperationMenu;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.page_operation_menu, menu);
        pageOperationMenu = menu;
        menu.findItem(R.id.copyPageMenuItem).setEnabled(false);
        menu.findItem(R.id.cutPageMenuItem).setEnabled(false);
        if (BunnyWorldEditorApplication.getCopiedPages() == null ||
                BunnyWorldEditorApplication.getCopiedPages().isEmpty()) {
            menu.findItem(R.id.pastePageMenuItem).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.copyPageMenuItem:
                // should make deep copy of checked pages
                List<Page> copiedPages = new ArrayList<>();
                for (Page page : game.getPageList()) {
                    if (page.isChecked()) {
                        copiedPages.add(new Page(page));
                    }
                }
                BunnyWorldEditorApplication.setCopiedPages(copiedPages);
                pageOperationMenu.findItem(R.id.pastePageMenuItem).setEnabled(true);

                // go back to normal mode
                backFromEditMode();

                String copyToastStr = copiedPages.size() + " pages copied!";
                Toast.makeText(this, copyToastStr, Toast.LENGTH_SHORT).show();

                break;
            case R.id.cutPageMenuItem:
                // should make deep copy of checked pages
                List<Page> copiedPages2 = new ArrayList<>();
                for (Page page : game.getPageList()) {
                    if (page.isChecked()) {
                        copiedPages2.add(new Page(page));
                    }
                }
                BunnyWorldEditorApplication.setCopiedPages(copiedPages2);
                pageOperationMenu.findItem(R.id.pastePageMenuItem).setEnabled(true);

                // delete all checked pages
                game.deleteCheckedPages();
                // notify change
                listAdapter.setPageList(game.getPageList());
                // go back to normal mode
                backFromEditMode();
                // update the db that pageList has changed
                model.addOrUpdateGame(game);

                String cutToastStr = copiedPages2.size() + " pages cut!";
                Toast.makeText(this, cutToastStr, Toast.LENGTH_SHORT).show();

                break;
            case R.id.pastePageMenuItem:
                List<Page> pagesToPaste = BunnyWorldEditorApplication.getCopiedPages();
                // sanity check
                if (pagesToPaste == null || pagesToPaste.isEmpty()) {
                    Toast.makeText(this, "No shape copied!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // rename each time at paste
                for (Page page : pagesToPaste) {
                    // set name for current page
                    game.addPageForPaste(page);
                    game.resetNameForAllShapesInPageAtPaste(page);
                }

                // notify change
                listAdapter.setPageList(game.getPageList());
                // go back to normal mode
                backFromEditMode();
                // update the db that pageList has changed
                model.addOrUpdateGame(game);

                String pasteToastStr = pagesToPaste.size() + " pages pasted!";
                Toast.makeText(this, pasteToastStr, Toast.LENGTH_SHORT).show();

                // clear the copied list and set enabled - only support paste once
                BunnyWorldEditorApplication.setCopiedPages(null);
                pageOperationMenu.findItem(R.id.pastePageMenuItem).setEnabled(false);

                break;
            default:
                break;
        }

        return true;

    }

}