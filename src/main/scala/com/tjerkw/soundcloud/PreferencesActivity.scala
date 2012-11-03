package com.tjerkw.soundcloud

import android.os.Bundle
import android.preference.PreferenceActivity

class PreferencesActivity extends PreferenceActivity() {

  override def onCreate(state:Bundle) {
    super.onCreate(state)
    getPreferenceManager.setSharedPreferencesName(SoundCloudWallpaper.SHARED_PREFS_NAME)
    addPreferencesFromResource(R.xml.prefs)
  }
}
