package com.gazlaws.codeboard.theme

// 미리 정의된 키보드 색상 테마들을 생성하는 팩토리 모음
object ThemeDefinitions {

    private val whiteColor = 0xffffffff.toInt()
    private val blackColor = 0xff000000.toInt()

    @JvmStatic
    fun Default(): ThemeInfo = MaterialDark()

    @JvmStatic
    fun MaterialDark(): ThemeInfo {
        val theme = ThemeInfo()
        theme.foregroundColor = whiteColor
        theme.backgroundColor = 0xff263238.toInt()
        return theme
    }

    @JvmStatic
    fun MaterialWhite(): ThemeInfo {
        val theme = Default()
        theme.foregroundColor = blackColor
        theme.backgroundColor = 0xffeceff1.toInt()
        return theme
    }

    @JvmStatic
    fun PureBlack(): ThemeInfo {
        val theme = MaterialDark()
        theme.backgroundColor = blackColor
        return theme
    }

    @JvmStatic
    fun White(): ThemeInfo {
        val theme = MaterialWhite()
        theme.backgroundColor = whiteColor
        return theme
    }

    @JvmStatic
    fun Blue(): ThemeInfo {
        val theme = MaterialDark()
        theme.backgroundColor = 0xff0d47a1.toInt()
        return theme
    }

    @JvmStatic
    fun Purple(): ThemeInfo {
        val theme = MaterialDark()
        theme.backgroundColor = 0xff4a148c.toInt()
        return theme
    }
}
