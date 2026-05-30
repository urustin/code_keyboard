package com.gazlaws.codeboard.theme

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils

// ThemeInfo를 실제 그리기에 쓰는 Paint/색상 등으로 변환해 보관하는 클래스
class UiTheme private constructor() {

    @JvmField var foregroundPaint: Paint = Paint()
    @JvmField var backgroundColor: Int = 0xff000000.toInt()
    @JvmField var fontHeight = 0f

    @JvmField var buttonBodyPadding = 5.0f
    @JvmField var buttonBodyPaint: Paint = Paint()
    @JvmField var buttonBodyBorderRadius = 8.0f
    @JvmField var enablePreview = false
    @JvmField var enableBorder = false
    @JvmField var portraitSize = 0f
    @JvmField var landscapeSize = 0f

    companion object {
        // ThemeInfo로부터 그리기용 UiTheme를 구성한다
        @JvmStatic
        fun buildFromInfo(info: ThemeInfo): UiTheme {
            val theme = UiTheme()
            theme.portraitSize = info.size
            theme.landscapeSize = info.sizeLandscape
            theme.enablePreview = info.enablePreview
            theme.enableBorder = info.enableBorder
            // 배경 - 테두리가 있으면 약간 더 어둡게
            theme.backgroundColor = if (info.enableBorder) {
                ColorUtils.blendARGB(info.backgroundColor, Color.BLACK, 0.2f)
            } else {
                info.backgroundColor
            }
            // 버튼 본체
            theme.buttonBodyPaint.color = info.backgroundColor
            // 전경(글자)
            theme.foregroundPaint.color = info.foregroundColor
            theme.fontHeight = info.fontSize
            theme.foregroundPaint.textSize = theme.fontHeight
            theme.foregroundPaint.textAlign = Paint.Align.CENTER
            theme.foregroundPaint.isAntiAlias = true
            theme.foregroundPaint.typeface = Typeface.DEFAULT
            return theme
        }
    }
}
