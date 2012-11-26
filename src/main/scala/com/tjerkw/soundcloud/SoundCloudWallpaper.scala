package com.tjerkw.soundcloud

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.{Bundle, Handler}
import android.graphics._
import android.graphics.BlurMaskFilter.Blur
import java.util.Random
import android.preference.PreferenceManager
import android.net.Uri
import android.content.Intent

class SoundCloudWallpaper extends WallpaperService {

  override def onCreateEngine:WallpaperService#Engine
    = new SoundCloudWallpaperEngine

  class SoundCloudWallpaperEngine extends super.Engine() {
    val prefs = new Prefs(
      PreferenceManager.getDefaultSharedPreferences(SoundCloudWallpaper.this)
    )
    // generates the waveforms
    var generator:WaveformGenerator = null
    // the current waveform
    var waveform:Waveform = null
    // sizeof the canvas
    var width = 0
    var height = 0
    // the swiping offset
    var xOffset = 0f
    // whether the surface is available and visible, so we can paint
    var canPaint = false

    // init paint
    val paint = new Paint()
    // enable blurring
    if (prefs.enableBlur) paint.setMaskFilter(
      new BlurMaskFilter(13, Blur.NORMAL)
    )
    paint.setTextSize(prefs.textSize)
    paint.setStyle(Paint.Style.FILL)

    // default bg color
    var bgColor = Color.WHITE
    // java requires a runnable object
    val runnable = new Runnable {
      def run = lockAndDraw
    }
    // this handler will handle the draw runnable
    val handler = new Handler
    // to generate random values for our color
    lazy val random = new Random()
    // we support touch events
    this setTouchEventsEnabled true

    /**
     * Generates a nice bright random color
     */
    def randomColor = {
      Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

    /**
     * Called when a new waveform has been generated by the actors
     */
    def updateWaveform(result:Waveform) {

      // rotate colors
      if(prefs.enableColorRotate) {
        bgColor = randomColor
      }
      // lock, because we dont want to draw a recycled bitmap
      synchronized {
        if (waveform != null) waveform.bitmap recycle()
        Logger i "Engine received waveform"
        this.waveform = result
      }
      handler post runnable
    }

    override def onCommand(action:String, x:Int, y:Int, z:Int,
      bundle:Bundle, resultRequested:Boolean):Bundle =
    {
      if ("android.wallpaper.tap" == action) {
        val i = new Intent(Intent.ACTION_VIEW)
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i setData (Uri parse waveform.track.uri )
        startActivity(i)
      }
      super.onCommand(action, x, y, z, bundle, resultRequested)
    }

    override def onDestroy() {
      super.onDestroy()
      handler removeCallbacks runnable
    }


    override def onSurfaceCreated(holder:SurfaceHolder) {
      super.onSurfaceCreated(holder)
      generator = new WaveformGenerator(
        prefs, updateWaveform
      )
      handler post runnable
    }

    override def onSurfaceDestroyed(holder:SurfaceHolder) {
      canPaint = false
      generator.stop
      handler removeCallbacks runnable
      super.onSurfaceDestroyed(holder)
    }

    override def onVisibilityChanged(visible:Boolean) {
      canPaint = visible
      super.onVisibilityChanged(visible)
      if (visible) {
        generator.start
        handler post runnable
      } else {
        generator.stop
        handler removeCallbacks runnable
      }
    }

    override def onSurfaceChanged(holder:SurfaceHolder, format:Int, width:Int, height:Int) {
      val prevWidth = this.width
      this.width = width
      this.height = height
      super.onSurfaceChanged(holder, format, width, height)
    }

    /**
     * The user is swiping the homescreen, also translate the waveform
     */
    override def onOffsetsChanged(xOffset:Float, yOffset:Float,  xOffsetStep:Float, yOffsetStep:Float, xPixelOffset:Int, yPixelOffset:Int) {
      this.xOffset = xOffset
      if (this.isVisible) handler post runnable
    }

    private def lockAndDraw() {
      if (!canPaint) return
      val holder = super.getSurfaceHolder
      var canvas:Canvas = null
      try {
        canvas = holder.lockCanvas
        if (canvas!=null) {
          draw(canvas)
        }
      } finally {
        if (canvas != null) holder unlockCanvasAndPost canvas
      }
    }

    private def draw(canvas:Canvas) {
      Logger i "Engine.draw"
      // clear canvas since png are transparent!
      canvas drawColor bgColor
      if (waveform != null) synchronized {

        val bitmap = waveform.bitmap
        val scale:Float = height.asInstanceOf[Float] / bitmap.getHeight
        val newWidth = (bitmap.getWidth * scale).asInstanceOf[Int]

        val x = -xOffset*(newWidth - width)
        canvas.save
        // slide movement
        canvas.translate(x, 0f)
        // fill the whole screen
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, newWidth, height), paint)
        canvas.restore

        // draw the track title
        canvas.drawText(waveform.track.title, width/7 - xOffset*width/3, height/2, paint)

      } else {
        canvas.drawText("Loading...", width/2, height/2, paint)
      }
    }
  }
}