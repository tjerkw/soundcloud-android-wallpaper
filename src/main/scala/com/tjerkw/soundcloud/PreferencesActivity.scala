package com.tjerkw.soundcloud

import android.os.Bundle
import android.preference.PreferenceActivity

class PreferencesActivity extends PreferenceActivity() {

  override def onCreate(state:Bundle) {
    super.onCreate(state)
    addPreferencesFromResource(R.xml.prefs)
  }
}
