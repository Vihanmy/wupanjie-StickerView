package com.xiaopo.flying.sticker.sticker.protocol;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xiaopo.flying.sticker.StickerUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author wupanjie
 */
public abstract class Sticker {

    @IntDef(flag = true, value = {
            Position.CENTER,
            Position.TOP,
            Position.BOTTOM,
            Position.LEFT,
            Position.RIGHT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Position {
        int CENTER = 1;
        int TOP = 1 << 1;
        int LEFT = 1 << 2;
        int RIGHT = 1 << 3;
        int BOTTOM = 1 << 4;
    }

    private final float[] matrixValues = new float[9];
    private final float[] unrotatedWrapperCorner = new float[8];
    private final float[] unrotatedPoint = new float[2];
    private final float[] boundPoints = new float[8];
    private final float[] mappedBounds = new float[8];
    private final RectF trappedRect = new RectF();
    private final Matrix matrix = new Matrix();
    private boolean isFlippedHorizontally;
    private boolean isFlippedVertically;


    @NonNull
    public Sticker setFlippedHorizontally(boolean flippedHorizontally) {
        isFlippedHorizontally = flippedHorizontally;
        return this;
    }

    @NonNull
    public Sticker setFlippedVertically(boolean flippedVertically) {
        isFlippedVertically = flippedVertically;
        return this;
    }

    public Sticker setMatrix(@Nullable Matrix matrix) {
        this.matrix.set(matrix);
        return this;
    }

    ///////////////////////////////////////////////////////// getters
    public boolean isFlippedHorizontally() {
        return isFlippedHorizontally;
    }

    public boolean isFlippedVertically() {
        return isFlippedVertically;
    }

    @NonNull
    public Matrix getMatrix() {
        return matrix;
    }

    public float[] getBoundPoints() {
        float[] points = new float[8];
        getBoundPoints(points);
        return points;
    }

    /**
     * 左上,右上,坐下, 右下 四个角的坐标
     */
    public void getBoundPoints(@NonNull float[] points) {
        if (!isFlippedHorizontally) {
            if (!isFlippedVertically) {
                points[0] = 0f;
                points[1] = 0f;
                points[2] = getWidth();
                points[3] = 0f;
                points[4] = 0f;
                points[5] = getHeight();
                points[6] = getWidth();
                points[7] = getHeight();
            } else {
                points[0] = 0f;
                points[1] = getHeight();
                points[2] = getWidth();
                points[3] = getHeight();
                points[4] = 0f;
                points[5] = 0f;
                points[6] = getWidth();
                points[7] = 0f;
            }
        } else {
            if (!isFlippedVertically) {
                points[0] = getWidth();
                points[1] = 0f;
                points[2] = 0f;
                points[3] = 0f;
                points[4] = getWidth();
                points[5] = getHeight();
                points[6] = 0f;
                points[7] = getHeight();
            } else {
                points[0] = getWidth();
                points[1] = getHeight();
                points[2] = 0f;
                points[3] = getHeight();
                points[4] = getWidth();
                points[5] = 0f;
                points[6] = 0f;
                points[7] = 0f;
            }
        }
    }

    @NonNull
    public float[] getMappedBoundPoints() {
        float[] dst = new float[8];
        getMappedPoints(dst, getBoundPoints());
        return dst;
    }

    @NonNull
    public float[] getMappedPoints(@NonNull float[] src) {
        float[] dst = new float[src.length];
        matrix.mapPoints(dst, src);
        return dst;
    }

    public void getMappedPoints(@NonNull float[] dst, @NonNull float[] src) {
        matrix.mapPoints(dst, src);
    }

    @NonNull
    public RectF getBound() {
        RectF bound = new RectF();
        getBound(bound);
        return bound;
    }

    public void getBound(@NonNull RectF dst) {
        dst.set(0, 0, getWidth(), getHeight());
    }

    @NonNull
    public RectF getMappedBound() {
        RectF dst = new RectF();
        getMappedBound(dst, getBound());
        return dst;
    }

    public void getMappedBound(@NonNull RectF dst, @NonNull RectF bound) {
        matrix.mapRect(dst, bound);
    }

    @NonNull
    public PointF getCenterPoint() {
        PointF center = new PointF();
        getCenterPoint(center);
        return center;
    }

    public void getCenterPoint(@NonNull PointF dst) {
        dst.set(getWidth() * 1f / 2, getHeight() * 1f / 2);
    }

    @NonNull
    public PointF getMappedCenterPoint() {
        PointF pointF = getCenterPoint();
        getMappedCenterPoint(pointF, new float[2], new float[2]);
        return pointF;
    }

    public void getMappedCenterPoint(
            @NonNull PointF dst,
            @NonNull float[] mappedPoints,
            @NonNull float[] src) {
        getCenterPoint(dst);
        src[0] = dst.x;
        src[1] = dst.y;
        getMappedPoints(mappedPoints, src);
        dst.set(mappedPoints[0], mappedPoints[1]);
    }

    public float getCurrentScale() {
        return getMatrixScale(matrix);
    }

    public float getCurrentHeight() {
        return getMatrixScale(matrix) * getHeight();
    }

    public float getCurrentWidth() {
        return getMatrixScale(matrix) * getWidth();
    }

    /**
     * This method calculates scale value for given Matrix object.
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2) + Math.pow(
                getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * @return - current image rotation angle.
     */
    public float getCurrentAngle() {
        return getMatrixAngle(matrix);
    }

    /**
     * This method calculates rotation angle for given Matrix object.
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) Math.toDegrees(-(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X))));
    }

    public float getMatrixValue(@NonNull Matrix matrix, @IntRange(from = 0, to = 9) int valueIndex) {
        matrix.getValues(matrixValues);
        return matrixValues[valueIndex];
    }

    ///////////////////////////////////////////////////////// utils

    /**
     * 判断某个坐标值是否是在当前 sticker 显示的范围中
     *
     * @param x StickerView 中的x轴坐标值
     * @param y StickerView 中的y轴坐标值
     * @return 判断结果
     */
    public boolean contains(float x, float y) {
        //return contains(new float[]{x, y});
        // REFA: Vihanmy 优化这里的运算效率应该, 这里已经new了3个对象了
        getMappedPoints(boundPoints, getBoundPoints());
        float x1 = boundPoints[0];
        float y1 = boundPoints[1];
        float x2 = boundPoints[2];
        float y2 = boundPoints[3];
        float x3 = boundPoints[4];
        float y3 = boundPoints[5];
        float x4 = boundPoints[6];
        float y4 = boundPoints[7];

        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x4, y4);
        path.lineTo(x3, y3);
        path.close();

        RectF bounds = new RectF();
        path.computeBounds(bounds, false);

        Region clipRegion = new Region();
        clipRegion.set((int) (bounds.left), (int) (bounds.top), (int) (bounds.right), (int) (bounds.bottom));

        RectF rectF = getMappedBound();
        Region re = new Region();
        re.setPath(path, clipRegion);
        return re.contains((int) x, (int) y);
    }

    public boolean contains(@NonNull float[] point) {
        Matrix tempMatrix = new Matrix();
        tempMatrix.setRotate(-getCurrentAngle());
        getBoundPoints(boundPoints);
        getMappedPoints(mappedBounds, boundPoints);
        tempMatrix.mapPoints(unrotatedWrapperCorner, mappedBounds);
        tempMatrix.mapPoints(unrotatedPoint, point);
        StickerUtils.trapToRect(trappedRect, unrotatedWrapperCorner);
        return trappedRect.contains(unrotatedPoint[0], unrotatedPoint[1]);
    }

    public void release() {
    }


    /////////////////////////////////////////////////////////
    public abstract void draw(@NonNull Canvas canvas);

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract Sticker setDrawable(@NonNull Drawable drawable);

    @NonNull
    public abstract Drawable getDrawable();

    @NonNull
    public abstract Sticker setAlpha(@IntRange(from = 0, to = 255) int alpha);
}
