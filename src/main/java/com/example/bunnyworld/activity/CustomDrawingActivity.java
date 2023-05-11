package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;
import static com.example.bunnyworld.utility.StoragePermissionUtil.verifyStoragePermissions;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.bunnyworld.R;
import com.example.bunnyworld.databinding.ActivityCustomDrawingBinding;
import com.example.bunnyworld.utility.PathUtil;
import com.example.bunnyworld.view.CustomDrawingView;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class CustomDrawingActivity extends AppCompatActivity {

    private ActivityCustomDrawingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = ActivityCustomDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.setTitle("Draw Custom Image");

        verifyStoragePermissions(this);

        binding.customDrawingView.setBinding(binding);

        // set listeners
        setModeSelectionListener();
        setColorSpinnerListener();
        setThicknessSpinnerListener();
        setSaveListener();

        // set default to medium
        binding.thicknessSpinner.setSelection(2);
    }

    private void setModeSelectionListener() {
        binding.brushRubberRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.brushRadio:
                    binding.colorSpinner.setEnabled(true);
                    binding.thicknessSpinner.setEnabled(true);
                    binding.colorSpinner.setSelection(0);
                    binding.thicknessSpinner.setSelection(2);
                    binding.customDrawingView.paint.setColor(Color.BLACK);
                    binding.customDrawingView.paint.setStrokeWidth(5);
                    binding.customDrawingView.paint.setXfermode(null); // cancel the clear effect
                    break;
                case R.id.rubberRadio:
                    binding.colorSpinner.setEnabled(false);
                    binding.thicknessSpinner.setEnabled(false);
                    binding.customDrawingView.clear();
                    break;
            }
        });
    }

    private void setColorSpinnerListener() {
        binding.colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        binding.customDrawingView.paint.setColor(Color.BLACK);
                        break;
                    case 1:
                        binding.customDrawingView.paint.setColor(Color.RED);
                        break;
                    case 2:
                        binding.customDrawingView.paint.setColor(Color.rgb(255, 165, 0));
                        break;
                    case 3:
                        binding.customDrawingView.paint.setColor(Color.YELLOW);
                        break;
                    case 4:
                        binding.customDrawingView.paint.setColor(Color.GREEN);
                        break;
                    case 5:
                        binding.customDrawingView.paint.setColor(Color.CYAN);
                        break;
                    case 6:
                        binding.customDrawingView.paint.setColor(Color.BLUE);
                        break;
                    case 7:
                        binding.customDrawingView.paint.setColor(0xFF800080);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setThicknessSpinnerListener() {
        binding.thicknessSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        binding.customDrawingView.paint.setStrokeWidth(1);
                        break;
                    case 1:
                        binding.customDrawingView.paint.setStrokeWidth(2.5f);
                        break;
                    case 2:
                        binding.customDrawingView.paint.setStrokeWidth(5);
                        break;
                    case 3:
                        binding.customDrawingView.paint.setStrokeWidth(8);
                        break;
                    case 4:
                        binding.customDrawingView.paint.setStrokeWidth(15);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setSaveListener() {
        binding.savePNGButton.setOnClickListener(v -> {

            List<String> allFileNames = getAllFileNames();
            String fileName = binding.imageFileNameEditText.getText().toString();

            // check if user has entered the file name
            if (fileName == null || fileName.isEmpty()) {
                Toast.makeText(this, R.string.image_file_name_empty_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            // check duplication
            for (String name : allFileNames) {
                if (name.equalsIgnoreCase(fileName)) {
                    Toast.makeText(this, R.string.image_file_name_exists_toast, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Notice:")
                    .setMessage("Are you sure you want to save the drawing to the device storage as PNG file?")
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Confirm", (dialog12, which) -> {

                        binding.customDrawingView.save(fileName);

                        Toast.makeText(CustomDrawingActivity.this,
                                R.string.image_file_save_successful_toast, Toast.LENGTH_SHORT).show();
                        dialog12.dismiss();
                    }).create();
            dialog.show();

        });

    }

    private List<String> getAllFileNames() {
        List<String> res = new ArrayList<>();

        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Pictures");
        String realPath = "";
        try {
            realPath = PathUtil.getPath(this, uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        File file = new File(realPath);
        File[] files = file.listFiles();
        if (files == null) {
            throw new RuntimeException("Empty path.");
        }
        for (int i =0; i < files.length; i++) {
            String nameWithPath = files[i].getName();
            String pureName = nameWithPath.substring(
                    nameWithPath.lastIndexOf("/") + 1,
                    nameWithPath.lastIndexOf("."));
            res.add(pureName);
        }

        return res;
    }
}