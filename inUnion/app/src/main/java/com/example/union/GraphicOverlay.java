package com.example.union;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.camera.core.CameraSelector;

import java.util.ArrayList;
import java.util.List;


public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();

    public int previewWidth;
    public int previewHeight;
    public boolean isLensFacingFront;


    public abstract static class Graphic {

        private final int imageWidth;

        private final int imageHeight;

        private final GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay, int width, int height) {
            this.overlay = overlay;
            imageWidth = width;
            imageHeight = height;
        }


        public abstract void draw(Canvas canvas);

        public RectF transform(Rect rect) {
            float scaleX = overlay.previewWidth / (float) imageWidth;
            float scaleY = overlay.previewHeight / (float) imageHeight;
            float flippedLeft;
            if (overlay.isLensFacingFront)
                flippedLeft = imageWidth - rect.right;
            else
                flippedLeft = rect.left;
            float flippedRight;
            if (overlay.isLensFacingFront)
                flippedRight = imageWidth - rect.left;
            else
                flippedRight = rect.right;

            float scaledLeft = scaleX * flippedLeft;
            float scaledTop = scaleY * rect.top;
            float scaledRight = scaleX * flippedRight;
            float scaledBottom = scaleY * rect.bottom;

            return new RectF(scaledLeft, scaledTop, scaledRight, scaledBottom);
        }


        public float translateX(float x) {
            float scaleX = overlay.previewWidth / (float) imageWidth;

            float flippedX;
            if (overlay.isLensFacingFront) {
                flippedX = imageWidth - x;
            } else {
                flippedX = x;
            }
            return flippedX * scaleX;
        }

        public float translateY(float y) {
            float scaleY = overlay.previewHeight / (float) imageHeight;
            return y * scaleY;
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        postInvalidate());
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

    public void setPreviewProperties(int previewWidth, int previewHeight, int lensFacing) {
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.isLensFacingFront = CameraSelector.LENS_FACING_FRONT == lensFacing;
    }
}