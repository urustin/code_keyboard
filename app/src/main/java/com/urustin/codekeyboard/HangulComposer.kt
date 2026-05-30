package com.urustin.codekeyboard

import android.view.inputmethod.InputConnection

// 두벌식 한글 입력 오토마타: 자모를 받아 초성/중성/종성을 조합해 음절로 만들고
// InputConnection의 setComposingText/commitText로 화면에 반영한다
class HangulComposer {

    private var cho = -1   // 초성 인덱스(-1 = 없음)
    private var jung = -1  // 중성 인덱스(-1 = 없음)
    private var jong = 0   // 종성 인덱스(0 = 없음)

    // 현재 조합 중인 음절/자모가 있는지
    val isComposing: Boolean
        get() = cho >= 0 || jung >= 0

    fun reset() { cho = -1; jung = -1; jong = 0 }

    // 자모 입력 처리(모음/자음 분기)
    fun onJamo(ic: InputConnection, jamo: Char) {
        when {
            JUNG.indexOf(jamo) >= 0 -> handleVowel(ic, JUNG.indexOf(jamo), jamo)
            CHO.indexOf(jamo) >= 0 -> handleConsonant(ic, CHO.indexOf(jamo), jamo)
            else -> { finish(ic); ic.commitText(jamo.toString(), 1) }
        }
        ic.setComposingText(block(), 1)
    }

    private fun handleVowel(ic: InputConnection, v: Int, jamo: Char) {
        when {
            cho >= 0 && jung < 0 -> jung = v                       // 초성 뒤 모음 → 음절 형성
            cho >= 0 && jung >= 0 && jong == 0 -> {                // 받침 없는 음절에 모음
                val comp = compoundVowel(jung, jamo)
                if (comp >= 0) jung = comp                         // 복합 모음(ㅗ+ㅏ=ㅘ)
                else { commit(ic); jung = v }                      // 새 단독 모음
            }
            cho >= 0 && jung >= 0 -> {                             // 받침 있는 음절에 모음 → 받침이 다음 초성으로
                val split = JONG_SPLIT[jong]
                val moveCho: Char
                if (split != null) { jong = split.first; moveCho = split.second }
                else { moveCho = JONG[jong]; jong = 0 }
                commit(ic)
                cho = CHO.indexOf(moveCho); jung = v
            }
            jung >= 0 -> {                                         // 단독 모음 뒤 모음
                val comp = compoundVowel(jung, jamo)
                if (comp >= 0) jung = comp else { commit(ic); jung = v }
            }
            else -> jung = v                                       // 단독 모음 시작
        }
    }

    private fun handleConsonant(ic: InputConnection, c: Int, jamo: Char) {
        when {
            cho < 0 && jung < 0 -> cho = c                         // 초성 시작
            cho >= 0 && jung < 0 -> { commit(ic); cho = c }        // 초성만 두 번 → 앞 초성 확정
            cho >= 0 && jung >= 0 && jong == 0 -> {                // 받침 자리
                val j = JONG.indexOf(jamo)
                if (j > 0) jong = j else { commit(ic); cho = c }   // ㄸㅃㅉ는 받침 불가 → 새 초성
            }
            cho >= 0 && jung >= 0 -> {                             // 이미 받침 있음 → 복합 받침 시도
                val comp = compoundJong(jong, jamo)
                if (comp > 0) jong = comp else { commit(ic); cho = c }
            }
            else -> { commit(ic); cho = c }                        // 단독 모음 뒤 자음
        }
    }

    // 조합 중인 내용을 확정(공백/엔터/모드전환 등 직전에 호출)
    fun finish(ic: InputConnection) {
        if (isComposing) { ic.commitText(block(), 1); reset() }
    }

    // 조합 중 백스페이스: 마지막 구성요소만 제거. 처리했으면 true
    fun backspace(ic: InputConnection): Boolean {
        if (!isComposing) return false
        when {
            jong > 0 -> jong = JONG_BACK[jong] ?: 0
            jung >= 0 -> jung = JUNG_BACK[jung] ?: -1
            else -> cho = -1
        }
        if (isComposing) ic.setComposingText(block(), 1)
        else { ic.setComposingText("", 1); ic.finishComposingText() }
        return true
    }

    // 현재 상태를 음절(또는 단독 자모) 문자열로 변환
    private fun block(): String = when {
        cho >= 0 && jung >= 0 -> (0xAC00 + (cho * 21 + jung) * 28 + jong).toChar().toString()
        cho >= 0 -> CHO[cho].toString()
        jung >= 0 -> JUNG[jung].toString()
        else -> ""
    }

    private fun commit(ic: InputConnection) { ic.commitText(block(), 1); reset() }

    private fun compoundVowel(jung: Int, jamo: Char): Int {
        val r = V_COMB[JUNG[jung].toString() + jamo] ?: return -1
        return JUNG.indexOf(r)
    }

    private fun compoundJong(jong: Int, jamo: Char): Int {
        val r = J_COMB[JONG[jong].toString() + jamo] ?: return -1
        return JONG.indexOf(r)
    }

    companion object {
        // 두벌식 자모 순서(유니코드 조합용 호환 자모)
        private const val CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        private const val JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
        private const val JONG = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

        // 복합 모음/복합 받침 결합 규칙
        private val V_COMB = mapOf(
            "ㅗㅏ" to 'ㅘ', "ㅗㅐ" to 'ㅙ', "ㅗㅣ" to 'ㅚ', "ㅜㅓ" to 'ㅝ',
            "ㅜㅔ" to 'ㅞ', "ㅜㅣ" to 'ㅟ', "ㅡㅣ" to 'ㅢ'
        )
        private val J_COMB = mapOf(
            "ㄱㅅ" to 'ㄳ', "ㄴㅈ" to 'ㄵ', "ㄴㅎ" to 'ㄶ', "ㄹㄱ" to 'ㄺ', "ㄹㅁ" to 'ㄻ',
            "ㄹㅂ" to 'ㄼ', "ㄹㅅ" to 'ㄽ', "ㄹㅌ" to 'ㄾ', "ㄹㅍ" to 'ㄿ', "ㄹㅎ" to 'ㅀ', "ㅂㅅ" to 'ㅄ'
        )

        // 복합 받침 분해: 받침인덱스 → (남길 받침인덱스, 다음 초성으로 보낼 자음)
        private val JONG_SPLIT: Map<Int, Pair<Int, Char>> = buildMap {
            J_COMB.forEach { (k, v) -> put(JONG.indexOf(v), Pair(JONG.indexOf(k[0]), k[1])) }
        }
        // 백스페이스용 복합 받침/모음 → 단일로 되돌림
        private val JONG_BACK: Map<Int, Int> = buildMap {
            J_COMB.forEach { (k, v) -> put(JONG.indexOf(v), JONG.indexOf(k[0])) }
        }
        private val JUNG_BACK: Map<Int, Int> = buildMap {
            V_COMB.forEach { (k, v) -> put(JUNG.indexOf(v), JUNG.indexOf(k[0])) }
        }
    }
}
