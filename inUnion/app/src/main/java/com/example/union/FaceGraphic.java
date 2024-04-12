package com.example.union;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

import com.example.union.R;
import com.example.union.ObscureType;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;
import java.util.Locale;


public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 8.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS =
            new int[][] {
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN},
                    {Color.BLACK, Color.GREEN}
            };

    private final Paint facePositionPaint;
    private final Paint[] idPaints;
    private final Paint[] boxPaints;
    private final Paint[] labelPaints;
    private final Bitmap smileyBitmap;
    private boolean isDrowsy;
    public RectF faceBoundingBox;
    private Face face;
    public int age;
    public int gender;
    public String name;
    public ObscureType obscureType = ObscureType.NONE;

    public FaceGraphic(GraphicOverlay overlay, Face face, boolean isDrowsy, int width, int height) {
        this(overlay, face, isDrowsy, width, height, -1, -1);
    }

    public FaceGraphic(GraphicOverlay overlay, Face face, boolean isDrowsy, int width, int height, int age, int gender) {
        super(overlay, width, height);
        smileyBitmap = BitmapFactory.decodeResource(overlay.getResources(), R.drawable.smiley);
        this.isDrowsy = isDrowsy;
        this.face = face;
        this.age = age;
        this. gender = gender;
        this.faceBoundingBox = transform(face.getBoundingBox());

        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1] /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (faceBoundingBox == null) {
            return;
        }

        float x = faceBoundingBox.centerX();
        float y = faceBoundingBox.centerY();
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        float left = faceBoundingBox.left;
        float top = faceBoundingBox.top;
        float right = faceBoundingBox.right;
        float bottom = faceBoundingBox.bottom;
        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

        int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

        float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());
        if (face.getSmilingProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(Locale.US, "Smiling: %.2f", face.getSmilingProbability())));
        }
        yLabelOffset -= lineHeight;
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "Drowsy: %s", isDrowsy)));

        if (face.getLeftEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Left eye open: %.2f", face.getLeftEyeOpenProbability())));
        }
        if (face.getRightEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Right eye open: %.2f", face.getRightEyeOpenProbability())));
        }

        yLabelOffset = yLabelOffset - 3 * lineHeight;
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerX: %.2f", face.getHeadEulerAngleX())));
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerY: %.2f", face.getHeadEulerAngleY())));
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerZ: %.2f", face.getHeadEulerAngleZ())));
        canvas.drawRect(
                left - BOX_STROKE_WIDTH,
                top + yLabelOffset,
                left + textWidth + (2 * BOX_STROKE_WIDTH),
                top,
                labelPaints[colorID]);
        yLabelOffset += ID_TEXT_SIZE;
        canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
        if (face.getTrackingId() != null) {
            canvas.drawText("", left, top + yLabelOffset, idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        if (obscureType == ObscureType.SMILEY) {
            canvas.drawBitmap(smileyBitmap, null, faceBoundingBox, null);

        } else if (obscureType == ObscureType.TRANSLUCENT) {
            Paint translucentPaint = new Paint();
            translucentPaint.setColor(Color.WHITE);
            translucentPaint.setAlpha(240);
            translucentPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            RectF faceOval = new RectF(faceBoundingBox);
            if (!face.getAllContours().isEmpty()) {
                float minX = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE;
                for (FaceContour faceContour : face.getAllContours()) {
                    if (faceContour.getFaceContourType() == FaceContour.FACE) {
                        for (PointF point : faceContour.getPoints()) {
                            minX = Math.min(minX, point.x);
                            maxX = Math.max(maxX, point.x);
                        }
                    }
                }
                faceOval.left = translateX(minX);
                faceOval.right = translateX(maxX);
            }
            canvas.drawOval(faceOval, translucentPaint);

        } else if (obscureType == ObscureType.NONE) {
            for (FaceContour contour : face.getAllContours()) {
                for (PointF point : contour.getPoints()) {
                    canvas.drawCircle(
                            translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
                }
            }
        }

        if (face.getSmilingProbability() != null) {
            canvas.drawText(
                    "Smiling: " + String.format(Locale.US, "%.2f", face.getSmilingProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        if (face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Drowsy: " + String.format(Locale.US, "%s", isDrowsy),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        if (face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }
        if (leftEye != null) {
            float leftEyeLeft =
                    translateX(leftEye.getPosition().x) - idPaints[colorID].measureText("Left Eye") / 2.0f;
            canvas.drawRect(
                    leftEyeLeft - BOX_STROKE_WIDTH,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
                    leftEyeLeft + idPaints[colorID].measureText("Left Eye") + BOX_STROKE_WIDTH,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
                    labelPaints[colorID]);
            canvas.drawText(
                    "Left Eye",
                    leftEyeLeft,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        }

        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        if (face.getRightEyeOpenProbability() != null) {
            canvas.drawText(
                    "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }
        if (rightEye != null) {
            float rightEyeLeft =
                    translateX(rightEye.getPosition().x) - idPaints[colorID].measureText("Right Eye") / 2.0f;
            canvas.drawRect(
                    rightEyeLeft - BOX_STROKE_WIDTH,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
                    rightEyeLeft + idPaints[colorID].measureText("Right Eye") + BOX_STROKE_WIDTH,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
                    labelPaints[colorID]);
            canvas.drawText(
                    "Right Eye",
                    rightEyeLeft,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        }



        if (age > -1) {
            canvas.drawText(
                    "Age: " + age, left, top + yLabelOffset, idPaints[colorID]);
        }
        yLabelOffset += lineHeight;
        Log.d("xx", "gender " + gender);
        if (gender != -1) {
            canvas.drawText("G: " + (gender == 0 ? "Male" : "Female"), left, top + yLabelOffset, idPaints[colorID]);
        }
        yLabelOffset += lineHeight;

        if (!TextUtils.isEmpty(name)) {
            canvas.drawText("", left, top + yLabelOffset, idPaints[colorID]);
        }
        yLabelOffset += lineHeight;

        drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);
    }

    private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
        FaceLandmark faceLandmark = face.getLandmark(landmarkType);
        if (faceLandmark != null) {
            canvas.drawCircle(
                    translateX(faceLandmark.getPosition().x),
                    translateY(faceLandmark.getPosition().y),
                    FACE_POSITION_RADIUS,
                    facePositionPaint);
        }
    }
}