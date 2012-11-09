package com.tjerkw.soundcloud

import com.soundcloud.api.{Http, Request, Env, ApiWrapper}
import org.apache.http.HttpStatus
import org.json.JSONArray
import java.net.URL
import java.io.IOException

/**
 * Object that the api returns
 * @param waveform url to the waveform
 * @param title title of the track
 * @param uri uri to the track
 */
case class Track(waveform:URL, title:String, uri:String)


/**
 * Class that talks to the soundcloud api.
 * It logs in, gets the url and parses the json,
 * and returns a sequence of waveform urls
 *
 * @param prefs preferences which contains credentials for logging into soundcloud
 */
class SoundCloudApi(prefs:Prefs) {

  private lazy val api = new ApiWrapper(
    prefs.clientId, prefs.clientSecret,
    null, null, Env.LIVE
  )

  def get:Seq[Track] = {
    Logger i "Logging to soundcloud api"
    api.login(prefs.username, prefs.password)

    Logger i "Retrieving url " + prefs.requestUrl
    val resp = api get Request.to(prefs.requestUrl)

    if (resp.getStatusLine.getStatusCode == HttpStatus.SC_OK) {

      //parse json
      val json:JSONArray = new JSONArray(Http.getString(resp))

      for(i <- 0 to json.length()-1) yield {
        val obj = json getJSONObject i

        // json to object
        new Track(
          new URL(obj getString "waveform_url"),
          obj getString "title",
          obj getString "uri"
        )
      }
    } else {
      Logger e "Got non 200 status code: " + resp.getStatusLine
      throw new IOException("Cloud not get api result " + resp.getStatusLine)
    }
  }

  /**
   * Returns an infinite lazy list of waveform url.
   * It only calls get once, and infinitly loops
   * that result
   */
  def infiniteGet:Seq[Track] = Stream.continually(get.toStream).flatten
}