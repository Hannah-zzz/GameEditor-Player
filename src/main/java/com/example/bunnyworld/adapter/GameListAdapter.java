package com.example.bunnyworld.adapter;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;

import com.example.bunnyworld.activity.ManageGamesActivity;
import com.example.bunnyworld.activity.ManagePagesActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.example.bunnyworld.R;
import com.example.bunnyworld.activity.PlayGameActivity;
import com.example.bunnyworld.dao.Model;
import com.example.bunnyworld.databinding.GameListItemBinding;
import com.example.bunnyworld.entity.Game;

import java.util.List;

public class GameListAdapter extends BaseAdapter {

    private List<Game> gameList;
    private Context context;
    private boolean checkBoxVisibility = false;

    public GameListAdapter(Context context) {
        this.context = context;
        notifyDataSetChanged();
    }

    public void setCheckBoxVisibility(boolean visibility) {
        checkBoxVisibility = visibility;
        notifyDataSetChanged();
    }

    public void setGameList(List<Game> gameList) {
        this.gameList = gameList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return gameList == null ? 0 : gameList.size();
    }

    @Override
    public Object getItem(int position) {
        return gameList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class GameListViewHolder extends RecyclerView.ViewHolder {

        private CheckBox selectGameCheckBoxView;
        private TextView gameNameTextView;
        private Button editGameButton;
        private Button playGameButton;
        private Button renameGameButton;

        public GameListViewHolder(@NonNull View itemView) {
            super(itemView);
            GameListItemBinding binding = GameListItemBinding.bind(itemView);
            selectGameCheckBoxView = binding.selectGameCheckBox;
            gameNameTextView = binding.gameNameTextView;
            editGameButton = binding.editGameButton;
            playGameButton = binding.playGameButton;
            renameGameButton = binding.renameGameButton;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GameListViewHolder gameListViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.game_list_item, parent, false);
            gameListViewHolder = new GameListViewHolder(convertView);
            convertView.setTag(gameListViewHolder);
        } else {
            gameListViewHolder = (GameListViewHolder) convertView.getTag();
        }

        // set text for view
        Game game = gameList.get(position);
        gameListViewHolder.gameNameTextView.setText(game.getName());
        gameListViewHolder.selectGameCheckBoxView.setChecked(game.isChecked());

        // set listener for the buttons in items
        setEditGameListener(gameListViewHolder, game);
        setPlayGameListener(gameListViewHolder, game);
        setRenameGameListener(gameListViewHolder, game);

        // set the checkbox logic
        if (checkBoxVisibility) {
            gameListViewHolder.selectGameCheckBoxView.setVisibility(View.VISIBLE);

            // together, make the edit and play buttons invisible
            // and make the rename button visible
            gameListViewHolder.editGameButton.setVisibility(View.INVISIBLE);
            gameListViewHolder.playGameButton.setVisibility(View.INVISIBLE);
            gameListViewHolder.renameGameButton.setVisibility(View.VISIBLE);


            final GameListViewHolder finalHolder = gameListViewHolder;

            gameListViewHolder.selectGameCheckBoxView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // set checked to game
                game.setChecked(isChecked);

                // check if all selected
                int countSelected = 0;
                for (Game curGame : gameList) {
                    if (curGame.isChecked()) countSelected++;
                }
                if (countSelected == gameList.size()) {
                    ((CheckBox) ((Activity) context).findViewById(R.id.selectAllCheckBox)).setChecked(true);
                } else {
                    ((CheckBox) ((Activity) context).findViewById(R.id.selectAllCheckBox)).setChecked(false);
                }

                // enable or disable the delete button
                if (countSelected == 0) {
                    ((Button) ((Activity) context).findViewById(R.id.deleteGameButton)).setEnabled(false);
                } else {
                    ((Button) ((Activity) context).findViewById(R.id.deleteGameButton)).setEnabled(true);
                }

            });

        } else {
            gameListViewHolder.selectGameCheckBoxView.setChecked(false);
            gameListViewHolder.selectGameCheckBoxView.setVisibility(View.INVISIBLE);

            gameListViewHolder.editGameButton.setVisibility(View.VISIBLE);
            gameListViewHolder.playGameButton.setVisibility(View.VISIBLE);
            gameListViewHolder.renameGameButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private void setEditGameListener(GameListViewHolder gameListViewHolder, Game game) {
        gameListViewHolder.editGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ManagePagesActivity.class);

            intent.putExtra("Game_Name", game.getName());

            ((ManageGamesActivity) context).startActivityForResult(intent, 1);
        });
    }

    private void setPlayGameListener(GameListViewHolder gameListViewHolder, Game game) {
        gameListViewHolder.playGameButton.setOnClickListener(v -> {

            // check if we can start the game
            if (game.isInternalValid()) {
                Intent intent = new Intent(context, PlayGameActivity.class);
                intent.putExtra("Game_Name", game.getName());
                context.startActivity(intent);
                Toast.makeText(context, R.string.game_start_successful_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.game_invalid_toast, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void setRenameGameListener(GameListViewHolder gameListViewHolder, Game game) {
        gameListViewHolder.renameGameButton.setOnClickListener(v -> {
            View view = ((Activity) context).getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
            final EditText editText = (EditText) view.findViewById(R.id.dialogEditText);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Rename game:")
                    .setView(view)
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Confirm", (dialog2, which) -> {
                        String newName = editText.getText().toString();

                        // should check if the newName conflicts with others
                        if (model.isInDB(newName) == Model.DUPLICATE_NAME) {
                            Toast.makeText(context,
                                    R.string.game_name_duplicate_toast, Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                            return;
                        } else if (model.isInDB(newName) == Model.INVALID_NAME) {
                            Toast.makeText(context,
                                    R.string.game_name_invalid_toast, Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                            return;
                        } else {
                            model.deleteGame(game);
                            game.setName(newName);
                            // notify game list view
                            this.setGameList(gameList);

                            // notify db that name has changed
                            model.addOrUpdateGame(game);

                            Toast.makeText(context,
                                    R.string.game_name_change_toast, Toast.LENGTH_SHORT).show();
                        }
                        dialog2.dismiss();
                    }).create();
            dialog.show();
        });
    }

}
