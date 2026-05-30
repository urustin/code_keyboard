package com.urustin.codekeyboard

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.TypedValue
import androidx.preference.PreferenceManager

// SharedPreferences를 감싸 키보드 설정 값들을 읽고 쓰는 헬퍼
class KeyboardPreferences(contextWrapper: ContextWrapper) {

    private val res: Resources = contextWrapper.resources
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(contextWrapper)

    fun isFirstStart(): Boolean = read("FIRST_START", true)

    fun setFirstStart(value: Boolean) = write("FIRST_START", value)

    fun isSoundEnabled(): Boolean = read("sound", res.getBoolean(R.bool.sound))

    fun setSoundEnabled(bool: Boolean) = write("sound", bool)

    fun isVibrateEnabled(): Boolean = try {
        read("vibrate", res.getBoolean(R.bool.vibrate))
    } catch (e: Exception) {
        true
    }

    // 주의: EditTextPreference는 값을 문자열로 저장하므로 null일 수 있음
    fun getVibrateLength(): Int = try {
        safeRead("vibrate_ms", res.getInteger(R.integer.vibrate_length).toString()).toInt()
    } catch (e: Exception) {
        1
    }

    fun setVibrateLength(length: Int) = write("vibrate_ms", length.toString())

    fun getBgColor(): Int =
        safeRead("bg_colour_picker", res.getInteger(R.integer.bg_color).toString()).toInt()

    fun setBgColor(color: String) = write("bg_colour_picker", color)

    fun getFgColor(): Int =
        safeRead("fg_colour_picker", res.getInteger(R.integer.fg_color).toString()).toInt()

    fun setFgColor(color: String) = write("fg_colour_picker", color)

    fun getPortraitSize(): Int = try {
        safeRead("size_portrait", res.getInteger(R.integer.size_portrait).toString()).toInt()
    } catch (e: Exception) {
        40
    }

    fun getLandscapeSize(): Int = try {
        safeRead("size_landscape", res.getInteger(R.integer.size_landscape).toString()).toInt()
    } catch (e: Exception) {
        70
    }

    fun getFontSizeAsSp(): Float {
        val fontSize = safeRead("font_size", res.getInteger(R.integer.font_size).toString())
        val dm = res.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat(), dm)
    }

    fun isPreviewEnabled(): Boolean = read("preview", res.getBoolean(R.bool.preview))

    fun isBorderEnabled(): Boolean = read("borders", res.getBoolean(R.bool.borders))

    fun getCustomSymbolsMain(): String? =
        read("input_symbols_main", res.getString(R.string.input_symbols_main))

    fun setCustomSymbolsMain(symbols: String) = write("input_symbols_main", symbols)

    fun getCustomSymbolsMain2(): String? =
        read("input_symbols_main_2", res.getString(R.string.input_symbols_main_2))

    fun setCustomSymbolsMain2(symbols: String) = write("input_symbols_main_2", symbols)

    fun getCustomSymbolsMainBottom(): String? =
        read("input_symbols_main_bottom", res.getString(R.string.input_symbols_main_bottom))

    fun setCustomSymbolsMainBottom(symbols: String) = write("input_symbols_main_bottom", symbols)

    fun getCustomSymbolsSym(): String? =
        read("input_symbols_sym", res.getString(R.string.input_symbols_sym_2))

    fun setCustomSymbolsSym(symbols: String) = write("input_symbols_sym", symbols)

    fun getCustomSymbolsSym2(): String? =
        read("input_symbols_sym_2", res.getString(R.string.input_symbols_sym))

    fun getCustomSymbolsSym3(): String? =
        read("input_symbols_sym_3", res.getString(R.string.input_symbols_sym_3))

    fun getCustomSymbolsSym4(): String? =
        read("input_symbols_sym_4", res.getString(R.string.input_symbols_sym_4))

    fun setCustomSymbolsSym2(symbols: String) = write("input_symbols_sym_2", symbols)

    fun setCustomSymbolsSym3(symbols: String) = write("input_symbols_sym_3", symbols)

    fun setCustomSymbolsSym4(symbols: String) = write("input_symbols_sym_4", symbols)

    fun setCustomSymbolsSymBottom(symbols: String) = write("input_symbols_sym_bottom", symbols)

    fun getNavBar(): Boolean = read("navbar", res.getBoolean(R.bool.navbar))

    fun getNavBarDark(): Boolean = read("navbar_dark", res.getBoolean(R.bool.navbar_dark))

    fun getLayoutIndex(): Int = safeRead("layout", "0").toInt()

    fun getThemeIndex(): Int = safeRead("theme", "0").toInt()

    fun getCustomTheme(): Boolean = read("custom_theme", res.getBoolean(R.bool.custom_theme))

    fun getPin1(): String? = read("pin1", res.getString(R.string.pin1))

    fun getPin2(): String? = read("pin2", res.getString(R.string.pin2))

    fun getPin3(): String? = read("pin3", res.getString(R.string.pin3))

    fun getPin4(): String? = read("pin4", res.getString(R.string.pin4))

    fun getPin5(): String? = read("pin5", res.getString(R.string.pin5))

    fun getPin6(): String? = read("pin6", res.getString(R.string.pin6))

    fun getPin7(): String? = read("pin7", res.getString(R.string.pin7))

    fun getNotification(): Boolean = read("notification", res.getBoolean(R.bool.notification))

    fun getTopRowActions(): Boolean = read("top_row_actions", res.getBoolean(R.bool.top_row_actions))

    fun resetAllToDefault() {
        preferences.edit().clear().apply()
        setFirstStart(false)
    }

    private fun read(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    private fun write(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    private fun read(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    private fun read(key: String, defaultValue: String?): String? =
        preferences.getString(key, defaultValue)

    private fun safeRead(key: String, defaultValue: String): String =
        read(key, defaultValue) ?: "0"

    private fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
