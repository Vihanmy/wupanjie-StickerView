package com.xiaopo.flying.sticker.iconevent;

import android.view.MotionEvent;

import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.iconevent.protocol.StickerIconEvent;

/**
 * @author wupanjie
 */

public class DeleteIconEvent implements StickerIconEvent {
  @Override public void onActionDown(StickerView stickerView, MotionEvent event) {

  }

  @Override public void onActionMove(StickerView stickerView, MotionEvent event) {

  }

  @Override public void onActionUp(StickerView stickerView, MotionEvent event) {
    stickerView.removeCurrentSticker();
  }
}
