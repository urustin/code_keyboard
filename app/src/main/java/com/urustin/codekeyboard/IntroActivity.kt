package com.urustin.codekeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPage

// 첫 실행 시 보여주는 인트로(튜토리얼) 화면. 키보드 활성화 안내 슬라이드를 구성한다
class IntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(IntroFragment.newInstance(R.layout.codeboard_intro1))
        addSlide(IntroFragment.newInstance(R.layout.codeboard_intro2))

        val sliderPage = SliderPage()
        sliderPage.title = "All the shortcuts!"
        sliderPage.description = "Click 'ctrl' for select all, cut, copy, paste, or undo." +
            "\nCtrl+Shift+Z for redo" + "\n Long press Space to change keyboard"
        sliderPage.imageDrawable = R.drawable.intro_tutorial
        sliderPage.backgroundColor = Color.parseColor("#3F51B5")
        addSlide(AppIntroFragment.newInstance(sliderPage))
        // 마법사 모드로 설정해 Skip 비활성화
        isWizardMode = true
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
    }

    // XML onClick: 시스템 입력기 설정 화면 열기
    fun enableButtonIntro(v: View) {
        val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }

    // XML onClick: 입력기 선택 팝업 띄우기
    fun changeButtonIntro(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
