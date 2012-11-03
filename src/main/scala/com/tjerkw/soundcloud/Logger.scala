package com.tjerkw.soundcloud

import android.util.Log

/**
 * Make logging easier,
 * always use the same tag
 */
object Logger {
  val tag = "SoundCloudWallpaper"

  def e(msg:String) = Log.e(tag, msg)
  def d(msg:String) = Log.d(tag, msg)
  def i(msg:String) = Log.i(tag, msg)
}