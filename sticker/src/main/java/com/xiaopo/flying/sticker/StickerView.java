package com.xiaopo.flying.sticker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.xiaopo.flying.sticker.iconevent.DeleteIconEvent;
import com.xiaopo.flying.sticker.iconevent.FlipHorizontallyEvent;
import com.xiaopo.flying.sticker.iconevent.ZoomIconEvent;
import com.xiaopo.flying.sticker.sticker.protocol.Sticker;
import com.xiaopo.flying.sticker.stickericon.BitmapStickerIcon;
import com.xiaopo.flying.sticker.util.LOGG;
import com.xiaopo.flying.sticker.util.MatrixUtilKt;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sticker View
 *
 * @author wupanjie
 */
public class StickerView extends FrameLayout {

    private final boolean showIcons;
    private final boolean showBorder;
    private final boolean bringToFrontCurrentSticker;

    @IntDef({
            ActionMode.NONE, ActionMode.DRAG, ActionMode.ZOOM_WITH_TWO_FINGER, ActionMode.ICON,
            ActionMode.CLICK
    })
    @Retention(RetentionPolicy.SOURCE)
    protected @interface ActionMode {
        int NONE = 0;
        int DRAG = 1;
        int ZOOM_WITH_TWO_FINGER = 2;
        int ICON = 3;
        int CLICK = 4;
    }

