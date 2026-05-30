package com.urustin.codekeyboard

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test

// MainActivity가 정상적으로 실행되는지 검증하는 계측 테스트
class MainActivityTest {

    @Test
    fun mainActivity_canBeLaunched() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClassName(instrumentation.targetContext, MainActivity::class.java.name)
        instrumentation.startActivitySync(intent)

        val currentActivity = InstrumentationRegistry.getInstrumentation().waitForMonitor(monitor)
        assertNotNull(currentActivity)

        instrumentation.removeMonitor(monitor)
    }
}
