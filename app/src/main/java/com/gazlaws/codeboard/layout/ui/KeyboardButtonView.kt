package com.gazlaws.codeboard.layout.ui

import android.content.Context
import android.graphics.Canvas
import android.inputmethodservice.KeyboardView
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import com.gazlaws.codeboard.layout.Key
import com.gazlaws.codeboard.layout.builder.KeyInfo
import com.gazlaws.codeboard.theme.UiTheme
import java.util.Timer
import java.util.TimerTask

// 키 하나에 해당하는 커스텀 View. 그리기/터치/반복입력/수정자 라벨 전환을 담당한다
class KeyboardButtonView(
    context: Context,
    private val key: Key,
    private val inputService: KeyboardView.OnKeyboardActionListener,
    private val uiTheme: UiTheme
) : View(context) {

    private val info: KeyInfo = key.info!!
    private var timer: Timer? = null
    private var currentLabel: String? = info.label
    private var pressed = false

    init {
        // 그림자 활성화
        outlineProvider = ViewOutlineProvider.BOUNDS
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> onPress()
            MotionEvent.ACTION_UP -> onRelease()
            else -> {}
        }
        return true
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val box = key.box!!
        val w = r - l
        val h = b - t
        val left = (l + w * box.left).toInt()
        val right = (l + w * box.right).toInt()
        val top = (t + h * box.top).toInt()
        val bottom = (t + h * box.bottom).toInt()
        super.layout(left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        drawButtonBody(canvas)
        drawButtonContent(canvas)
        super.draw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        autoReleaseIfPressed()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        autoReleaseIfPressed()
    }

    private fun drawButtonContent(canvas: Canvas) {
        val x = (width / 2).toFloat()
        val y = height / 2 + uiTheme.fontHeight / 3
        canvas.drawText(currentLabel ?: "", x, y, uiTheme.foregroundPaint)

        val icon = info.icon
        if (icon != null) {
            icon.setTint(uiTheme.foregroundPaint.color)

            val padding = uiTheme.buttonBodyPadding.toInt() * 2
            val top: Int
            val left: Int
            val squareSize: Int
            if (width > height) {
                top = 2 * padding
                squareSize = height / 2 - top
                left = width / 2 - squareSize
            } else {
                left = 2 * padding
                squareSize = width / 2 - left
                top = height / 2 - squareSize
            }
            val right = left + squareSize * 2
            val bottom = top + squareSize * 2
            icon.setBounds(left, top, right, bottom)
            icon.draw(canvas)
        }
    }

    private fun drawButtonBody(canvas: Canvas) {
        val left = uiTheme.buttonBodyPadding
        val top = uiTheme.buttonBodyPadding
        val right = width - uiTheme.buttonBodyPadding
        val bottom = height - uiTheme.buttonBodyPadding
        val rx = uiTheme.buttonBodyBorderRadius
        val ry = uiTheme.buttonBodyBorderRadius
        canvas.drawRoundRect(left, top, right, bottom, rx, ry, uiTheme.buttonBodyPaint)
    }

    private fun onPress() {
        pressed = true
        inputService.onPress(info.code)
        if (info.isRepeatable) {
            startRepeating()
        }
        submitKeyEvent()
        animatePress()
    }

    private fun onRelease() {
        pressed = false
        // 주의: 방향키가 입력 뷰 밖으로 나가면 onRelease가 호출되지 않을 수 있음
        if (info.code != 0) {
            inputService.onRelease(info.code)
        }
        if (info.isRepeatable) {
            stopRepeating()
        }
        animateRelease()
    }

    private fun submitKeyEvent() {
        if (info.code != 0) {
            inputService.onKey(info.code, null)
        }
        if (info.outputText != null) {
            inputService.onText(info.outputText)
        }
    }

    private fun autoReleaseIfPressed() {
        if (pressed) {
            onRelease()
        }
    }

    private fun stopRepeating() {
        val t = timer ?: return
        t.cancel()
        timer = null
    }

    private fun startRepeating() {
        if (timer != null) {
            stopRepeating()
            return
        }
        timer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    submitKeyEvent()
                }
            }, 400, 50)
        }
    }

    private fun animatePress() {
        if (uiTheme.enablePreview) {
            translationY = -200.0f
            scaleX = 1.2f
            scaleY = 1.2f
            elevation = 21.0f
        } else {
            alpha = .1f
        }
    }

    private fun animateRelease() {
        if (uiTheme.enablePreview) {
            translationY = 0.0f
            scaleX = 1.0f
            scaleY = 1.0f
            elevation = 0.0f
        } else {
            animate().alpha(1.0f).setDuration(400)
        }
    }

    fun applyShiftModifier(shiftPressed: Boolean) {
        if (info.onShiftLabel != null) {
            val nextLabel = if (shiftPressed) info.onShiftLabel else info.label
            setCurrentLabel(nextLabel)
        }
    }

    fun applyCtrlModifier(ctrlPressed: Boolean) {
        if (info.onCtrlLabel != null) {
            val nextLabel = if (ctrlPressed) info.onCtrlLabel else info.label
            setCurrentLabel(nextLabel)
        }
    }

    private fun setCurrentLabel(nextLabel: String?) {
        if (nextLabel !== currentLabel) {
            currentLabel = nextLabel
            invalidate()
        }
    }
}
