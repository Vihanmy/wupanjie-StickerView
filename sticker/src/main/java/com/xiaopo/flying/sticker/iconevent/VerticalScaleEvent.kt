package com.xiaopo.flying.sticker.iconevent

import android.view.MotionEvent
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.iconevent.protocol.StickerIconEvent

class VerticalScaleEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {

    }

    override fun onActionMove(stickerView: StickerView?, event: MotionEvent?) {
        stickerView?.scale(event!!, true)
    }

    override fun onActionUp(stickerView: StickerView?, event: MotionEvent?) {
    }
}