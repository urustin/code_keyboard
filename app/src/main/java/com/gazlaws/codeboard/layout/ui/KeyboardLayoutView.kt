package com.gazlaws.codeboard.layout.ui

import android.content.Context
import android.view.ViewGroup
import com.gazlaws.codeboard.theme.UiTheme

// 키 버튼들을 담는 컨테이너 ViewGroup. 측정/배치와 수정자(Shift/Ctrl) 전파를 담당한다
class KeyboardLayoutView(context: Context, private val uiTheme: UiTheme) : ViewGroup(context) {

    init {
        setBackgroundColor(uiTheme.backgroundColor)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val metrics = context.resources.displayMetrics
        val availableHeight = metrics.heightPixels
        val availableWidth = metrics.widthPixels

        val keyboardSize = if (availableHeight > availableWidth) {
            uiTheme.portraitSize
        } else {
            uiTheme.landscapeSize
        }

        setMeasuredDimension(availableWidth, (availableHeight * keyboardSize).toInt())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            getChildAt(i).layout(l, t, r, b)
        }
    }

    fun applyShiftModifier(shiftPressed: Boolean) {
        for (button in keyboardButtons) {
            button.applyShiftModifier(shiftPressed)
        }
    }

    fun applyCtrlModifier(ctrlPressed: Boolean) {
        for (button in keyboardButtons) {
            button.applyCtrlModifier(ctrlPressed)
        }
    }

    // 자식 View 중 키 버튼만 추려서 반환
    private val keyboardButtons: Collection<KeyboardButtonView>
        get() {
            val list = ArrayList<KeyboardButtonView>(childCount)
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is KeyboardButtonView) {
                    list.add(child)
                }
            }
            return list
        }
}
