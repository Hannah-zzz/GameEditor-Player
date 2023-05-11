package com.example.bunnyworld.view;



import android.content.Context;
//import android.content.Intent;
//import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
//import android.net.Uri;
import android.os.Vibrator;
//import android.provider.DocumentsContract;
//import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import com.example.bunnyworld.R;
//import com.example.bunnyworld.activity.EditShapeScriptActivity;
import com.example.bunnyworld.activity.PlayGameActivity;
import com.example.bunnyworld.databinding.ActivityPlayGameBinding;
import com.example.bunnyworld.entity.Game;
import com.example.bunnyworld.entity.Page;
import com.example.bunnyworld.entity.ScriptAction;
import com.example.bunnyworld.entity.Shape;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Math;
import java.util.Objects;

//import com.example.bunnyworld.databinding.ActivityEditShapeScriptBinding;


public class PageView extends View {

    private Game game;
    private Page page;
    private List<Shape> shapeList;
    private List<Shape> inventoryList;
    private Context context;
    public static final Map<String, Integer> IMAGE_NAME_ID_MAP = new HashMap<>();
    private ActivityPlayGameBinding binding;
    private Paint generalPaint;
    private Paint selectedPaint;
    private Paint fillingPaint;
    private Paint boarderPaint;
    private Paint possessionsPaint;
    int viewWidth, viewHeight;
    private long timeDown = 0l, timeMove;
    private float X = 0f, Y = 0f;

    private float lastX = 0f, lastY = 0f;
    private int inventory_top;
    //boolean isMoving;
    private Shape selectedShapeInView = null;
    private List<Integer> rect;

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


    public static float convertDpToPixel(int dp, Context context){
        return (float) dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initPaint();
        initMap();
        initInternalSoundIDMap();
        this.inventory_top = (int) convertDpToPixel(400, this.context);
    }

    public void setBinding(ActivityPlayGameBinding binding) {
        this.binding = binding;
    }


    public void setGame(Game game) {
        this.game = game;
    }

    public void setPage(Page page) {
        this.page = page;
        this.shapeList = page.getShapeList();
        setInventory();
        //this.page.setName(page.getName());
        for(Shape s: shapeList) {
            String script1 = "";
            script1 = s.getScript();
            String[] temp = script1.split(";");
            for (String scrip: temp){
                String[] words1 = scrip.split(" ");

                if(words1.length != 1) {
                    String trigger1 = words1[1];

                    if (trigger1.compareToIgnoreCase("enter") == 0) {
                        System.out.println(trigger1);
                        //handle on enter event
                        //loop over all shapes and any on enter actions
                        String action1 = words1[2];
                        String destinationOrSoundOrName = words1[3];
                        doAction(action1, destinationOrSoundOrName);
                    }
                }
            }
        }
        invalidate();
    }

