package com.gazlaws.codeboard.layout

// 키의 사각 영역(좌표/크기)을 표현하고 경계값을 계산하는 클래스
class Box {
    @JvmField var x = 0f
    @JvmField var y = 0f
    @JvmField var width = 0f
    @JvmField var height = 0f

    val area: Float get() = width * height
    val left: Float get() = x
    val right: Float get() = x + width
    val top: Float get() = y
    val bottom: Float get() = y + height

    companion object {
        // 좌표/크기로 Box 생성하는 팩토리
        @JvmStatic
        fun create(x: Float, y: Float, width: Float, height: Float): Box {
            val box = Box()
            box.x = x
            box.y = y
            box.width = width
            box.height = height
            return box
        }
    }
}
