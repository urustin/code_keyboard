package com.urustin.codekeyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.urustin.codekeyboard.theme.IOnFocusListenable

// 설정 화면을 담는 메인 액티비티(SettingsFragment 호스팅)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_activity_main)
        val extras = intent.extras
        val frag = SettingsFragment()
        frag.arguments = extras
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, frag)
            .commit()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.settings_container)
        if (currentFragment is IOnFocusListenable) {
            currentFragment.onWindowFocusChanged(hasFocus)
        }
    }
}
