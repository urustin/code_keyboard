package com.urustin.codekeyboard.layout.builder

import android.graphics.drawable.Drawable

// 실제 키를 빌드하기 위한 정보(코드/라벨/크기/수정자 여부 등)를 담는다
class KeyInfo {
    // 키를 누를 때 보내는 키 코드
    @JvmField var code = 0

    // 키보드에 표시되는 라벨
    @JvmField var label: String? = null

    // 같은 행 내 다른 키 대비 상대 크기
    @JvmField var size = 0f

    // 길게 눌러 반복 입력 가능한지
    @JvmField var isRepeatable = false

    // 수정자 키(Shift/Ctrl)인지
    @JvmField var isModifier = false

    // 키를 눌렀을 때 출력할 텍스트
    @JvmField var outputText: String? = null

    // Shift 수정자가 눌렸을 때 표시할 라벨
    @JvmField var onShiftLabel: String? = null

    // Ctrl 수정자가 눌렸을 때 표시할 라벨
    @JvmField var onCtrlLabel: String? = null

    // 키보드에 표시되는 아이콘
    @JvmField var icon: Drawable? = null
}
