package com.xiaopo.flying.stickerview

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Layout
import android.util.Log
import android.view.View
import android.widget.Toast
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.StickerView.OnStickerOperationListener
import com.xiaopo.flying.sticker.iconevent.DeleteIconEvent
import com.xiaopo.flying.sticker.iconevent.FlipHorizontallyEvent
import com.xiaopo.flying.sticker.iconevent.FlipVerticallyEvent
import com.xiaopo.flying.sticker.iconevent.HorizontalScaleEvent
import com.xiaopo.flying.sticker.iconevent.RotateEvent
import com.xiaopo.flying.sticker.iconevent.VerticalScaleEvent
import com.xiaopo.flying.sticker.iconevent.ZoomIconEvent
import com.xiaopo.flying.sticker.iconevent.singledirectionscale.BottomScaleEvent
import com.xiaopo.flying.sticker.iconevent.singledirectionscale.LeftScaleEvent
import com.xiaopo.flying.sticker.iconevent.singledirectionscale.RightScaleEvent
import com.xiaopo.flying.sticker.iconevent.singledirectionscale.TopScaleEvent
import com.xiaopo.flying.sticker.sticker.DrawableSticker
import com.xiaopo.flying.sticker.sticker.TextSticker
import com.xiaopo.flying.sticker.sticker.protocol.Sticker
import com.xiaopo.flying.sticker.stickericon.BitmapStickerIcon
import com.xiaopo.flying.stickerview.util.FileUtil
import com.xiaopo.flying.sticker.R  as Rst

class MainActivity : AppCompatActivity() {
    private var stickerView: StickerView? = null
    private var sticker: TextSticker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stickerView = findViewById(R.id.sticker_view) as StickerView
        val toolbar = findViewById(R.id.toolbar) as Toolbar

        val deleteIcon = BitmapStickerIcon(getDrawable(Rst.drawable.sticker_ic_close_white_18dp), BitmapStickerIcon.LEFT_TOP)
        deleteIcon.iconEvent = DeleteIconEvent()

        val zoomIcon = BitmapStickerIcon(getDrawable(Rst.drawable.sticker_ic_scale_white_18dp), BitmapStickerIcon.RIGHT_BOTTOM)
        zoomIcon.iconEvent = ZoomIconEvent()

        val flipIcon = BitmapStickerIcon(getDrawable(Rst.drawable.sticker_ic_flip_white_18dp), BitmapStickerIcon.RIGHT_TOP)
        flipIcon.iconEvent = FlipHorizontallyEvent()

        val heartIcon = BitmapStickerIcon(getDrawable(R.drawable.ic_favorite_white_24dp), BitmapStickerIcon.LEFT_BOTTOM)
        heartIcon.iconEvent = HelloIconEvent()

        /////////////////////////////////////////////////////////
        val verticalFlip = BitmapStickerIcon(getDrawable(Rst.drawable.sticker_ic_flip_vertical_white_18dp), BitmapStickerIcon.CENTER_BOTTOM)
        verticalFlip.iconEvent = FlipVerticallyEvent()

