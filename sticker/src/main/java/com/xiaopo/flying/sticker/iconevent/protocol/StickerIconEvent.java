package com.xiaopo.flying.sticker.iconevent.protocol;

import android.view.MotionEvent;

import com.xiaopo.flying.sticker.StickerView;

/**
 * @author wupanjie
 */

public interface StickerIconEvent {
  void onActionDown(StickerView stickerView, MotionEvent event);

  void onActionMove(StickerView stickerView, MotionEvent event);

  void onActionUp(StickerView stickerView, MotionEvent event);
}
