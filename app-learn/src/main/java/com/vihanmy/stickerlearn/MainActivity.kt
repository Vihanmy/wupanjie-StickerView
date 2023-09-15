package com.vihanmy.stickerlearn

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.sticker.SampleSticker

class MainActivity : AppCompatActivity() {

    private val stickerView: StickerView by lazy { (findViewById(R.id.sticker_view) as StickerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stickerView.addSticker(SampleSticker(this))
        stickerView.addSticker(SampleSticker(this))
    }
}