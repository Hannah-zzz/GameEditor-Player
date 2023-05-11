package com.example.bunnyworld.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.bunnyworld.R;
import com.example.bunnyworld.databinding.ActivityCustomDrawingBinding;
import com.example.bunnyworld.utility.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

public class CustomDrawingView extends View {

    private int viewWidth;
    private int viewHeight;
    private float preX;
    private float preY;
    private Path path;
    public Paint paint;
    Bitmap cacheBitmap=null;
    Canvas cacheCanvas=null;

    private Context context;

    private ActivityCustomDrawingBinding binding;

    public CustomDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    private void init() {
        // these two maybe problemSome
        this.viewWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.viewHeight = (int) (400 * context.getResources().getDisplayMetrics().density);

        // bitmap and canvas should be in cache
        this.cacheBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        this.cacheCanvas = new Canvas();
        this.path = new Path();

        // draw cacheBitmap on cacheCanvas
        this.cacheCanvas.setBitmap(cacheBitmap);
        this.paint = new Paint(Paint.DITHER_FLAG);// prevent shaking
        this.paint.setColor(Color.BLACK);

        // init paint
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(1);
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(0xFFFFFFFF); // background color
        Paint bmpPaint = new Paint();
        canvas.drawBitmap(cacheBitmap, 0, 0, bmpPaint);// draw cacheBitmap
        canvas.drawPath(path, paint);
        canvas.save(); // use new save() here
        canvas.restore();
    }

    public void setBinding(ActivityCustomDrawingBinding binding) {
        this.binding = binding;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                // the starting point of new path
                path.moveTo(x, y);
                preX=x;
                preY=y;
                break;
            case MotionEvent.ACTION_MOVE:
                // control the vertical and horizontal distance to be smaller than 625
                float dx = Math.abs(x-preX);
                float dy = Math.abs(y-preY);
                if (dx < 625 || dy < 625) {
                    path.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
                    preX = x;
                    preY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                cacheCanvas.drawPath(path, paint);
                path.reset();
                break;
        }
        invalidate();
        return true;
    }

    // This function allows the touch on shapeDrawingView to not be interfered by the scroll view
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    public void clear(){
        // when overlaps
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setStrokeWidth(50);
    }

    // The input is sure to be not duplicate with th existing files.
    public void save(String imageFileName){

        try {
            saveBitmap(imageFileName);
        } catch (RuntimeException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveBitmap(String fileName) throws IOException {
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Pictures");

        String realPath = "";
        try {
            realPath = PathUtil.getPath(context, uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        File file = new File(realPath + "/" + fileName +".png");
        file.createNewFile();
        FileOutputStream fileOS = new FileOutputStream(file);
        // compress the file to png
        cacheBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOS);
        fileOS.flush();// write from cache to outputStream
        fileOS.close();
    }

}

// reference: https://cloud.tencent.com/developer/article/1734049
