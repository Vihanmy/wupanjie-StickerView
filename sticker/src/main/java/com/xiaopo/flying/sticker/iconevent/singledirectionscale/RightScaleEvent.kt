package com.xiaopo.flying.sticker.iconevent.singledirectionscale

import android.view.MotionEvent
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.sticker.protocol.Sticker

class RightScaleEvent : SingleDirectionScale() {
    override fun onActionMove(stickerView: StickerView?, event: MotionEvent?) {
        stickerView!!.scaleByDirection(event!!, Sticker.Position.RIGHT)
    }
}