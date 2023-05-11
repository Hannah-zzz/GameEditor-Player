package com.example.bunnyworld.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.bunnyworld.R;
import com.example.bunnyworld.activity.EditPageActivity;
import com.example.bunnyworld.databinding.ActivityEditOnePageBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.Shape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapeDrawingView extends View {

    private Game game;
    private Page page;
    private List<Shape> shapeList;
    
    private EditPageActivity.UndoSupporter undoSupporter;

    private Paint generalPaint;
    private Paint selectedPaint;
    private Paint fillingPaint;

    private Context context;
    private EditPageActivity activity;

    public static final Map<String, Integer> IMAGE_NAME_ID_MAP = new HashMap<>();

    private ActivityEditOnePageBinding binding;

    public ShapeDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        this.activity = (EditPageActivity) context; 

        initPaint();
        initMap();
    }

    public void setBinding(ActivityEditOnePageBinding binding) {
        this.binding = binding;
    }

    public void setPage(Page page) {
        this.page = page;
        this.shapeList = page.getShapeList();
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setUndoSupporter(EditPageActivity.UndoSupporter undoSupporter) {
        this.undoSupporter = undoSupporter;
    }

    private void initPaint() {
        // setup general paint
        generalPaint = new Paint();
        generalPaint.setColor(Color.LTGRAY);
        generalPaint.setStyle(Paint.Style.STROKE);
        generalPaint.setStrokeWidth(5.0f);
        // setup selectedPaint
        selectedPaint = new Paint();
        selectedPaint.setColor(Color.GREEN);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(15.0f);
        // setup fillingPaint
        fillingPaint = new Paint();
        fillingPaint.setColor(Color.WHITE);
        fillingPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // ask the shapes to draw themselves
        if (page == null || page.getShapeList() == null) return;
        for (Shape shape : page.getShapeList()) {

            // turn image to drawable
            BitmapDrawable shapeImageDrawable = getDrawableFromShape(shape, context);

            if (shape.getSelected()) {
                shape.drawShape(canvas, selectedPaint, fillingPaint, shapeImageDrawable);
            } else {
                shape.drawShape(canvas, generalPaint, fillingPaint, shapeImageDrawable);
            }
        }

    }

    // Helper function to change image in shape to drawable
    public static BitmapDrawable getDrawableFromShape(Shape shape, Context context) {
        BitmapDrawable shapeImageDrawable;
        if (shape.getImage() != null && !shape.getImage().isEmpty()) {
            Integer internalImageId = IMAGE_NAME_ID_MAP.get(shape.getImage());
            if (internalImageId != null) {
                // internal preload image
                shapeImageDrawable = (BitmapDrawable) context.getResources().getDrawable(internalImageId);
            } else {
                Bitmap image = BitmapFactory.decodeFile(shape.getImage());
                // check if successfully read the image from storage
                if (image == null) {
                    Toast.makeText(context, "External image not found or blocked.",
                            Toast.LENGTH_SHORT).show();
                    shapeImageDrawable = null;
                } else {
                    shapeImageDrawable = new BitmapDrawable(image);
                }
            }
        } else {
            shapeImageDrawable = null;
        }

        return shapeImageDrawable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // how to deal with user touch depends on what model it is in
        int currentCheck = binding.selectModeRadioGroup.getCheckedRadioButtonId();
        switch (currentCheck) {
            case R.id.selectButton:
                selectShape(event);
                break;
            case R.id.addButton:
                addNewShape(event);
                break;
            case R.id.deleteButton:
                deleteShape(event);
                break;
        }
        return true;
    }

    // This function allows the touch on shapeDrawingView to not be interfered by the scroll view
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().getParent().requestDisallowInterceptTouchEvent(true);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private long timeDown = 0l, timeMove;
    private float X = 0f, Y = 0f;
    boolean isMoving;
    private Shape selectedShapeInView = null;
    // This function selects the shape that is touched.
    private void selectShape(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                // record time down
                timeDown = System.currentTimeMillis();
                isMoving = false;

                X = event.getX();
                Y = event.getY();

                boolean haveFound = false;
                for (int i = shapeList.size() - 1; i >= 0; i--) {
                    Shape curShape = shapeList.get(i);
                    // with in range -> have found
                    if (inRange(X, Y, curShape)) {
                        // notify the activity of selection
                        activity.setSelectedShape(curShape, false);
                        selectedShapeInView = curShape;
                        haveFound = true;
                        break;
                    }
                }

                // check if we fail to find a shape - means nothing is selected
                if (!haveFound) {
                    activity.setSelectedShape(null, false);
                }
                
                break;

            case MotionEvent.ACTION_MOVE:
                // should check if has selected
                if (selectedShapeInView == null) break;

                timeMove = System.currentTimeMillis();
                long durationTime = timeMove - timeDown;

                if (durationTime > 500) {
                    vibrate();

                    isMoving = true;

                    float X1 = event.getX();
                    float Y1 = event.getY();

                    float deltaX = X1 - X;
                    float deltaY = Y1 - Y;

                    float prevX = selectedShapeInView.getRect().get(0);
                    float prevY = selectedShapeInView.getRect().get(1);
                    float width = selectedShapeInView.getRect().get(2);
                    float height = selectedShapeInView.getRect().get(3);
                    selectedShapeInView.setRect(
                            (int) (prevX + deltaX), (int) (prevY + deltaY),
                            (int) width, (int) height
                    );

                    activity.setSelectedShape(selectedShapeInView, false);

                    // the current point is the next start point
                    X = X1;
                    Y = Y1;
                }
                
                break;

            case MotionEvent.ACTION_UP:
                if (isMoving) {
                    undoSupporter.pushToUndoStack();
                }                
                break;               

        }
    }

    private void vibrate() {
        if (!isMoving) {
            Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
            vibrator.vibrate(100);
        }
    }

    // Helper function that checks if the given point is in the range of the given shape.
    private boolean inRange(float X, float Y, Shape shape) {
        float startX = shape.getRect().get(0);
        float startY = shape.getRect().get(1);
        float width = shape.getRect().get(2);
        float height = shape.getRect().get(3);
        return X >= startX && Y >= startY
                && X <= (startX + width) && Y <= (startY + height);
    }


    // It may be bad practice to have such global variables.
    // I need them to store the start point of my drawing for each shape.
    private float X0 = 0f;
    private float Y0 = 0f;
    private boolean haveAdded = false;

    private void addNewShape(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // When first click, record and update the start point position.
                // Notice: I do not create a new shape at first click without drag
                X0 = event.getX();
                Y0 = event.getY();
                haveAdded = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float X = event.getX();
                float Y = event.getY();

                Shape curShape;
                if (haveAdded) {
                    curShape = shapeList.get(shapeList.size() - 1);
                } else {
                    curShape = new Shape((int) X0, (int) Y0, (int) X, (int) Y);
                    game.addShapeToPage(curShape, page);
                    haveAdded = true;
                }

                curShape.setDiagonal((int) X0, (int) Y0, (int) X, (int) Y);
                activity.setSelectedShape(curShape, false);

                break;
            case MotionEvent.ACTION_UP:
                undoSupporter.pushToUndoStack();                    
                break;    
        }

    }

    // This function erases the shape that the user clicks on.
    private void deleteShape(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float X = event.getX();
                float Y = event.getY();

                for (int i = shapeList.size() - 1; i >= 0; i--) {
                    Shape curShape = shapeList.get(i);
                    // within range -> have found and should remove
                    if (inRange(X, Y, curShape)) {
                        game.deleteShapeFromPage(curShape, page);

                        // activate the save button
                        activity.setNeedSaveStatus(true);

                        break;
                    }
                }

                // set selected shape to null
                activity.setSelectedShape(null, false);
            
                undoSupporter.pushToUndoStack();
                
                break;
        }
    }

    private static void initMap() {
        String[] imageNameArray =
                {"carrot.png", "carrot2.png", "death.png", "duck.png", "fire.png", "mystic.png"};
        int[] idArray = {R.drawable.carrot, R.drawable.carrot2, R.drawable.death,
                R.drawable.duck, R.drawable.fire, R.drawable.mystic};
        for (int i = 0; i < imageNameArray.length; i++) {
            IMAGE_NAME_ID_MAP.put(imageNameArray[i], idArray[i]);
        }
    }
}