package com.gazlaws.codeboard.theme

// 윈도우 포커스 변화 콜백을 받기 위한 인터페이스
interface IOnFocusListenable {
    fun onWindowFocusChanged(hasFocus: Boolean)
}