    private void doAction(String action1, String destinationOrSoundOrName) {
        if (action1.compareToIgnoreCase("goto") == 0) {
            page = game.getPage(destinationOrSoundOrName);
            setPage(page);
        }
        if (action1.compareToIgnoreCase("play") == 0) {
            MediaPlayer mp;
            String soundName = destinationOrSoundOrName;
            if (!SOUND_NAME_PATH_MAP.containsKey(soundName)) {
                // internal
                mp = MediaPlayer.create(
                        this.context, INTERNAL_SOUND_ID_MAP.get(soundName));
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

        if (action1.compareToIgnoreCase("hide") == 0) {
            for(Page p: game.getPageList()) {
                for (Shape check : p.getShapeList()) {
                    if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                        System.out.println("here");
                        check.setVisible(false);
                        break;
                    }
                }
            }
            for(Shape check: game.getInventory()){
                if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                    System.out.println("here");
                    check.setVisible(false);
                    break;
                }
            }
        }
        if (action1.compareToIgnoreCase("show") == 0) {
            for(Page p: game.getPageList()) {
                for (Shape check : p.getShapeList()) {
                    if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                        check.setVisible(true);
                        break;
                    }
                }
            }
            for(Shape check: game.getInventory()){
                if (destinationOrSoundOrName.compareToIgnoreCase(check.getName()) == 0) {
                    check.setVisible(true);
                    break;
                }
            }
        }
    }



    private void initInternalSoundIDMap() {
        int[]IDArray = {R.raw.carrotcarrotcarrot, R.raw.evillaugh, R.raw.fire,
                R.raw.hooray, R.raw.munch, R.raw.munching, R.raw.woof};
        for (int i = 0; i < 7; i++) {
            INTERNAL_SOUND_ID_MAP.put(SOUND_NAME_LIST.get(i), IDArray[i]);
        }
    }

    public void setInventory() {
        this.inventoryList = this.game.getInventory();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
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
        // setup boarderPaint
        boarderPaint = new Paint();
        boarderPaint.setColor(Color.BLACK);
        boarderPaint.setStyle(Paint.Style.STROKE);
        boarderPaint.setStrokeWidth(5.0f);
        // setup paint to write possessions
        possessionsPaint = new Paint();
        possessionsPaint.setColor(Color.GRAY);
        possessionsPaint.setStyle(Paint.Style.FILL);
        possessionsPaint.setTextSize(60.0f);
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


    @Override
    protected void onDraw(Canvas canvas) {
        //handle the on event of any shapes
        super.onDraw(canvas);
        canvas.drawLine(0.0f, (float) inventory_top, this.viewWidth, (float) inventory_top, boarderPaint);
        canvas.drawText("Possessions:", 20.0f, ((float) inventory_top + 60.0f), possessionsPaint);
       // canvas.drawRect(50.0f,inventory_top,this.viewWidth-50.0f,this.viewHeight-50.0f,boarderPaint);
        // ask the shapes to draw themselves
        for (Shape shape : page.getShapeList()) {
            if(shape.getVisible()) {
                if(!Objects.isNull(selectedShapeInView) && shape.isDropTarget(selectedShapeInView.getName())){
                    shape.outlineShape(canvas, selectedPaint);
                    drawShape(canvas, shape);
                } else {
                    drawShape(canvas, shape);
                }
            }
        }
        for (Shape shape : inventoryList) {
            drawShape(canvas, shape);
        }
        if (!(selectedShapeInView == null)) {
            drawShape(canvas, selectedShapeInView);
        }
    }


    private void drawShape(Canvas canvas, Shape shape) {
        // turn image to drawable
        BitmapDrawable shapeImageDrawable;
        if (shape.getImage() != null && !shape.getImage().isEmpty()) {
            Integer internalImageId = IMAGE_NAME_ID_MAP.get(shape.getImage());
            if (internalImageId != null) {
                // internal preload image
                shapeImageDrawable = (BitmapDrawable) getResources().getDrawable(internalImageId);
            } else {
                Bitmap image = BitmapFactory.decodeFile(shape.getImage());
                // check if successfully read the image from storage
                if (image == null) {
                    Toast.makeText(context, "External image not found.", Toast.LENGTH_SHORT);
                    shapeImageDrawable = null;
                } else {
                    shapeImageDrawable = new BitmapDrawable(image);
                }
            }
        } else {
            shapeImageDrawable = null;
        }

        //to handle if shape is put on the boarder between page and possessions/inventory
        if(shape.getVisible()) {
            shape.drawShape(canvas, generalPaint, fillingPaint, shapeImageDrawable);
        }
    }


    //when user touches game page, check if they are touching aka selecting a shape
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // record time down
                timeDown = System.currentTimeMillis();
                selectShape(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (selectedShapeInView == null) break;
                if(selectedShapeInView.getMovable()){
                    moveSelectedShape(event);
                }
                return true;
            case MotionEvent.ACTION_UP:
                checkDrop(event);
                ((PlayGameActivity) context).setSelectedShape(null, false);
                selectedShapeInView = null;
                fixAnyBoarderShapes();
                invalidate();
                return true;
        }
        return false;
    }


    private void checkDrop(MotionEvent event) {
        if(!Objects.isNull(selectedShapeInView)) {
            X = event.getX(); //maybe take out
            Y = event.getY(); //Maybe take out

            for (int i = page.getShapeList().size() - 1; i >= 0; i--) {
                Shape curShape = shapeList.get(i);
                if (inRange(X, Y, curShape)) {
                    if(curShape.isDropTarget(selectedShapeInView.getName())) {
                        List<ScriptAction> toDo = curShape.handleDrop(selectedShapeInView.getName());
                        if(!toDo.isEmpty()){
                            for (int j = 0; j < toDo.size(); j++) {
                                String action = toDo.get(j).getAction();
                                String subjectOfAction = toDo.get(j).getNameToDoActionOn();
                                doAction(action, subjectOfAction);
                            }
                            return;
                        }
                    } else if (selectedShapeInView.getName().compareToIgnoreCase(curShape.getName()) != 0){
                        selectedShapeInView.setRect(
                                (int) lastX, (int) lastY,
                                (int) selectedShapeInView.getWidth(), (int) selectedShapeInView.getHeight());
                        //Toast.makeText(context, "Not There!", Toast.LENGTH_SHORT);
                        return;
                    }
                }
            }
        }
    }


    private void fixAnyBoarderShapes(){
        for (int i = 0; i < shapeList.size(); i++) {
            Shape curShape = shapeList.get(i);
            int top = curShape.getTopCoord();
            int height = curShape.getHeight();
            if((top < this.inventory_top) && ((top + height) > this.inventory_top)) {
                //if more is in the page view
                if(Math.abs(top - this.inventory_top) >  Math.abs(top + height - this.inventory_top)) {
                    curShape.setRect(curShape.getLeftCoord(),((this.inventory_top) - height - 20), curShape.getWidth(), height);
                    game.inventoryToPage(curShape, page);
                } else {
                    curShape.setRect(curShape.getLeftCoord(),(this.inventory_top + 70), curShape.getWidth(), height);
                    game.addToInventory(curShape,page);
                }
            }
        }
        for (int i = 0; i < inventoryList.size(); i++) {
            Shape curShape = inventoryList.get(i);
            int top = curShape.getTopCoord();
            int height = curShape.getHeight();
            int left = curShape.getLeftCoord();
            int width = curShape.getWidth();
            if((top < this.inventory_top) && ((top + height) > this.inventory_top)) {
                //if more is in the page view
                if(Math.abs(top - this.inventory_top) >  Math.abs(top + height - this.inventory_top)) {
                    curShape.setRect(left,((this.inventory_top) - height - 20), width, height); 
                    game.inventoryToPage(curShape, page);
                } else {
                    curShape.setRect(left,(this.inventory_top + 70), width, height);
                    game.addToInventory(curShape,page);
                }
                //top = curShape.getTopCoord();
            }
        }
    }

    private void selectShape(MotionEvent event) {
        X = event.getX();
        Y = event.getY();
        boolean haveFound = false;

        for (int i = shapeList.size() - 1; i >= 0; i--) {
            Shape curShape = shapeList.get(i);
            // with in range -> have found
            if (inRange(X, Y, curShape)) {
                // notify the activity of selection
                ((PlayGameActivity) context).setSelectedShape(curShape, false); //maybe don't need
                selectedShapeInView = curShape;
                haveFound = true;

                //redraw selected shape at front
                selectedShapeInView.setRect(
                        (int) (selectedShapeInView.getLeftCoord()), (int) (selectedShapeInView.getTopCoord()),
                        (int) selectedShapeInView.getWidth(), (int) selectedShapeInView.getHeight());
                break;

            }
        }
        for (int i = inventoryList.size() - 1; i >= 0; i--) {
            Shape curShape = inventoryList.get(i);
            // with in range -> have found
            if (inRange(X, Y, curShape)) {
                // notify the activity of selection
                ((PlayGameActivity) context).setSelectedShape(curShape, false); //maybe don't need
                selectedShapeInView = curShape;
                haveFound = true;
                break;
            }
        }
        if(haveFound) {
            lastX = this.selectedShapeInView.getLeftCoord();
            lastY = this.selectedShapeInView.getTopCoord();
        }
        if (!haveFound) {
            ((PlayGameActivity) context).setSelectedShape(null, false);
            selectedShapeInView = null;
        }
        invalidate();
    }

    private void moveSelectedShape(MotionEvent event){

        //find coordinates of where the user is now touching
        float X1 = event.getX();
        float Y1 = event.getY();
        //find how the distance that the user has moved their touch
        float deltaX = X1 - X;
        float deltaY = Y1 - Y;

        float prevX = selectedShapeInView.getLeftCoord();
        float prevY = selectedShapeInView.getTopCoord();
        float width = selectedShapeInView.getWidth();
        float height = selectedShapeInView.getHeight();

        selectedShapeInView.setRect(
                (int) (prevX + deltaX), (int) (prevY + deltaY),
                (int) width, (int) height);

        ((PlayGameActivity) context).setSelectedShape(selectedShapeInView, false);

        //if shape is moved into possession area
        if (this.inventory_top < (prevY + deltaY - height/2) ) {
            game.addToInventory(selectedShapeInView, page);
        }

        //if the shape is now in the page area
        if((Y1-height/2) < this.inventory_top){
            game.inventoryToPage(selectedShapeInView, page);
        }

        // the current point is the next start point
        X = X1;
        Y = Y1;
        invalidate();
    }


    // Helper function that checks if the given point is in the range of the given shape.
    // aka is there a shape where the user touched
    private boolean inRange(float X, float Y, Shape shape) {
        float startX = shape.getRect().get(0);
        float startY = shape.getRect().get(1);
        float width = shape.getRect().get(2);
        float height = shape.getRect().get(3);
        return X >= startX && Y >= startY
                && X <= (startX + width) && Y <= (startY + height);
    }

}
