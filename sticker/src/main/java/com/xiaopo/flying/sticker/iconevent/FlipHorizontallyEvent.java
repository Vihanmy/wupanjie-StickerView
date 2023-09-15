package com.xiaopo.flying.sticker.iconevent;

import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.iconevent.protocol.AbstractFlipEvent;

/**
 * @author wupanjie
 */

public class FlipHorizontallyEvent extends AbstractFlipEvent {

  @Override @StickerView.Flip
  protected int getFlipDirection() {
    return StickerView.FLIP_HORIZONTALLY;
  }
}
