package com.gazlaws.codeboard.layout

import com.gazlaws.codeboard.layout.builder.KeyInfo

// 키 하나를 구성하는 박스(위치/크기)와 정보(코드/라벨 등)를 묶는 단순 홀더
class Key {
    @JvmField var box: Box? = null
    @JvmField var info: KeyInfo? = null
}
