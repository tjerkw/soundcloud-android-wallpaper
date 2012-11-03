package com.tjerkw.soundcloud

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.Handler
import android.graphics.{BitmapFactory, Bitmap, Canvas, Paint}
import actors.Actor._
import java.net.{HttpURLConnection, URL}
import actors.Actor

object SoundCloudWallpaper {
  def SHARED_PREFS_NAME = "SoundCloudPrefs"
}

class SoundCloudWallpaper extends WallpaperService {
  var engine:SoundCloudWallpaperEngine = null
  var throttler:Throttler = null

  override def onCreate {
    // load the preferences
    val prefs = new Prefs(
      this.getSharedPreferences(
        SoundCloudWallpaper.SHARED_PREFS_NAME, 0
      )
    )
    engine = new SoundCloudWallpaperEngine
    // init api and actors
    val api = new SoundCloudApi(prefs)

    // receives bitmaps from actors
    val receiver = actor {
      receive {
        case bitmap:Bitmap => {
          Logger i "Retrieved waveform"
          if(engine!=null) engine.updateBitmap(bitmap)
        }
      }
    }
    val bitmapProducer = new BitmapProducer(receiver)
    // we only want to show an waveform every x seconds
    throttler = new Throttler(
      bitmapProducer,
      prefs.timeBetweenWaveforms
    )
    val urlProducer = new WaveformUrlProducer(
      throttler, api
    )
    bitmapProducer.start()
    urlProducer.start()
    receiver.start()
  }

  override def onDestroy {
    // stop receiving and decoding bitmaps
    if (throttler!=null) throttler ! "stop"
  }

  override def onCreateEngine():WallpaperService#Engine = engine

  class SoundCloudWallpaperEngine extends super.Engine() {
    val handler = new Handler()
    var width = 0
    var height = 0
    val paint = new Paint()
    val runnable = new Runnable() {
      def run = draw
    }
    var bitmap:Bitmap = null

    def updateBitmap(bm:Bitmap) {
      bitmap = bm
      handler post runnable
    }


    override def onSurfaceCreated(holder:SurfaceHolder) {
      paint setColor 0xFFFFFF
      super.onSurfaceCreated(holder)
      handler post runnable
    }

    override def onSurfaceDestroyed(holder:SurfaceHolder) {
      super.onSurfaceDestroyed(holder)
      handler removeCallbacks runnable
    }

    override def onVisibilityChanged(visible:Boolean) {
      super.onVisibilityChanged(visible)
      if (visible) {
        handler post runnable
      } else {
        handler removeCallbacks runnable
      }
    }

    override def onSurfaceChanged(holder:SurfaceHolder, format:Int, width:Int, height:Int) {
      this.width = width
      this.height = height
      super.onSurfaceChanged(holder, format, width, height)
    }

    def draw() {
      val holder = super.getSurfaceHolder
      var canvas:Canvas = null
      try {
        canvas = holder.lockCanvas()
        if (canvas!=null) {
          if (bitmap != null) {
            canvas.rotate(90f)
            canvas.drawBitmap(bitmap, 0, 0, paint)
          } else {
            canvas.drawText("Loading...", 20, 20, paint)
          }
        }
      } finally {
        if (canvas != null) holder unlockCanvasAndPost canvas
      }
    }
  }

}

/**
 * Makes a call to the soundcloud api and produces
 * waveforum url's. These are sent to a receiving actor
 */
class WaveformUrlProducer(receiver:Actor, api:SoundCloudApi) extends Actor {
  def act() {
    for (waveformUrl <- api.get) {
      Logger i "Got waveformUrl " + waveformUrl
      receiver ! waveformUrl
    }
  }
}

/**
 * Receives URL's, retrieves them and decodes them into
 * Bitmap objects.
 *
 * @param receiver
 */
class BitmapProducer(receiver:Actor) extends Actor {
  def act() {
    react {
      case waveformUrl:URL => {
        receiver ! produce(waveformUrl)
      }
    }
  }

  def produce(waveformUrl:URL):Bitmap = {
    Logger i "Retrieving stream for bitmap " + waveformUrl
    val conn:HttpURLConnection = waveformUrl.openConnection.asInstanceOf[HttpURLConnection]
    conn setDoInput true
    conn.connect
    val input = conn.getInputStream
    Logger i "decoding stream"
    BitmapFactory decodeStream input
  }
}

class Throttler(receiver:Actor, timeout:Int) extends Actor {
  def act() {
    react {
      case "stop" => exit
      case msg => {
        receiver ! msg
        // note that using wait in a actor is not a good idea in general
        Thread.currentThread().wait(timeout)

      }
    }
  }
}