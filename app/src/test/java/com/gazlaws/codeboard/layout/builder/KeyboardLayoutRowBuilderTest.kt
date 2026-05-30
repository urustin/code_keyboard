package com.gazlaws.codeboard.layout.builder

import com.gazlaws.codeboard.layout.Box
import com.gazlaws.codeboard.layout.Key
import org.junit.Assert.assertEquals
import org.junit.Test

// 한 행 빌더의 키 개수/너비/높이/간격 계산을 검증하는 단위 테스트
class KeyboardLayoutRowBuilderTest {

    private val defaultBox = Box.create(0f, 0f, 100f, 10f)

    @Test
    fun setBox_doesNotThrow() {
        builder().setBox(Box.create(0f, 600f, 320f, 32f))
    }

    @Test
    fun addKey_canBeCalledMultipleTimes() {
        builder().setBox(defaultBox).addKey(KeyInfo()).addKey(KeyInfo()).addKey(KeyInfo())
    }

    @Test
    fun build_returnsCorrectNumberOfKeys() {
        assertEquals(2, builder().setBox(defaultBox).addKey(KeyInfo()).addKey(KeyInfo()).build().size)
    }

    @Test
    fun build_proportinallySplitsAvaiableWidth() {
        val result = buildTwoKeysRow()
        assertEquals(result[0].box!!.width.toDouble(), 25.0, 0.01)
        assertEquals(result[1].box!!.width.toDouble(), 75.0, 0.01)
    }

    @Test
    fun build_usesAllAvaiableHeight() {
        val result = buildTwoKeysRow()
        assertEquals(result[0].box!!.height.toDouble(), 10.0, 0.01)
    }

    @Test
    fun build_addsGapBetweenKeys() {
        val result = buildTwoKeysRow(20f)
        assertEquals(result[0].box!!.width.toDouble(), 20.0, 0.01)
        assertEquals(result[1].box!!.width.toDouble(), 60.0, 0.01)
    }

    private fun builder() = KeyboardLayoutRowBuilder()

    private fun buildTwoKeysRow(gap: Float = 0f): ArrayList<Key> {
        val keyA = KeyInfo().apply { size = 1f }
        val keyB = KeyInfo().apply { size = 3f }
        return builder().setBox(defaultBox)
            .setGap(gap)
            .addKey(keyA).addKey(keyB).build()
    }
}