    @IntDef(flag = true, value = {FLIP_HORIZONTALLY, FLIP_VERTICALLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flip {
    }

    private final float[] handingStickerDownBoundsPoints = new float[8];  //手指按下时,选中图层在画布中的显示范围四个角的落脚点

    private static final String TAG = "StickerView";

    private static final int DEFAULT_MIN_CLICK_DELAY_TIME = 200;

    public static final int FLIP_HORIZONTALLY = 1;
    public static final int FLIP_VERTICALLY = 1 << 1;

    private final List<Sticker> stickers = new ArrayList<>();
    private final List<BitmapStickerIcon> icons = new ArrayList<>(4);

    private final Paint borderPaint = new Paint();
    private final RectF stickerRect = new RectF();

    private final Matrix sizeMatrix = new Matrix();
    private final Matrix downMatrix = new Matrix();
    private final Matrix moveMatrix = new Matrix();

    // region storing variables
    private final float[] bitmapPoints = new float[8];
    private final float[] bounds = new float[8];
    private final float[] point = new float[2];
    private final PointF currentCenterPoint = new PointF();
    private final float[] tmp = new float[2];
    private PointF midPoint = new PointF();
    // endregion
    private final int touchSlop;

    private BitmapStickerIcon currentIcon;
    //the first point down position
    private float downX;
    private float downY;

    private float oldDistance = 0f;
    private float oldRotation = 0f;

    @ActionMode
    private int currentMode = ActionMode.NONE;

    private Sticker handlingSticker;

    private boolean locked;
    private boolean constrained;

    private OnStickerOperationListener onStickerOperationListener;

    private long lastClickTime = 0;
    private int minClickDelayTime = DEFAULT_MIN_CLICK_DELAY_TIME;

    public StickerView(Context context) {
        this(context, null);
    }

    public StickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.StickerView);
            showIcons = a.getBoolean(R.styleable.StickerView_showIcons, false);
            showBorder = a.getBoolean(R.styleable.StickerView_showBorder, false);
            bringToFrontCurrentSticker =
                    a.getBoolean(R.styleable.StickerView_bringToFrontCurrentSticker, false);

            borderPaint.setAntiAlias(true);
            borderPaint.setColor(a.getColor(R.styleable.StickerView_borderColor, Color.BLACK));
            borderPaint.setAlpha(a.getInteger(R.styleable.StickerView_borderAlpha, 128));

            configDefaultIcons();
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public void configDefaultIcons() {
        BitmapStickerIcon deleteIcon = new BitmapStickerIcon(
                ContextCompat.getDrawable(getContext(), R.drawable.sticker_ic_close_white_18dp),
                BitmapStickerIcon.LEFT_TOP);
        deleteIcon.setIconEvent(new DeleteIconEvent());
        BitmapStickerIcon zoomIcon = new BitmapStickerIcon(
                ContextCompat.getDrawable(getContext(), R.drawable.sticker_ic_scale_white_18dp),
                BitmapStickerIcon.RIGHT_BOTTOM);
        zoomIcon.setIconEvent(new ZoomIconEvent());
        BitmapStickerIcon flipIcon = new BitmapStickerIcon(
                ContextCompat.getDrawable(getContext(), R.drawable.sticker_ic_flip_white_18dp),
                BitmapStickerIcon.RIGHT_TOP);
        flipIcon.setIconEvent(new FlipHorizontallyEvent());

        icons.clear();
        icons.add(deleteIcon);
        icons.add(zoomIcon);
        icons.add(flipIcon);
    }

    /**
     * Swaps sticker at layer [[oldPos]] with the one at layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    public void swapLayers(int oldPos, int newPos) {
        if (stickers.size() >= oldPos && stickers.size() >= newPos) {
            Collections.swap(stickers, oldPos, newPos);
            invalidate();
        }
    }

    /**
     * Sends sticker from layer [[oldPos]] to layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    public void sendToLayer(int oldPos, int newPos) {
        if (stickers.size() >= oldPos && stickers.size() >= newPos) {
            Sticker s = stickers.get(oldPos);
            stickers.remove(oldPos);
            stickers.add(newPos, s);
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            stickerRect.left = left;
            stickerRect.top = top;
            stickerRect.right = right;
            stickerRect.bottom = bottom;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawStickers(canvas);
    }

    protected void drawStickers(Canvas canvas) {

        //【】绘制sticker
        for (int i = 0; i < stickers.size(); i++) {
            Sticker sticker = stickers.get(i);
            if (sticker != null) {
                sticker.draw(canvas);
            }
        }

        //【】绘制框线和四周的触点
        if (handlingSticker != null && !locked && (showBorder || showIcons)) {

            //计算四个角落点的坐标
            getStickerPoints(handlingSticker, bitmapPoints);
            float x1 = bitmapPoints[0];
            float y1 = bitmapPoints[1];
            float x2 = bitmapPoints[2];
            float y2 = bitmapPoints[3];
            float x3 = bitmapPoints[4];
            float y3 = bitmapPoints[5];
            float x4 = bitmapPoints[6];
            float y4 = bitmapPoints[7];

            if (showBorder) {
                canvas.drawLine(x1, y1, x2, y2, borderPaint);
                canvas.drawLine(x1, y1, x3, y3, borderPaint);
                canvas.drawLine(x2, y2, x4, y4, borderPaint);
                canvas.drawLine(x4, y4, x3, y3, borderPaint);
            }

            //draw icons
            if (showIcons) {
                float rotation = StickerUtil.INSTANCE.calculateRotation(x4, y4, x3, y3);
                for (int i = 0; i < icons.size(); i++) {
                    BitmapStickerIcon icon = icons.get(i);
                    switch (icon.getPosition()) {
                        case BitmapStickerIcon.LEFT_TOP:
                            configIconMatrix(icon, x1, y1, rotation);
                            break;
                        case BitmapStickerIcon.RIGHT_TOP:
                            configIconMatrix(icon, x2, y2, rotation);
                            break;
                        case BitmapStickerIcon.LEFT_BOTTOM:
                            configIconMatrix(icon, x3, y3, rotation);
                            break;
                        case BitmapStickerIcon.RIGHT_BOTTOM:
                            configIconMatrix(icon, x4, y4, rotation);
                            break;


                        //计算四个边中心点的坐标
                        case BitmapStickerIcon.CENTER_TOP:
                            configIconMatrix(icon, (x1 + x2) / 2f, (y1 + y2) / 2f, rotation);
                            break;
                        case BitmapStickerIcon.CENTER_RIGHT:
                            configIconMatrix(icon, (x2 + x4) / 2f, (y2 + y4) / 2f, rotation);
                            break;
                        case BitmapStickerIcon.CENTER_LEFT:
                            configIconMatrix(icon, (x1 + x3) / 2f, (y1 + y3) / 2f, rotation);
                            break;
                        case BitmapStickerIcon.CENTER_BOTTOM:
                            configIconMatrix(icon, (x3 + x4) / 2f, (y3 + y4) / 2f, rotation);
                            break;
                    }
                    icon.draw(canvas, borderPaint);
                }
            }
        }
    }

    protected void configIconMatrix(@NonNull BitmapStickerIcon icon, float x, float y,
                                    float rotation) {
        icon.setX(x);
        icon.setY(y);
        icon.getMatrix().reset();

        icon.getMatrix().postRotate(rotation, icon.getWidth() / 2, icon.getHeight() / 2);
        icon.getMatrix().postTranslate(x - icon.getWidth() / 2, y - icon.getHeight() / 2);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (locked) return super.onInterceptTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();

                return findCurrentIconTouched() != null || findHandlingSticker() != null;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (locked) {
            return super.onTouchEvent(event);
        }

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!onTouchDown(event)) {
                    return false;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDistance = StickerUtil.INSTANCE.calculateDistance(event);
                oldRotation = StickerUtil.INSTANCE.calculateRotation(event);

                midPoint = StickerUtil.INSTANCE.calculateMidPoint(event, midPoint);

                if (
                        handlingSticker != null
                                && handlingSticker.contains(event.getX(1), event.getY(1))
                                && findCurrentIconTouched() == null
                ) {
                    currentMode = ActionMode.ZOOM_WITH_TWO_FINGER;   ///█ 判定为双指
                }
                break;

            case MotionEvent.ACTION_MOVE:
                handleCurrentMode(event);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                onTouchUp(event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (currentMode == ActionMode.ZOOM_WITH_TWO_FINGER && handlingSticker != null) {
                    if (onStickerOperationListener != null) {
                        onStickerOperationListener.onStickerZoomFinished(handlingSticker);
                    }
                }
                currentMode = ActionMode.NONE; //█ 多指事件结束
                break;
        }

        return true;
    }

    /**
     * 初始化操作 + 判断是否点击到了某个 sticker 的区域
     *
     * @param event MotionEvent received from {@link #onTouchEvent)
     * @return true if has touch something
     */
    protected boolean onTouchDown(@NonNull MotionEvent event) {
        currentMode = ActionMode.DRAG;  //█ 事件开始, 预设为拖拽

        downX = event.getX();
        downY = event.getY();

        midPoint = calculateMidPoint();         // 显示的Sticker中心, 在StickerView坐标系中的坐标
        oldDistance = StickerUtil.INSTANCE.calculateDistance(midPoint.x, midPoint.y, downX, downY);  //开始触摸点与sticker显示中心的初始距离
        oldRotation = StickerUtil.INSTANCE.calculateRotation(midPoint.x, midPoint.y, downX, downY);  //开始触摸点与sticker显示中心的初始角度

        //【】确定点击的是sticker还是四周的触摸描点
        currentIcon = findCurrentIconTouched();
        if (currentIcon != null) {
            currentMode = ActionMode.ICON; //█ 判定点击的为控制点
            currentIcon.onActionDown(this, event);
        } else {
            handlingSticker = findHandlingSticker();
        }

        //【】
        if (handlingSticker != null) {
            downMatrix.set(handlingSticker.getMatrix());

            if (bringToFrontCurrentSticker) {
                stickers.remove(handlingSticker);  //点击后是否将sticker移动到顶层
                stickers.add(handlingSticker);
            }

            if (onStickerOperationListener != null) {  //sticker被点击的事件
                onStickerOperationListener.onStickerTouchedDown(handlingSticker);
            }
            handlingSticker.getMappedPoints(handingStickerDownBoundsPoints, handlingSticker.getBoundPoints());
        }

        if (currentIcon == null && handlingSticker == null) {
            return false;
        }
        invalidate();
        return true;
    }

    protected void onTouchUp(@NonNull MotionEvent event) {
        long currentTime = SystemClock.uptimeMillis();

        if (currentMode == ActionMode.ICON && currentIcon != null && handlingSticker != null) {
            currentIcon.onActionUp(this, event);
        }

        if (currentMode == ActionMode.DRAG
                && Math.abs(event.getX() - downX) < touchSlop
                && Math.abs(event.getY() - downY) < touchSlop
                && handlingSticker != null) {
            currentMode = ActionMode.CLICK; //█ 判定为点击
            if (onStickerOperationListener != null) {
                onStickerOperationListener.onStickerClicked(handlingSticker);
            }
            if (currentTime - lastClickTime < minClickDelayTime) {
                if (onStickerOperationListener != null) {
                    onStickerOperationListener.onStickerDoubleTapped(handlingSticker);
                }
            }
        }

        if (currentMode == ActionMode.DRAG && handlingSticker != null) {
            if (onStickerOperationListener != null) {
                onStickerOperationListener.onStickerDragFinished(handlingSticker);
            }
        }

        currentMode = ActionMode.NONE; //█ 单指事件结束
        lastClickTime = currentTime;
    }

    protected void handleCurrentMode(@NonNull MotionEvent event) {
        switch (currentMode) {
            case ActionMode.NONE:
            case ActionMode.CLICK:
                break;
            case ActionMode.DRAG:
                if (handlingSticker != null) {
                    moveMatrix.set(downMatrix);
                    moveMatrix.postTranslate(event.getX() - downX, event.getY() - downY);
                    handlingSticker.setMatrix(moveMatrix);
                    if (constrained) {
                        constrainSticker(handlingSticker);
                    }
                }
                break;
            case ActionMode.ZOOM_WITH_TWO_FINGER:
                if (handlingSticker != null) {
                    float newDistance = StickerUtil.INSTANCE.calculateDistance(event);
                    float newRotation = StickerUtil.INSTANCE.calculateRotation(event);

                    moveMatrix.set(downMatrix);
                    moveMatrix.postScale(newDistance / oldDistance, newDistance / oldDistance, midPoint.x,
                            midPoint.y);
                    moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y);
                    handlingSticker.setMatrix(moveMatrix);
                }

                break;

            case ActionMode.ICON:
                if (handlingSticker != null && currentIcon != null) {
                    currentIcon.onActionMove(this, event);
                }
                break;
        }
    }

