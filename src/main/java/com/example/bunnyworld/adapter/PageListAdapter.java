package com.example.bunnyworld.adapter;
import static com.example.bunnyworld.BunnyWorldEditorApplication.model;

import com.example.bunnyworld.R;
import com.example.bunnyworld.activity.EditPageActivity;
import com.example.bunnyworld.activity.ManagePagesActivity;
import com.example.bunnyworld.databinding.PageListItemBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PageListAdapter extends BaseAdapter {

    private Game game;
    private List<Page> pageList;
    private Context context;
    private boolean checkBoxVisibility = false;
    
    public PageListAdapter(Context context, Game game) {
        this.context = context;
        this.game = game;
        notifyDataSetChanged();
    }

    public void setCheckBoxVisibility(boolean visibility) {
        checkBoxVisibility = visibility;
        notifyDataSetChanged();
    }

    public void setPageList(List<Page> pageList) {
        this.pageList = pageList;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return pageList == null ? 0 : pageList.size();
    }

    @Override
    public Object getItem(int position) {
        return pageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    
    private static class PageListViewHolder extends RecyclerView.ViewHolder {

        private CheckBox selectPageCheckBoxView;
        private TextView pageNameTextView;
        private Button renamePageButton;
        private TextView errorCheckingTextView;
        
        public PageListViewHolder(@NonNull View itemView) {
            super(itemView);
            PageListItemBinding binding = PageListItemBinding.bind(itemView);
            selectPageCheckBoxView = binding.selectPageCheckBox;
            pageNameTextView = binding.pageNameTextView;
            renamePageButton = binding.renamePageButton;
            errorCheckingTextView = binding.errorCheckingText;
        }
    }   
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        // generate item view holder whether already exists or not
        PageListViewHolder pageListViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.page_list_item, parent, false);
            pageListViewHolder = new PageListViewHolder(convertView);
            convertView.setTag(pageListViewHolder);
        } else {
            pageListViewHolder = (PageListViewHolder) convertView.getTag();
        }
        
        // set text for view
        Page page = game.getPageList().get(position);
        String pageName = page.getName();
        // position == 0 is start page
        if (position == 0) {
            pageName += " (Start)";
            // not allow user to delete the start page
            pageListViewHolder.selectPageCheckBoxView.setEnabled(false);
        }

        pageListViewHolder.pageNameTextView.setText(pageName);
        pageListViewHolder.selectPageCheckBoxView.setChecked(page.isChecked());

        // set rename listener
        setRenamePageListener(pageListViewHolder, page);

        // onclick and go to the edit page screen
        setPageItemOnClickListener(pageListViewHolder, game, page);

        // set the checkbox logic
        if (checkBoxVisibility) {
            pageListViewHolder.selectPageCheckBoxView.setVisibility(View.VISIBLE);
            pageListViewHolder.errorCheckingTextView.setVisibility(View.INVISIBLE);

            // together, set the click behavior to disabled
            // and make the rename button visible
            pageListViewHolder.itemView.setEnabled(false);
            pageListViewHolder.renamePageButton.setVisibility(View.VISIBLE);
            
            pageListViewHolder.selectPageCheckBoxView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // set checked to game
                page.setChecked(isChecked);

                // check if all selected
                int countChecked = 0;
                for (Page curPage : pageList) {
                    if (curPage.isChecked()) countChecked++;
                }
                if (countChecked >= 1 && countChecked == pageList.size() - 1) { // start page can never be selected
                    ((CheckBox) ((Activity) context).findViewById(R.id.pageSelectAllCheckBox)).setChecked(true);
                } else {
                    ((CheckBox) ((Activity) context).findViewById(R.id.pageSelectAllCheckBox)).setChecked(false);
                }

                // enable or disable the delete button
                if (countChecked == 0) {
                    ((Button) ((Activity) context).findViewById(R.id.pageDeleteButton)).setEnabled(false);
                } else {
                    ((Button) ((Activity) context).findViewById(R.id.pageDeleteButton)).setEnabled(true);
                }

                // enable or disable the copy and cut menu item
                if (countChecked == 0) {
                    ((ManagePagesActivity) context).getPageOperationMenu().
                            findItem(R.id.copyPageMenuItem).setEnabled(false);
                    ((ManagePagesActivity) context).getPageOperationMenu().
                            findItem(R.id.cutPageMenuItem).setEnabled(false);
                } else {
                    ((ManagePagesActivity) context).getPageOperationMenu().
                            findItem(R.id.copyPageMenuItem).setEnabled(true);
                    ((ManagePagesActivity) context).getPageOperationMenu().
                            findItem(R.id.cutPageMenuItem).setEnabled(true);
                }

            });

        } else {
            pageListViewHolder.selectPageCheckBoxView.setChecked(false);
            pageListViewHolder.selectPageCheckBoxView.setVisibility(View.INVISIBLE);

            pageListViewHolder.itemView.setEnabled(true);
            pageListViewHolder.renamePageButton.setVisibility(View.INVISIBLE);

            pageListViewHolder.errorCheckingTextView.setVisibility(View.VISIBLE);
        }

        // set the internal script error checking view
        if (page.getInternalState()) {
            pageListViewHolder.errorCheckingTextView.setText(R.string.internal_correct_text);
            pageListViewHolder.errorCheckingTextView.setTextColor(Color.GREEN);
        } else {
            pageListViewHolder.errorCheckingTextView.setText(R.string.internal_error_text);
            pageListViewHolder.errorCheckingTextView.setTextColor(Color.RED);
        }

        return convertView;
    }

    private void setPageItemOnClickListener(
            PageListViewHolder pageListViewHolder, Game game, Page page) {
        pageListViewHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditPageActivity.class);

            // pass game to the page editor
            intent.putExtra("Game_Name", game.getName());
            intent.putExtra("Page_Name", page.getName());

            ((ManagePagesActivity) context).startActivityForResult(intent, 1);
        });
    }

    private void setRenamePageListener(PageListViewHolder pageListViewHolder, Page page) {
        pageListViewHolder.renamePageButton.setOnClickListener(v -> {
            View view = ((Activity) context).getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
            final EditText editText = (EditText) view.findViewById(R.id.dialogEditText);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Rename page:")
                    .setView(view)
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Confirm", (dialog2, which) -> {

                        String newName = editText.getText().toString();
                        String prevName = page.getName();

                        // should check if duplication and if has white space
                        if (!game.setNameForPage(newName, page)) {
                            String failToastString = context.getResources().
                                    getString(R.string.page_name_fail_toast);
                            Toast.makeText(context, failToastString, Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                            return;
                        }

                        // change the relevant shape scripts
                        game.updateAllShapeScripts(prevName, newName);

                        // update page name view
                        this.setPageList(pageList);

                        // notify db that name has changed
                        model.addOrUpdateGame(game);

                        String toastString = context.getResources().
                                getString(R.string.page_name_change_toast);
                        Toast.makeText(context, toastString, Toast.LENGTH_SHORT).show();
                        dialog2.dismiss();
                    }).create();
            dialog.show();
        });
    }

}
