package com.tjerkw.soundcloud

import actors.{TIMEOUT, Actor}
import java.net.{HttpURLConnection, URL}
import android.graphics.{BitmapFactory, Bitmap}
import actors.Actor._

case class Waveform(track:Track, bitmap:Bitmap)

/**
 * A class that generates soundcloud results in a timeout interval.
 * The result (error or track) are passed to the listener
 * It uses the preferences as settings to connect to soundcloud,
 * and for timeout settings.
 *
 * @param prefs the preferences
 * @param listener the callback
 */
class WaveformGenerator(prefs:Prefs, listener: Waveform => Unit) {

  private var stopped = false

  // actor that receives a bitmap and passes it to the listener
  private val receiver = actor {
    loop {
      react {
        case result:Waveform => {
          // handle it
          listener(result)
          requestNext
        }
      }
    }
  }

  // generate urls from api
  private val producer = new WaveformUrlProducer(
    // used to make an api call
    new SoundCloudApi(prefs),
    // generate Bitmap from url
    new ResultProducer(receiver),
    // the timeout interval
    prefs.timeBetweenWaveforms
  )

  def start {
    // note that the other actors where started by default
    stopped = false
    requestNext
  }

  def stop = stopped = true

  private def requestNext {
    Logger i "Requesting next"
    if(!stopped) producer ! NextWaveform
  }
}

// signal to generate a waveform
object NextWaveform

/**
 * Makes a call to the soundcloud api and produces
 * waveforum url's. These are sent to a receiving actor.
 * It will keep generating
 */
class WaveformUrlProducer( api:SoundCloudApi, receiver:Actor, timeout:Int) extends Actor {
  start // start by default

  def act {
    for (track <- api.infiniteGet) {
      Logger i "Got waveformUrl " + track.waveform
      receive {
        case NextWaveform => {
          Logger i "WaveUrlProducer sending waveformUrl " + track.waveform
          receiver ! track
        }
      }
      // now at least wait a bit before we even accepting
      // to generate a next waveform
      receiveWithin(timeout) {
        case TIMEOUT =>
      }
    }
  }
}

/**
 * Receives URL's, retrieves them and decodes them into
 * Bitmap objects.
 *
 * @param receiver it will sent the generated bitmaps to this receiver
 */
class ResultProducer(receiver:Actor) extends Actor {
  start // start by default

  def act() {
    loop {
      react {
        case track:Track =>
          receiver ! new Waveform(track, produce(track.waveform))
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
    val options = new BitmapFactory.Options
    options.inSampleSize = 2
    BitmapFactory.decodeStream(input, null, options)
  }
}