        val horizontalScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_scale_horizontal), BitmapStickerIcon.CENTER_RIGHT)
        horizontalScale.iconEvent = HorizontalScaleEvent()

        val verticalScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_scale_vertical), BitmapStickerIcon.CENTER_TOP)
        verticalScale.iconEvent = VerticalScaleEvent()

        val rotate = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_rotate), BitmapStickerIcon.CENTER_LEFT)
        rotate.iconEvent = RotateEvent()

        /////////////////////////////////////////////////////////
        val topScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_top_scale), BitmapStickerIcon.CENTER_TOP).apply {
            iconEvent = TopScaleEvent()
        }
        val leftScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_left_scale), BitmapStickerIcon.CENTER_LEFT).apply {
            iconEvent = LeftScaleEvent()
        }
        val rightScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_right_scale), BitmapStickerIcon.CENTER_RIGHT).apply {
            iconEvent = RightScaleEvent()
        }
        val bottomScale = BitmapStickerIcon(getDrawable(Rst.drawable.st_ic_bottom_scale), BitmapStickerIcon.CENTER_BOTTOM).apply {
            iconEvent = BottomScaleEvent()
        }

        stickerView!!.icons = listOf(
            deleteIcon,
            // flipIcon,
            // heartIcon,
            // verticalFlip,

            //【】rotate
            // rotate,

            //【】
            zoomIcon,

            //【】 horizontal and vertical scale
            // horizontalScale,
            // verticalScale,

            //【】 single direction scale
            topScale,
            leftScale,
            rightScale,
            bottomScale,

            )

        // default icon layout
        // stickerView.configDefaultIcons();
        stickerView!!.setBackgroundColor(Color.WHITE)
        stickerView!!.setLocked(false)
        stickerView!!.setConstrained(true)
        sticker = TextSticker(this)
        sticker!!.setDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.drawable.sticker_transparent_background
            )
        )
        sticker!!.setText("Hello, world!")
        sticker!!.setTextColor(Color.BLACK)
        sticker!!.setTextAlign(Layout.Alignment.ALIGN_CENTER)
        sticker!!.resizeText()
        stickerView!!.setOnStickerOperationListener(object : OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                Log.d(TAG, "onStickerAdded")
            }

            override fun onStickerClicked(sticker: Sticker) {
                // stickerView.removeAllSticker();
                if (sticker is TextSticker) {
                    sticker.setTextColor(Color.RED)
                    stickerView!!.replace(sticker)
                    stickerView!!.invalidate()
                }
                Log.d(TAG, "onStickerClicked")
            }

            override fun onStickerDeleted(sticker: Sticker) {
                Log.d(TAG, "onStickerDeleted")
            }

            override fun onStickerDragFinished(sticker: Sticker) {
                Log.d(TAG, "onStickerDragFinished")
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                Log.d(TAG, "onStickerTouchedDown")
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                Log.d(TAG, "onStickerZoomFinished")
            }

            override fun onStickerFlipped(sticker: Sticker) {
                Log.d(TAG, "onStickerFlipped")
            }

            override fun onStickerDoubleTapped(sticker: Sticker) {
                Log.d(TAG, "onDoubleTapped: double tap will be with two click")
            }
        })
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name)
            toolbar.inflateMenu(R.menu.menu_save)
            toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.item_save) {
                    val file = FileUtil.getNewFile(this@MainActivity, "Sticker")
                    if (file != null) {
                        stickerView!!.save(file)
                        Toast.makeText(
                            this@MainActivity, "saved in " + file.absolutePath,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this@MainActivity, "the file is null", Toast.LENGTH_SHORT).show()
                    }
                }
                //                    stickerView.replace(new DrawableSticker(
                //                            ContextCompat.getDrawable(MainActivity.this, R.drawable.haizewang_90)
                //                    ));
                false
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERM_RQST_CODE)
        } else {
            loadSticker()
        }
    }

    private fun loadSticker() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.haizewang_215)
        val drawable1 = ContextCompat.getDrawable(this, R.drawable.haizewang_23)
        stickerView!!.addSticker(DrawableSticker(drawable))
        stickerView!!.addSticker(DrawableSticker(drawable1), Sticker.Position.BOTTOM or Sticker.Position.RIGHT)
        val bubble = ContextCompat.getDrawable(this, R.drawable.bubble)
        stickerView!!.addSticker(
            TextSticker(applicationContext)
                .setDrawable(bubble)
                .setText("Sticker")
                .setMaxTextSize(14f)
                .resizeText(), Sticker.Position.TOP
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_RQST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSticker()
        }
    }

    fun testReplace(view: View?) {
        if (stickerView!!.replace(sticker)) {
            Toast.makeText(this@MainActivity, "Replace Sticker successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Replace Sticker failed!", Toast.LENGTH_SHORT).show()
        }
    }

    fun testLock(view: View?) {
        stickerView!!.setLocked(!stickerView!!.isLocked)
    }

    fun testRemove(view: View?) {
        if (stickerView!!.removeCurrentSticker()) {
            Toast.makeText(this@MainActivity, "Remove current Sticker successfully!", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(this@MainActivity, "Remove current Sticker failed!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun testRemoveAll(view: View?) {
        stickerView!!.removeAllStickers()
    }

    fun reset(view: View?) {
        stickerView!!.removeAllStickers()
        loadSticker()
    }

    fun testAdd(view: View?) {
        val sticker = TextSticker(this)
        sticker.setText("Hello, world!")
        sticker.setTextColor(Color.BLUE)
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
        sticker.resizeText()
        stickerView!!.addSticker(sticker)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val PERM_RQST_CODE = 110
    }

    /////////////////////////////////////////////////////////
    private fun Activity.getDrawable(@DrawableRes res: Int) = ContextCompat.getDrawable(this, res)
}