    public void zoomAndRotateCurrentSticker(@NonNull MotionEvent event) {
        zoomAndRotateSticker(handlingSticker, event);
    }

    public void zoomAndRotateSticker(@Nullable Sticker sticker, @NonNull MotionEvent event) {
        if (sticker != null) {
            float newDistance = StickerUtil.INSTANCE.calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
            float newRotation = StickerUtil.INSTANCE.calculateRotation(midPoint.x, midPoint.y, event.getX(), event.getY());

            moveMatrix.set(downMatrix);
            moveMatrix.postScale(newDistance / oldDistance, newDistance / oldDistance, midPoint.x,
                    midPoint.y);
            moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y);
            handlingSticker.setMatrix(moveMatrix);
        }
    }

    public void rotateSticker(@NonNull MotionEvent event) {
        if (handlingSticker == null) return;

        float newRotation = StickerUtil.INSTANCE.calculateRotation(midPoint.x, midPoint.y, event.getX(), event.getY());
        moveMatrix.set(downMatrix);
        moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y);
        handlingSticker.setMatrix(moveMatrix);
    }

    public void scale(@NonNull MotionEvent event, boolean isVertical) {
        if (handlingSticker != null) {
            //垂直拉伸
            if (isVertical) {
                float[] downMatrixValues = new float[9];
                downMatrix.getValues(downMatrixValues);

                // 从矩阵数值中提取旋转角度
                float scaleX = downMatrixValues[Matrix.MSCALE_X];
                float skewY = downMatrixValues[Matrix.MSKEW_Y];
                double rAngle = Math.toDegrees(Math.atan2(skewY, scaleX));//当前旋转角度

                float angle = StickerUtil.INSTANCE.calculateRotation(midPoint.x, midPoint.y, event.getX(), event.getY());//手指和图形中点连线角度
                float newXDistance = (float) (StickerUtil.INSTANCE.calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY()) * Math.abs(Math.sin(-(angle - rAngle) * (Math.PI / 180))));
                moveMatrix.set(downMatrix);
                moveMatrix.postRotate(-(float) rAngle, midPoint.x, midPoint.y);
                moveMatrix.postScale(1, newXDistance / oldDistance, midPoint.x, midPoint.y);
                moveMatrix.postRotate((float) rAngle, midPoint.x, midPoint.y);
                handlingSticker.setMatrix(moveMatrix);
            }
            //水平拉伸
            else {
                float[] downMatrixValues = new float[9];
                downMatrix.getValues(downMatrixValues);

                // 从矩阵数值中提取旋转角度
                float scaleX = downMatrixValues[Matrix.MSCALE_X];
                float skewY = downMatrixValues[Matrix.MSKEW_Y];
                double rAngle = Math.toDegrees(Math.atan2(skewY, scaleX));//当前旋转角度

                float angle = StickerUtil.INSTANCE.calculateRotation(midPoint.x, midPoint.y, event.getX(), event.getY());//手指和图形中点连线角度
                float newXDistance = (float) (StickerUtil.INSTANCE.calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY()) * Math.abs(Math.cos(-(angle - rAngle) * (Math.PI / 180))));
                moveMatrix.set(downMatrix);
                moveMatrix.postRotate(-(float) rAngle, midPoint.x, midPoint.y);
                moveMatrix.postScale(newXDistance / oldDistance, 1, midPoint.x, midPoint.y);
                moveMatrix.postRotate((float) rAngle, midPoint.x, midPoint.y);
                handlingSticker.setMatrix(moveMatrix);
            }

        }
    }


    /**
     * 在单个方向上进行缩放
     */
    public void scaleByDirection(@NonNull MotionEvent event, @Sticker.Position int direction) {

        //【】计算中心点
        //计算四个角落点的坐标
        float x1 = handingStickerDownBoundsPoints[0];
        float y1 = handingStickerDownBoundsPoints[1];
        float x2 = handingStickerDownBoundsPoints[2];
        float y2 = handingStickerDownBoundsPoints[3];
        float x3 = handingStickerDownBoundsPoints[4];
        float y3 = handingStickerDownBoundsPoints[5];
        float x4 = handingStickerDownBoundsPoints[6];
        float y4 = handingStickerDownBoundsPoints[7];


        int scaleCenterX = 0;
        int scaleCenterY = 0;
        switch (direction) {

            case Sticker.Position.BOTTOM:
                scaleCenterX = (int) ((x1 + x2) / 2f);
                scaleCenterY = (int) ((y1 + y2) / 2f);
                break;

            case Sticker.Position.TOP:
                scaleCenterX = (int) ((x3 + x4) / 2f);
                scaleCenterY = (int) ((y3 + y4) / 2f);
                break;

            case Sticker.Position.LEFT:
                scaleCenterX = (int) ((x2 + x4) / 2f);
                scaleCenterY = (int) ((y2 + y4) / 2f);
                break;

            case Sticker.Position.RIGHT:
                scaleCenterX = (int) ((x1 + x3) / 2f);
                scaleCenterY = (int) ((y1 + y3) / 2f);
                break;

            case Sticker.Position.CENTER:
                break;
        }


        //【】从矩阵中提取当前旋转角度
        double rotateAngle = MatrixUtilKt.getRotateAngle(downMatrix);

        //【】手指和旋转中心连线的角度
        float angle = StickerUtil.INSTANCE.calculateRotation(scaleCenterX, scaleCenterY, event.getX(), event.getY());

        //【】从角度计算手指拖动的分量
        double directionWeight = 1f;
        switch (direction) {
            case Sticker.Position.BOTTOM:
            case Sticker.Position.TOP:
                directionWeight = Math.abs(Math.sin(-(angle - rotateAngle) * (Math.PI / 180)));
                break;

            case Sticker.Position.LEFT:
            case Sticker.Position.RIGHT:
                directionWeight = Math.abs(Math.cos(-(angle - rotateAngle) * (Math.PI / 180)));
                break;

            case Sticker.Position.CENTER:
                break;
        }

        //【】计算当前单个方向上的缩放
        int downDistance = (int) (StickerUtil.INSTANCE.calculateDistance(scaleCenterX, scaleCenterY, downX, downY) * 1); // TODO: Vihanmy 2023-11-02 为什么这里使用分量后, 反而效果不好
        float nowDistance = (float) (StickerUtil.INSTANCE.calculateDistance(scaleCenterX, scaleCenterY, event.getX(), event.getY()) * directionWeight);
        float scale_X = nowDistance / downDistance;
        float scale_Y = 1f;

        switch (direction) {

            case Sticker.Position.BOTTOM:
            case Sticker.Position.TOP:
                scale_X = 1f;
                scale_Y = nowDistance / downDistance;
                break;

            case Sticker.Position.LEFT:
            case Sticker.Position.RIGHT:
                scale_X = nowDistance / downDistance;
                scale_Y = 1f;
                break;

            case Sticker.Position.CENTER:
                break;
        }

        //【】使用矩阵进行图形变化
        moveMatrix.set(downMatrix);
        moveMatrix.postRotate(-(float) rotateAngle, scaleCenterX, scaleCenterY);
        moveMatrix.postScale(scale_X, scale_Y, scaleCenterX, scaleCenterY);
        moveMatrix.postRotate((float) rotateAngle, scaleCenterX, scaleCenterY);
        handlingSticker.setMatrix(moveMatrix);

        String logStr =
                "direction:" + direction + "\n" +
                        "directionWeight:" + directionWeight + "\n" +
                        "scale_X:" + scale_X + " scale_Y:" + scale_Y + "\n" +
                        "scaleCenterX:" + scaleCenterX + " scaleCenterY:" + scaleCenterY + "\n" +
                        "rotateAngle:" + rotateAngle + " angle:" + angle + "\n" +
                        "downDistance:" + downDistance + " nowDistance:" + nowDistance + "\n" +
                        "";

        LOGG.INSTANCE.error(TAG, "scaleByDirection:" + logStr);
    }

    protected void constrainSticker(@NonNull Sticker sticker) {
        float moveX = 0;
        float moveY = 0;
        int width = getWidth();
        int height = getHeight();
        sticker.getMappedCenterPoint(currentCenterPoint, point, tmp);
        if (currentCenterPoint.x < 0) {
            moveX = -currentCenterPoint.x;
        }

        if (currentCenterPoint.x > width) {
            moveX = width - currentCenterPoint.x;
        }

        if (currentCenterPoint.y < 0) {
            moveY = -currentCenterPoint.y;
        }

        if (currentCenterPoint.y > height) {
            moveY = height - currentCenterPoint.y;
        }

        sticker.getMatrix().postTranslate(moveX, moveY);
    }

    @Nullable
    protected BitmapStickerIcon findCurrentIconTouched() {
        for (BitmapStickerIcon icon : icons) {
            float x = icon.getX() - downX;
            float y = icon.getY() - downY;
            float distance_pow_2 = x * x + y * y;
            if (distance_pow_2 <= Math.pow(icon.getIconRadius() + icon.getIconRadius(), 2)) {
                return icon;
            }
        }

        return null;
    }

    /**
     * find the touched Sticker
     **/
    @Nullable
    protected Sticker findHandlingSticker() {
        for (int i = stickers.size() - 1; i >= 0; i--) {
            if (stickers.get(i).contains(downX, downY)) {
                return stickers.get(i);
            }
        }
        return null;
    }

    @NonNull
    protected PointF calculateMidPoint() {
        if (handlingSticker == null) {
            midPoint.set(0, 0);
            return midPoint;
        }
        /*
         * midPoint  : midPoint 经过转换后在 StickerView 中的坐标
         * point     : midPoint 经过转换后在 StickerView 中的坐标
         * tmp       :
         */
        handlingSticker.getMappedCenterPoint(midPoint, point, tmp);
        return midPoint;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        for (int i = 0; i < stickers.size(); i++) {
            Sticker sticker = stickers.get(i);
            if (sticker != null) {
                transformSticker(sticker);
            }
        }
    }

    /**
     * Sticker's drawable will be too bigger or smaller
     * This method is to transform it to fit
     * step 1：let the center of the sticker image is coincident with the center of the View.
     * step 2：Calculate the zoom and zoom
     **/
    protected void transformSticker(@Nullable Sticker sticker) {
        if (sticker == null) {
            Log.e(TAG, "transformSticker: the bitmapSticker is null or the bitmapSticker bitmap is null");
            return;
        }

        sizeMatrix.reset();

        float width = getWidth();
        float height = getHeight();
        float stickerWidth = sticker.getWidth();
        float stickerHeight = sticker.getHeight();
        //step 1
        float offsetX = (width - stickerWidth) / 2;
        float offsetY = (height - stickerHeight) / 2;

        sizeMatrix.postTranslate(offsetX, offsetY);

        //step 2
        float scaleFactor;
        if (width < height) {
            scaleFactor = width / stickerWidth;
        } else {
            scaleFactor = height / stickerHeight;
        }

        sizeMatrix.postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f);

        sticker.getMatrix().reset();
        sticker.setMatrix(sizeMatrix);

        invalidate();
    }

    public void flipCurrentSticker(int direction) {
        flip(handlingSticker, direction);
    }

    public void flip(@Nullable Sticker sticker, @Flip int direction) {
        if (sticker != null) {
            sticker.getCenterPoint(midPoint);
            if ((direction & FLIP_HORIZONTALLY) > 0) {
                sticker.getMatrix().preScale(-1, 1, midPoint.x, midPoint.y);
                sticker.setFlippedHorizontally(!sticker.isFlippedHorizontally());
            }
            if ((direction & FLIP_VERTICALLY) > 0) {
                sticker.getMatrix().preScale(1, -1, midPoint.x, midPoint.y);
                sticker.setFlippedVertically(!sticker.isFlippedVertically());
            }

            if (onStickerOperationListener != null) {
                onStickerOperationListener.onStickerFlipped(sticker);
            }

            invalidate();
        }
    }

    public boolean replace(@Nullable Sticker sticker) {
        return replace(sticker, true);
    }

    public boolean replace(@Nullable Sticker sticker, boolean needStayState) {
        if (handlingSticker != null && sticker != null) {
            float width = getWidth();
            float height = getHeight();
            if (needStayState) {
                sticker.setMatrix(handlingSticker.getMatrix());
                sticker.setFlippedVertically(handlingSticker.isFlippedVertically());
                sticker.setFlippedHorizontally(handlingSticker.isFlippedHorizontally());
            } else {
                handlingSticker.getMatrix().reset();
                // reset scale, angle, and put it in center
                float offsetX = (width - handlingSticker.getWidth()) / 2f;
                float offsetY = (height - handlingSticker.getHeight()) / 2f;
                sticker.getMatrix().postTranslate(offsetX, offsetY);

                float scaleFactor;
                if (width < height) {
                    scaleFactor = width / handlingSticker.getDrawable().getIntrinsicWidth();
                } else {
                    scaleFactor = height / handlingSticker.getDrawable().getIntrinsicHeight();
                }
                sticker.getMatrix().postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f);
            }
            int index = stickers.indexOf(handlingSticker);
            stickers.set(index, sticker);
            handlingSticker = sticker;

            invalidate();
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(@Nullable Sticker sticker) {
        if (stickers.contains(sticker)) {
            stickers.remove(sticker);
            if (onStickerOperationListener != null) {
                onStickerOperationListener.onStickerDeleted(sticker);
            }
            if (handlingSticker == sticker) {
                handlingSticker = null;
            }
            invalidate();

            return true;
        } else {
            Log.d(TAG, "remove: the sticker is not in this StickerView");

            return false;
        }
    }

    public boolean removeCurrentSticker() {
        return remove(handlingSticker);
    }

    public void removeAllStickers() {
        stickers.clear();
        if (handlingSticker != null) {
            handlingSticker.release();
            handlingSticker = null;
        }
        invalidate();
    }

    @NonNull
    public StickerView addSticker(@NonNull Sticker sticker) {
        return addSticker(sticker, Sticker.Position.CENTER);
    }

    public StickerView addSticker(@NonNull final Sticker sticker,
                                  final @Sticker.Position int position) {
        if (ViewCompat.isLaidOut(this)) {
            addStickerImmediately(sticker, position);
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    addStickerImmediately(sticker, position);
                }
            });
        }
        return this;
    }

    protected void addStickerImmediately(@NonNull Sticker sticker, @Sticker.Position int position) {
        setStickerPosition(sticker, position);


        float scaleFactor, widthScaleFactor, heightScaleFactor;

        widthScaleFactor = (float) getWidth() / sticker.getDrawable().getIntrinsicWidth();
        heightScaleFactor = (float) getHeight() / sticker.getDrawable().getIntrinsicHeight();
        scaleFactor = widthScaleFactor > heightScaleFactor ? heightScaleFactor : widthScaleFactor;

        sticker.getMatrix()
                .postScale(scaleFactor / 2, scaleFactor / 2, getWidth() / 2, getHeight() / 2);

        handlingSticker = sticker;
        stickers.add(sticker);
        if (onStickerOperationListener != null) {
            onStickerOperationListener.onStickerAdded(sticker);
        }
        invalidate();
    }

    protected void setStickerPosition(@NonNull Sticker sticker, @Sticker.Position int position) {
        float width = getWidth();
        float height = getHeight();
        float offsetX = width - sticker.getWidth();
        float offsetY = height - sticker.getHeight();
        if ((position & Sticker.Position.TOP) > 0) {
            offsetY /= 4f;
        } else if ((position & Sticker.Position.BOTTOM) > 0) {
            offsetY *= 3f / 4f;
        } else {
            offsetY /= 2f;
        }
        if ((position & Sticker.Position.LEFT) > 0) {
            offsetX /= 4f;
        } else if ((position & Sticker.Position.RIGHT) > 0) {
            offsetX *= 3f / 4f;
        } else {
            offsetX /= 2f;
        }
        sticker.getMatrix().postTranslate(offsetX, offsetY);
    }


    public void save(@NonNull File file) {
        try {
            StickerUtils.saveImageToGallery(file, createBitmap());
            StickerUtils.notifySystemGallery(getContext(), file);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            //
        }
    }

    @NonNull
    public Bitmap createBitmap() throws OutOfMemoryError {
        handlingSticker = null;
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        this.draw(canvas);
        return bitmap;
    }


    @NonNull
    public StickerView setLocked(boolean locked) {
        this.locked = locked;
        invalidate();
        return this;
    }

    @NonNull
    public StickerView setMinClickDelayTime(int minClickDelayTime) {
        this.minClickDelayTime = minClickDelayTime;
        return this;
    }

    @NonNull
    public StickerView setConstrained(boolean constrained) {
        this.constrained = constrained;
        postInvalidate();
        return this;
    }

    @NonNull
    public StickerView setOnStickerOperationListener(
            @Nullable OnStickerOperationListener onStickerOperationListener) {
        this.onStickerOperationListener = onStickerOperationListener;
        return this;
    }

    public void setIcons(@NonNull List<BitmapStickerIcon> icons) {
        this.icons.clear();
        this.icons.addAll(icons);
        invalidate();
    }

    ///////////////////////////////////////////////////////// getters
    public int getStickerCount() {
        return stickers.size();
    }

    @Nullable
    public OnStickerOperationListener getOnStickerOperationListener() {
        return onStickerOperationListener;
    }

    @Nullable
    public Sticker getCurrentSticker() {
        return handlingSticker;
    }

    @NonNull
    public List<BitmapStickerIcon> getIcons() {
        return icons;
    }

    public int getMinClickDelayTime() {
        return minClickDelayTime;
    }

    public boolean isConstrained() {
        return constrained;
    }

    public boolean isNoneSticker() {
        return getStickerCount() == 0;
    }

    public boolean isLocked() {
        return locked;
    }

    @NonNull
    public float[] getStickerPoints(@Nullable Sticker sticker) {
        float[] points = new float[8];
        getStickerPoints(sticker, points);
        return points;
    }

    public void getStickerPoints(@Nullable Sticker sticker, @NonNull float[] dst) {
        if (sticker == null) {
            Arrays.fill(dst, 0);
            return;
        }
        sticker.getBoundPoints(bounds);
        sticker.getMappedPoints(dst, bounds);
    }

    ///////////////////////////////////////////////////////// event listener
    public interface OnStickerOperationListener {
        void onStickerAdded(@NonNull Sticker sticker);

        void onStickerClicked(@NonNull Sticker sticker);

        void onStickerDeleted(@NonNull Sticker sticker);

        void onStickerDragFinished(@NonNull Sticker sticker);

        void onStickerTouchedDown(@NonNull Sticker sticker);

        void onStickerZoomFinished(@NonNull Sticker sticker);

        void onStickerFlipped(@NonNull Sticker sticker);

        void onStickerDoubleTapped(@NonNull Sticker sticker);
    }
}
