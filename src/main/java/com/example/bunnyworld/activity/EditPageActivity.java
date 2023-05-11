package com.example.bunnyworld.activity;

import static com.example.bunnyworld.BunnyWorldEditorApplication.model;
import static com.example.bunnyworld.utility.StoragePermissionUtil.verifyStoragePermissions;
import static com.example.bunnyworld.view.ShapeDrawingView.getDrawableFromShape;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.bunnyworld.BunnyWorldEditorApplication;
import com.example.bunnyworld.R;
import com.example.bunnyworld.databinding.ActivityEditOnePageBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;
import com.example.bunnyworld.utility.PathUtil;

import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EditPageActivity extends AppCompatActivity {

    private Game game;
    private Page page;
    private Shape selectedShape;

    // indicates whether there is a selected shape
    private boolean selectedMode;
    private boolean needSaveStatus;

    // Supporter to do the undo operations
    private UndoSupporter undoSupporter;

    // list to store the spinner items when choosing image
    private List<String> imageNameList = new ArrayList<>(Arrays.asList(
            "Select Image", "carrot.png", "carrot2.png", "death.png", "duck.png", "fire.png", "mystic.png"
    ));
    private Map<String, String> imageNamePathMap = new HashMap<>();

    private ActivityEditOnePageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditOnePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // tricky: get storage access permission
        verifyStoragePermissions(this);

        // get game and page from parent
        String gameName = getIntent().getStringExtra("Game_Name");
        String pageName = getIntent().getStringExtra("Page_Name");
        game = model.getGame(gameName);
        page = game.getPage(pageName);
        // set label for the current activity based on page name
        if (page != null) {
            this.setTitle(page.getName());
        }

        // init undo supporter - must after page is initialized
        this.undoSupporter = this.new UndoSupporter();

        // initialize selected shape to null
        setSelectedShape(null, false);

        setNeedSaveStatus(false);

        // set binding and game, and page for the shape drawing view
        binding.shapeDrawingView.setGame(game);
        binding.shapeDrawingView.setPage(page);
        binding.shapeDrawingView.setBinding(binding);
        binding.shapeDrawingView.setUndoSupporter(undoSupporter);

        setListeners();

    }

    private void setListeners() {
        // set listeners for buttons
        setSaveListener();
        setShowListener();
        setBrowseListener();
        setShiftButtonListener();

        // setup image choosing spinner
        setImageChoosingSpinner();
        setImageSpinnerListener();
        setOriginalCheckBoxListener();
        setCustomDrawingListener();

        // set listeners for the text input edittext and fontSize spinner
        setInputTextListener();
        setFontSizeListener();

    }

    // The second field is deprecated and not in use now.
    public void setSelectedShape(Shape shape, boolean needCopy) {
        if (shape != null) {
            // This one is selected, and all other shapes are set not selected
            for (Shape curShape : page.getShapeList()) {
                if (curShape.getName().equals(shape.getName())) {
                    curShape.setSelected(true);
                } else {
                    curShape.setSelected(false);
                }
            }

            this.selectedShape = shape;

            setSelectedMode(true);
            updateShapeAttributeView();
            setNeedSaveStatus(true);

        } else {
            for (Shape curShape : page.getShapeList()) {
                curShape.setSelected(false);
            }
            this.selectedShape = null;
            setSelectedMode(false);
        }
        binding.shapeDrawingView.invalidate();
    }

    private void setSelectedMode(boolean selectedMode) {

        this.selectedMode = selectedMode;

        if (selectedMode) {
            binding.saveButton.setEnabled(true);
            binding.browseButton.setEnabled(true);
            binding.shiftButton.setEnabled(true);
            binding.showButton.setEnabled(true);

            binding.movableCheckBox.setEnabled(true);
            binding.visibleCheckBox.setEnabled(true);

            binding.inputX.setEnabled(true);
            binding.inputY.setEnabled(true);
            binding.inputWidth.setEnabled(true);
            binding.inputHeight.setEnabled(true);
            binding.inputShapeName.setEnabled(true);
            binding.imageChoosingSpinner.setEnabled(true);
            binding.customDrawingButton.setEnabled(true);
            binding.inputText.setEnabled(true);
            binding.fontSizeSpinner.setEnabled(true);

            shapeOperationMenu.findItem(R.id.copyShapeMenuItem).setEnabled(true);
            shapeOperationMenu.findItem(R.id.cutShapeMenuItem).setEnabled(true);
        } else {
            // make all attribute views and related buttons inactive
            binding.browseButton.setEnabled(false);
            binding.shiftButton.setEnabled(false);
            binding.showButton.setEnabled(false);

            binding.movableCheckBox.setChecked(false);
            binding.visibleCheckBox.setChecked(false);
            binding.movableCheckBox.setEnabled(false);
            binding.visibleCheckBox.setEnabled(false);

            binding.inputX.setText("");
            binding.inputY.setText("");
            binding.inputWidth.setText("");
            binding.inputHeight.setText("");
            binding.inputShapeName.setText("");
            binding.inputText.setText("");

            binding.inputX.setEnabled(false);
            binding.inputY.setEnabled(false);
            binding.inputWidth.setEnabled(false);
            binding.inputHeight.setEnabled(false);
            binding.inputShapeName.setEnabled(false);
            binding.imageChoosingSpinner.setEnabled(false);
            binding.imageChoosingSpinner.setSelection(0); // clear the previous selection
            binding.customDrawingButton.setEnabled(false);
            binding.inputText.setEnabled(false);
            binding.fontSizeSpinner.setEnabled(false);
            binding.fontSizeSpinner.setSelection(0); // clear the previous selection

            if (shapeOperationMenu != null) {
                shapeOperationMenu.findItem(R.id.copyShapeMenuItem).setEnabled(false);
                shapeOperationMenu.findItem(R.id.cutShapeMenuItem).setEnabled(false);
            }

        }
    }

    // This function updates the below state view of selected shape.
    private void updateShapeAttributeView() {

        binding.inputX.setText(String.valueOf(selectedShape.getRect().get(0)));
        binding.inputY.setText(String.valueOf(selectedShape.getRect().get(1)));
        binding.inputWidth.setText(String.valueOf(selectedShape.getRect().get(2)));
        binding.inputHeight.setText(String.valueOf(selectedShape.getRect().get(3)));

        binding.inputShapeName.setText(selectedShape.getName());
        binding.movableCheckBox.setChecked(selectedShape.getMovable());
        binding.visibleCheckBox.setChecked(selectedShape.getVisible());

        // set selected item for spinners
        setImageSelectionForSelectedShape();
        setFontSizeForSelectedShape();

        binding.originalCheckBox.setChecked(selectedShape.isOriginalImageSize());
        binding.inputText.setText(selectedShape.getText());
    }

    // select image spinner selected item to be the image name
    private void setImageSelectionForSelectedShape() {
        // check if have image name first
        if (selectedShape.getImage() == null || selectedShape.getImage().isEmpty()) {
            binding.imageChoosingSpinner.setSelection(0);
            return;
        }

        // from paste: the selected image can be not in the spinner
        setImageChoosingSpinner();

        // discard image path if it has one
        String imageName = selectedShape.getImage().substring(
                selectedShape.getImage().lastIndexOf("/") + 1);

        // set selected item - may not be the last in the list
        int position;
        for (position = 0; position < imageNameList.size(); position++) {
            if (imageNameList.get(position).equals(imageName)) break;
        }
        binding.imageChoosingSpinner.setSelection(position);
    }

    private void setFontSizeForSelectedShape() {
        // check if have text and user set font size first
        if (selectedShape.getText() == null || selectedShape.getText().isEmpty()
                || Float.compare(selectedShape.getFontSize(), 50f) == 0) {
            binding.fontSizeSpinner.setSelection(0);
            return;
        }

        // transfer from px to sp
        float scaledSizeInPixels = selectedShape.getFontSize();
        int spSize = (int) (scaledSizeInPixels / getResources().getDisplayMetrics().scaledDensity);

        // set selected item
        int position;
        String[] fontSizeArray = getResources().getStringArray(R.array.fontSizeSpinnerClass);
        for (position = 0; position < fontSizeArray.length; position++) {
            if (fontSizeArray[position].equals(String.valueOf(spSize))) break;
        }
        binding.fontSizeSpinner.setSelection(position);
    }

    public void setNeedSaveStatus(boolean needSave) {
        this.needSaveStatus = needSave;
        binding.saveButton.setEnabled(needSave);
    }

    private void setSaveListener() {
        binding.saveButton.setOnClickListener(v -> {

            // check if has selected
            if (selectedShape != null) {
                // set attributes for the selected shape
                if (!setAttributes()) {
                    return;
                }
            }

            // save the game
            model.addOrUpdateGame(game);

            // inactivate the button
            setNeedSaveStatus(false);

            setSelectedShape(null, false);

            Toast.makeText(this, R.string.shape_save_successful_toast, Toast.LENGTH_SHORT).show();
        });
    }

    // set attributes for the selected shape for later saving
    private boolean setAttributes() {

        if (!setNewNameForSelectedShape()) return false;

        // if the geometric has been changed and the user does not press "show"
        updateShapeView();

        selectedShape.setMovable(binding.movableCheckBox.isChecked());
        selectedShape.setVisible(binding.visibleCheckBox.isChecked());

        // the name, the geometric, and the checkbox states of the shape will be undo together
        undoSupporter.pushToUndoStack();

        return true;
    }

    private boolean setNewNameForSelectedShape() {

        String newName = binding.inputShapeName.getText().toString();
        String prevName = selectedShape.getName();

        // should ensure that the name has changed
        if (!newName.equals(prevName)) {
            boolean addNewNameStatus = game.setNameForShape(newName, selectedShape);
            if (!addNewNameStatus) {
                Toast.makeText(this,
                        R.string.shape_name_invalid_toast, Toast.LENGTH_SHORT).show();
                return false;
            }
            // if set successful, replace the relevant name in scripts
            game.updateAllShapeScripts(prevName, newName);
        }
        return true;
    }

    private void setShowListener() {
        binding.showButton.setOnClickListener(v -> {
            updateShapeView();
            undoSupporter.pushToUndoStack();
        });
    }

    private void updateShapeView() {
        // get the user input
        int inputX = Integer.valueOf(binding.inputX.getText().toString());
        int inputY = Integer.valueOf(binding.inputY.getText().toString());
        int inputWidth = Integer.valueOf(binding.inputWidth.getText().toString());
        int inputHeight = Integer.valueOf(binding.inputHeight.getText().toString());

        selectedShape.setRect(inputX, inputY, inputWidth, inputHeight);

        // tell the view to draw itself
        binding.shapeDrawingView.invalidate();
    }

    private void setBrowseListener() {
        binding.browseButton.setOnClickListener(v -> {
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Pictures");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            startActivityForResult(intent, 2);
        });
    }

    // set item for image spinner, and initialize map
    private void setImageChoosingSpinner() {
        // must take into consideration if user has used external images
        Set<String> internalImageNameSet = new HashSet<>(imageNameList);
        for (Shape shape : page.getShapeList()) {
            // discard image path if it has one
            String imageName = shape.getImage().substring(shape.getImage().lastIndexOf("/") + 1);
            if (!internalImageNameSet.contains(imageName) && !imageName.isEmpty()) {
                internalImageNameSet.add(imageName);
                imageNameList.add(imageName);
                imageNamePathMap.put(imageName, shape.getImage()); // second parameter is the real path
            }
        }

        // set adapter for the spinner
        String[] imageNameArray = new String[imageNameList.size()];
        for (int i = 0; i < imageNameList.size(); i++) {
            imageNameArray[i] = imageNameList.get(i);
        }
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this, R.layout.spinner_item, imageNameArray);
        binding.imageChoosingSpinner.setAdapter(spinnerAdapter);
    }

    private void setImageSpinnerListener() {
        binding.imageChoosingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // check if selection is valid
                if (position == 0) {
                    binding.originalCheckBox.setChecked(false);
                    binding.originalCheckBox.setEnabled(false);
                    return;
                }

                if (position <= 6) {
                    // internal images
                    selectedShape.setImage(imageNameList.get(position));
                } else {
                    // external images
                    String realPath = imageNamePathMap.get(imageNameList.get(position));
                    selectedShape.setImage(realPath);
                }

                binding.originalCheckBox.setChecked(selectedShape.isOriginalImageSize());
                binding.originalCheckBox.setEnabled(true);

                binding.shapeDrawingView.invalidate();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setOriginalCheckBoxListener() {
        // must be onclick, because the checked state will also change on selected shape changed
        binding.originalCheckBox.setOnClickListener(v -> {
            CheckBox checkbox = (CheckBox) v;
            if (checkbox.isChecked()) {
                BitmapDrawable bitmapDrawable =
                        getDrawableFromShape(selectedShape, EditPageActivity.this);
                Bitmap bitmap = bitmapDrawable.getBitmap();

                float width = bitmap.getWidth();
                float height = bitmap.getHeight();
                float curWidth = selectedShape.getRect().get(2);
                float curHeight = selectedShape.getRect().get(3);

                // always change the shorter edge
                if (curWidth > curHeight) {
                    curHeight = curWidth * height / width;
                } else {
                    curWidth = curHeight * width / height;
                }

                selectedShape.setRect(
                        selectedShape.getRect().get(0),
                        selectedShape.getRect().get(1),
                        (int) curWidth,
                        (int) curHeight
                );

                selectedShape.setOriginalImageSize(true);

                // update drawing view and the state view
                setSelectedShape(selectedShape, false);

                undoSupporter.pushToUndoStack();

            } else {
                selectedShape.setOriginalImageSize(false);
            }
        });
    }

    private void setCustomDrawingListener() {
        binding.customDrawingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomDrawingActivity.class);
            this.startActivity(intent);
        });
    }

    private void setInputTextListener() {
        binding.inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selectedShape != null) {
                    selectedShape.setText(binding.inputText.getText().toString());
                    binding.shapeDrawingView.invalidate();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void setFontSizeListener() {
        binding.fontSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedShape != null && position != 0) {
                    int spSize = Integer.valueOf(binding.fontSizeSpinner.getSelectedItem().toString());
                    float scaledSizeInPixels = spSize * getResources().getDisplayMetrics().scaledDensity;
                    selectedShape.setFontSize(scaledSizeInPixels);
                    
                    binding.shapeDrawingView.invalidate();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setShiftButtonListener() {
        binding.shiftButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditShapeScriptActivity.class);

            // Page names and shape names are prepared for assembling the script
            intent.putExtra("Page_Name_Array_Without_Current_Page", game.getPageNameArrayWithoutPage(page));

            // we should get the name array of shapes in the whole game
            String[] shapeNameArray = game.getShapeNameArrayWithoutShapeAtPage(null, null);
            String[] shapeNameArrayWithoutShape = game.getShapeNameArrayWithoutShapeAtPage(selectedShape, page);
            intent.putExtra("Shape_Name_Array", shapeNameArray);
            intent.putExtra("Shape_Name_Array_Without_Selected_Shape", shapeNameArrayWithoutShape);

            intent.putExtra("Shape_Name", selectedShape.getName());
            intent.putExtra("Script", selectedShape.getScript());

            this.startActivityForResult(intent, 1);
        });
    }


    // when come back from:
    // 1. script assembler; 2. image browser
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String script = intent.getStringExtra("script");
                    // set all script, not just one
                    selectedShape.setScript(script);
                    
                    undoSupporter.pushToUndoStack();
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {

                    Uri uri = intent.getData();

                    if (uri == null) return;

                    String realPath = "";
                    try {
                        realPath = PathUtil.getPath(this, uri);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }

                    String imageName = realPath.substring(realPath.lastIndexOf("/") + 1);

                    // add image item
                    // check if already exists first
                    if (!imageNamePathMap.containsKey(imageName)) {
                        imageNameList.add(imageName);
                        imageNamePathMap.put(imageName, realPath);
                    }

                    // update spinner items and listener
                    setImageChoosingSpinner();
                    setImageSpinnerListener();

                    selectedShape.setImage(realPath);

                    // set selected item - redraw will trigger on selection
                    setImageSelectionForSelectedShape();

                    Toast.makeText(this, R.string.external_image_read_toast,
                            Toast.LENGTH_SHORT).show();

                    undoSupporter.pushToUndoStack();
                }
                break;
        }

    }


    private Menu shapeOperationMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shape_operation_menu, menu);
        shapeOperationMenu = menu;

        menu.findItem(R.id.copyShapeMenuItem).setEnabled(false);
        menu.findItem(R.id.cutShapeMenuItem).setEnabled(false);
        menu.findItem(R.id.undoMenuItem).setEnabled(false);
        if (BunnyWorldEditorApplication.getCopiedShape() == null) {
            menu.findItem(R.id.pasteShapeMenuItem).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.copyShapeMenuItem:
                // should make deep copy of selected shape
                BunnyWorldEditorApplication.setCopiedShape(new Shape(selectedShape));
                shapeOperationMenu.findItem(R.id.pasteShapeMenuItem).setEnabled(true);
                Toast.makeText(this, "Shape copied!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.cutShapeMenuItem:
                // should make deep copy of selected shape
                BunnyWorldEditorApplication.setCopiedShape(new Shape(selectedShape));
                shapeOperationMenu.findItem(R.id.pasteShapeMenuItem).setEnabled(true);
                // delete cut shape in the current page
                page.deleteShape(selectedShape);
                setSelectedShape(null, false);
                Toast.makeText(this, "Shape cut!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.pasteShapeMenuItem:
                Shape pastedShape = BunnyWorldEditorApplication.getCopiedShape();
                // sanity check
                if (pastedShape == null) {
                    Toast.makeText(this, "No shape copied!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // rename each time at paste
                game.pasteShapeToPage(pastedShape, page);
                setSelectedShape(pastedShape, false);
                Toast.makeText(this, "Shape pasted!", Toast.LENGTH_SHORT).show();

                undoSupporter.pushToUndoStack();

                // should copy another time
                BunnyWorldEditorApplication.setCopiedShape(new Shape(selectedShape));
                break;
            case R.id.undoMenuItem:
                undoSupporter.pollFromUndoStack();
                break;
            default:
                break;
        }

        return true;

    }

    // This class supports all operations related to undo
    public class UndoSupporter {
        // use a stack of pages to support undo
        private final Deque<Page> UNDO_STACK;

        public UndoSupporter() {
            this.UNDO_STACK = new ArrayDeque<>();
            pushToUndoStack();
        }

        // Call this function every time a change is made to the page
        public void pushToUndoStack() {
            // make the current state a deep copy
            Page pageToPush = new Page(page);
            UNDO_STACK.offerFirst(pageToPush);

            // should check if is the original state push
            if (UNDO_STACK.size() > 1) {
                shapeOperationMenu.findItem(R.id.undoMenuItem).setEnabled(true);
            }
        }

        public void pollFromUndoStack() {
            // one calling undo, it must exists at least 2 pages
            // the bottom page in the stack must be the original state
            if (UNDO_STACK.size() <= 1) {
                throw new RuntimeException("Internal Error: undo operation error.");
            }

            UNDO_STACK.pollFirst();
            // tricky: when peek, we should also not give out the original one in the stack
            // deep copy page to solve this problem
            Page pageToRestore = new Page(UNDO_STACK.peekFirst());
            EditPageActivity.this.page = pageToRestore;
            binding.shapeDrawingView.setPage(pageToRestore);
            // Because we only update the game, so we should set page into the game
            game.updatePage(pageToRestore);
            binding.shapeDrawingView.invalidate();

            Toast.makeText(EditPageActivity.this, R.string.undo_toast, Toast.LENGTH_SHORT).show();

            // should check if undo to the original state
            if (UNDO_STACK.size() == 1) {
                shapeOperationMenu.findItem(R.id.undoMenuItem).setEnabled(false);
                EditPageActivity.this.setNeedSaveStatus(false);
            } else {
                EditPageActivity.this.setNeedSaveStatus(true);
            }

            // unselect all shapes at undo
            EditPageActivity.this.setSelectedShape(null, false);
        }
    }


}

