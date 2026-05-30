package com.urustin.codekeyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.urustin.codekeyboard.layout.Box
import com.urustin.codekeyboard.layout.builder.KeyboardLayoutBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

// 전체 레이아웃 빌더가 행/키를 올바른 개수로 만드는지 검증하는 계측 테스트
class KeyboardLayoutBuilderTest {

    @Test
    fun build_returnsCorrectNumberOfKeys() {
        val keyboard = builder().setBox(Box.create(0f, 0f, 100f, 100f))
            .newRow().addKey(1f).addKey(1f)
            .newRow().addKey(1f).addKey(1f).build()
        assertEquals(4, keyboard.size)
    }

    private fun builder(): KeyboardLayoutBuilder {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        return KeyboardLayoutBuilder(appContext)
    }
}
