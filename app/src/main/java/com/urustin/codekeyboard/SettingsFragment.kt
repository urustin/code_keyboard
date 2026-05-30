package com.urustin.codekeyboard

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.urustin.codekeyboard.theme.IOnFocusListenable
import com.urustin.codekeyboard.theme.ThemeDefinitions
import com.urustin.codekeyboard.theme.ThemeInfo
import com.github.evilbunny2008.androidmaterialcolorpickerdialog.ColorPicker

// 앱 설정 화면. 테마/색상/기호/진동 등 환경설정을 다룬다
class SettingsFragment : PreferenceFragmentCompat(), IOnFocusListenable {

    private lateinit var keyboardPreferences: KeyboardPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        keyboardPreferences = KeyboardPreferences(requireActivity())

        // 별도 스레드에서 첫 실행 여부를 확인해 인트로 화면을 띄운다
        Thread {
            if (keyboardPreferences.isFirstStart()) {
                startActivity(Intent(activity, IntroActivity::class.java))
                keyboardPreferences.setFirstStart(false)
            }
        }.start()

        // 숫자만 입력 가능하게 제한
        val numberOnlyPreferences = arrayOf("vibrate_ms", "font_size", "size_portrait", "size_landscape")
        for (key in numberOnlyPreferences) {
            val editTextPreference = preferenceManager.findPreference<EditTextPreference>(key)!!
            editTextPreference.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            }
        }

        val themePreference = preferenceManager.findPreference<ListPreference>("theme")!!
        themePreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (!keyboardPreferences.getCustomTheme()) {
                    val index = newValue.toString().toInt()
                    preference.summary = resources.getStringArray(R.array.Themes)[index]
                    setThemeByIndex(index)
                    true
                } else {
                    preference.summary = "Custom Theme is set"
                    false
                }
            }

        val bundle = arguments
        if (bundle != null && bundle.getInt("notification") == 1) {
            scrollToPreference("notification")
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == null) {
            return false
        }
        when (preference.key) {
            "change_keyboard" -> {
                val imm = requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
                preference.summary = getCurrentImeLabel(requireActivity().applicationContext)
            }
            "bg_colour_picker", "fg_colour_picker" -> {
                openColourPicker(preference.key)
                preferenceManager.findPreference<Preference>("theme")!!.summary = "Custom Theme is set"
            }
            "restore_default" -> confirmReset()
            "restore_old" -> classicSymbols()
            else -> {}
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun confirmReset() {
        AlertDialog.Builder(activity)
            .setTitle("Reset?")
            .setMessage("This will reset all your custom symbols to the default")
            .setPositiveButton("Yes") { _, _ ->
                keyboardPreferences.resetAllToDefault()
                preferenceScreen.removeAll()
                addPreferencesFromResource(R.xml.preferences)
            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

    fun classicSymbols() {
        AlertDialog.Builder(activity)
            .setTitle("Reset?")
            .setMessage("This will reset all your custom symbols to the old CodeBoard layout")
            .setPositiveButton("Yes") { _, _ ->
                keyboardPreferences.resetAllToDefault()
                var newValue = "()1234567890#"
                keyboardPreferences.setCustomSymbolsMain(newValue)
                keyboardPreferences.setCustomSymbolsSym(newValue)
                newValue = "+-=:*/{}+$[]"
                keyboardPreferences.setCustomSymbolsMain2(newValue)
                keyboardPreferences.setCustomSymbolsSym2(newValue)
                newValue = "&|%\\<>;',."
                keyboardPreferences.setCustomSymbolsMainBottom(newValue)
                keyboardPreferences.setCustomSymbolsSymBottom(newValue)
                preferenceScreen.removeAll()
                addPreferencesFromResource(R.xml.preferences)
            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

    private fun setThemeByIndex(index: Int) {
        val themeInfo: ThemeInfo = when (index) {
            1 -> ThemeDefinitions.MaterialDark()
            2 -> ThemeDefinitions.MaterialWhite()
            3 -> ThemeDefinitions.PureBlack()
            4 -> ThemeDefinitions.White()
            5 -> ThemeDefinitions.Blue()
            6 -> ThemeDefinitions.Purple()
            else -> ThemeDefinitions.Default()
        }
        keyboardPreferences.setBgColor(themeInfo.backgroundColor.toString())
        keyboardPreferences.setFgColor(themeInfo.foregroundColor.toString())
    }

    fun openColourPicker(key: String) {
        var color = 0
        if (key == "bg_colour_picker") {
            color = keyboardPreferences.getBgColor()
        } else if (key == "fg_colour_picker") {
            color = keyboardPreferences.getFgColor()
        }
        val cp = ColorPicker(activity, Color.red(color), Color.green(color), Color.blue(color))
        cp.show()
        cp.enableAutoClose()
        cp.setCallback { chosen ->
            if (key == "bg_colour_picker") {
                keyboardPreferences.setBgColor(chosen.toString())
            } else if (key == "fg_colour_picker") {
                keyboardPreferences.setFgColor(chosen.toString())
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            val imePreference = preferenceManager.findPreference<Preference>("change_keyboard")!!
            imePreference.summary = getCurrentImeLabel(requireActivity().applicationContext)
        }
    }

    companion object {
        // 현재 시스템 기본 입력기(IME)의 사람이 읽을 수 있는 이름을 반환
        @JvmStatic
        fun getCurrentImeLabel(context: Context): CharSequence? {
            var readableName: CharSequence? = null
            val keyboard = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
            )
            val componentName = ComponentName.unflattenFromString(keyboard)
            if (componentName != null) {
                val packageName = componentName.packageName
                try {
                    val packageManager = context.packageManager
                    val info = packageManager.getApplicationInfo(packageName, 0)
                    readableName = info.loadLabel(packageManager)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            return readableName
        }
    }
}
