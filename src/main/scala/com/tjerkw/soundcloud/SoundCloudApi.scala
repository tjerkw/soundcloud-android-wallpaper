package com.tjerkw.soundcloud

import com.soundcloud.api.{Http, Request, Env, ApiWrapper}
import org.apache.http.HttpStatus
import org.json.JSONArray
import java.net.URL

/**
 * Class that talks to the soundcloud api.
 * It logs in, gets the url and parses the json,
 * and returns a sequence of waveform urls
 *
 * @param prefs
 */
class SoundCloudApi(prefs:Prefs) {
  val api = new ApiWrapper(
    prefs.clientId, prefs.clientSecret,
    null, null, Env.LIVE
  )

  def get:Seq[URL] = {
    Logger i "Loggi to soundcloud api"
    api.login(prefs.username, prefs.password)

    Logger i "Retrieving url " + prefs.requestUrl
    val resp = api get Request.to(prefs.requestUrl)

    if (resp.getStatusLine.getStatusCode == HttpStatus.SC_OK) {
      val json:JSONArray = new JSONArray(Http.getString(resp))
      for(i <- 0 to json.length()-1) yield {
        new URL(json.getJSONObject(i).getString("waveform_url"))
      }
    } else {
      Logger e "Got non 200 status code: " + resp.getStatusLine
      Seq.empty
    }
  }
}