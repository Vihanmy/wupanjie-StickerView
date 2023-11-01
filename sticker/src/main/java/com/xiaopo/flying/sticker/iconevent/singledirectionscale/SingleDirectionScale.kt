package com.xiaopo.flying.sticker.iconevent.singledirectionscale

import android.view.MotionEvent
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.iconevent.protocol.StickerIconEvent

abstract class SingleDirectionScale : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionUp(stickerView: StickerView?, event: MotionEvent?) {}
}