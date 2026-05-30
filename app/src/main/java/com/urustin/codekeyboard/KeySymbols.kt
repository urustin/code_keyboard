package com.urustin.codekeyboard

// 표준 US 자판 기준, Shift를 눌렀을 때 숫자/기호가 바뀌는 매핑
object KeySymbols {
    val SHIFT: Map<Char, Char> = mapOf(
        '`' to '~',
        '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
        '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')',
        '-' to '_', '=' to '+',
        ',' to '<', '.' to '>', '/' to '?', '?' to '/',
        ';' to ':', ':' to ';',
        '\'' to '"', '"' to '\''
    )
}
