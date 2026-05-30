package com.urustin.codekeyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.urustin.codekeyboard.layout.Box
import com.urustin.codekeyboard.layout.Definitions
import com.urustin.codekeyboard.layout.builder.KeyboardLayoutBuilder
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 각 레이아웃 정의가 유효한 키(코드 또는 출력 텍스트 보유)를 만드는지 검증하는 계측 테스트
class DefinitionsTest {

    @Test
    fun addArrowsRow_producesValidResult() {
        val definitions = Definitions(ApplicationProvider.getApplicationContext<Context>())
        val builder = builder()
        definitions.addArrowsRow(builder)
        validate(builder)
    }

    @Test
    fun addCopyPasteRow() {
        val definitions = Definitions(ApplicationProvider.getApplicationContext<Context>())
        val builder = builder()
        definitions.addCopyPasteRow(builder)
        validate(builder)
    }

    @Test
    fun addQwertyRows() {
        val builder = builder()
        Definitions.addQwertyRows(builder)
        validate(builder)
    }

    @Test
    fun addQwertzRows() {
        val builder = builder()
        Definitions.addQwertzRows(builder)
        validate(builder)
    }

    @Test
    fun addAzertyRows() {
        val builder = builder()
        Definitions.addAzertyRows(builder)
        validate(builder)
    }

    @Test
    fun addClipboardRow() {
        val definitions = Definitions(ApplicationProvider.getApplicationContext<Context>())
        val builder = builder()
        definitions.addClipboardActions(builder)
        validate(builder)
    }

    private fun validate(builder: KeyboardLayoutBuilder) {
        for (key in builder.build()) {
            assertNotNull(key.info)
            assertNotNull(key.box)
            // 코드 또는 출력 텍스트 중 하나는 반드시 설정돼 있어야 함
            assertTrue(key.info!!.code != 0 || key.info!!.outputText != null)
        }
    }

    private fun builder(): KeyboardLayoutBuilder {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        return KeyboardLayoutBuilder(appContext).setBox(Box.create(0f, 0f, 1f, 1f))
    }
}
