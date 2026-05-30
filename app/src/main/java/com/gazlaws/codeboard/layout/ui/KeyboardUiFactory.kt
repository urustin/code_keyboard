package com.gazlaws.codeboard.layout.ui

import android.content.Context
import android.inputmethodservice.KeyboardView
import android.view.View
import android.widget.RelativeLayout
import com.gazlaws.codeboard.layout.Key
import com.gazlaws.codeboard.theme.ThemeDefinitions
import com.gazlaws.codeboard.theme.ThemeInfo
import com.gazlaws.codeboard.theme.UiTheme

// 빌드된 Key 목록과 테마로부터 실제 키보드 View(레이아웃 + 버튼들)를 생성한다
class KeyboardUiFactory(private val inputService: KeyboardView.OnKeyboardActionListener) {

    @JvmField
    var theme: ThemeInfo = ThemeDefinitions.Default()

    fun createKeyboardView(context: Context, keys: Collection<Key>): KeyboardLayoutView {
        val uiTheme = UiTheme.buildFromInfo(this.theme)
        val layout = createKeyGroupView(context, uiTheme)
        for (key in keys) {
            val params = getKeyLayoutParams(key)
            val view = createKeyView(context, key, uiTheme)
            layout.addView(view, params)
        }
        return layout
    }

    private fun createKeyGroupView(context: Context, uiTheme: UiTheme): KeyboardLayoutView {
        return KeyboardLayoutView(context, uiTheme)
    }

    private fun createKeyView(context: Context, key: Key, uiTheme: UiTheme): KeyboardButtonView {
        val view = KeyboardButtonView(context, key, inputService, uiTheme)
        val box = key.box!!
        view.layout(box.left.toInt(), box.top.toInt(), box.right.toInt(), box.bottom.toInt())
        return view
    }

    private fun getKeyLayoutParams(key: Key): RelativeLayout.LayoutParams {
        val box = key.box!!
        val width = box.width.toInt()
        val height = box.height.toInt()
        val params = RelativeLayout.LayoutParams(width, height)
        params.leftMargin = box.x.toInt()
        params.topMargin = box.y.toInt()
        return params
    }
}
