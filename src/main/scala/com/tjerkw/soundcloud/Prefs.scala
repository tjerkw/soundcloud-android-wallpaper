package com.tjerkw.soundcloud

import android.content.SharedPreferences

class Prefs(prefs:SharedPreferences) {

  def getSoundCloudUrl = "http://soundcloud.com/get/2"

  def username = "tjerkw"
  def password = "aabbccdd"

  def clientId = "874a6ca0b5a57c77f362e1788e6b331b"
  def clientSecret = "eb5dd6fad39806b3e334fccfc3956bed"

  def requestUrl = "/users/" + username + "/favorites"

  def timeBetweenWaveforms = 1000
}

