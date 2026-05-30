package com.urustin.codekeyboard

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.KeyboardView
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.graphics.ColorUtils
import com.urustin.codekeyboard.layout.Box
import com.urustin.codekeyboard.layout.Definitions
import com.urustin.codekeyboard.layout.Key
import com.urustin.codekeyboard.layout.builder.KeyboardLayoutBuilder
import com.urustin.codekeyboard.layout.builder.KeyboardLayoutException
import com.urustin.codekeyboard.layout.ui.KeyboardLayoutView
import com.urustin.codekeyboard.layout.ui.KeyboardUiFactory
import com.urustin.codekeyboard.theme.ThemeDefinitions
import com.urustin.codekeyboard.theme.ThemeInfo
import java.util.Timer
import java.util.TimerTask

// 코딩용 커스텀 키보드의 핵심 입력기(IME) 서비스. 키 입력/수정자/레이아웃 생성을 담당한다
class CodeBoardIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    var sEditorInfo: EditorInfo? = null
    private var vibratorOn = false
    private var vibrateLength = 0
    private var soundOn = false
    private var shiftLock = false
    private var ctrlLock = false
    private var shift = false
    private var ctrl = false
    private var mKeyboardState = R.integer.keyboard_normal
    private var timerLongPress: Timer? = null
    private var mKeyboardUiFactory: KeyboardUiFactory? = null
    private var mCurrentKeyboardLayoutView: KeyboardLayoutView? = null
    private var longPressedSpaceButton = false

    // NotificationReceiver가 키보드 강제 표시에 사용하는 토큰
    var mToken: IBinder? = null

    private var mNotificationReceiver: NotificationReceiver? = null

    // 한글 입력 모드 여부와 한글 조합기
    private var koreanMode = false
    private val hangulComposer = HangulComposer()

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // 주의: 롱프레스는 그 다음, 여기는 onDown 시점
        val ic: InputConnection = currentInputConnection
        var code = primaryCode.toChar()

        // 한글 모드: 자모는 조합기로 보내고, 백스페이스/그 외 키는 조합 확정 후 일반 처리
        if (koreanMode) {
            if (isHangulJamo(primaryCode)) {
                var jamo = primaryCode.toChar()
                if (shift) {
                    jamo = koreanShift(jamo)
                    if (!shiftLock) {
                        shift = false
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
                        shiftKeyUpdateView()
                    }
                }
                hangulComposer.onJamo(ic, jamo)
                return
            }
            if (primaryCode == -5 && hangulComposer.backspace(ic)) {
                return
            }
            hangulComposer.finish(ic)
        }

        when (primaryCode) {
            // shift/ctrl 메타 수정자를 쓰지 않는 케이스 먼저 처리
            53737 -> ic.performContextMenuAction(android.R.id.selectAll)
            53738 -> ic.performContextMenuAction(android.R.id.cut)
            53739 -> ic.performContextMenuAction(android.R.id.copy)
            53740 -> ic.performContextMenuAction(android.R.id.paste)
            53741 -> ic.performContextMenuAction(android.R.id.undo)
            53742 -> ic.performContextMenuAction(android.R.id.redo)
            KeyboardLayoutBuilder.CODE_SWITCH_LANGUAGE -> {
                // globe: 영문 ↔ 한글 입력 모드 토글
                koreanMode = !koreanMode
                setInputView(onCreateInputView())
            }
            -1 -> {
                // SYM: 키보드 상태 전환(일반 → 기호 → 클립보드)
                mKeyboardState = if (mKeyboardState == R.integer.keyboard_normal && !ctrl) {
                    R.integer.keyboard_sym
                } else if (ctrl) {
                    R.integer.keyboard_clipboard
                } else {
                    R.integer.keyboard_normal
                }
                // 뷰 재생성 - 단순히 shift 해제
                if (shift) {
                    shift = false
                    shiftLock = false
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
                }
                if (ctrl) {
                    ctrl = false
                    ctrlLock = false
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
                }
                setInputView(onCreateInputView())
                controlKeyUpdateView()
                shiftKeyUpdateView()
            }
            17 -> { // KEYCODE_CTRL_LEFT: ctrl 키 누름을 에뮬레이트
                if (!ctrlLock && !ctrl) {
                    ctrl = true
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT))
                } else if (!ctrlLock && ctrl) {
                    ctrl = false
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
                }
                controlKeyUpdateView()
            }
            16 -> { // KEYCODE_SHIFT_LEFT: shift 키 누름을 에뮬레이트(화살표 선택에 유용)
                if (!shiftLock && !shift) {
                    shift = true
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT))
                } else if (!shiftLock && shift) {
                    shift = false
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
                }
                shiftKeyUpdateView()
            }
            else -> {
                var meta = 0
                if (shift) {
                    meta = KeyEvent.META_SHIFT_ON
                    // 숫자/기호는 매핑된 기호로, 그 외(글자)는 대문자로
                    code = KeySymbols.SHIFT[code] ?: code.uppercaseChar()
                    if (!shiftLock) {
                        shift = false
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
                        shiftKeyUpdateView()
                    }
                }
                if (ctrl) {
                    meta = meta or KeyEvent.META_CTRL_ON
                    if (!ctrlLock) {
                        ctrl = false
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
                        controlKeyUpdateView()
                    }
                }
                // primaryCode(문자값)를 실제 KeyEvent 코드로 변환
                val ke = when (primaryCode) {
                    9 -> KeyEvent.KEYCODE_TAB
                    -2 -> KeyEvent.KEYCODE_ESCAPE
                    32 -> KeyEvent.KEYCODE_SPACE
                    -5 -> KeyEvent.KEYCODE_DEL
                    -4 -> KeyEvent.KEYCODE_ENTER
                    -6 -> KeyEvent.KEYCODE_F1
                    -7 -> KeyEvent.KEYCODE_F2
                    -8 -> KeyEvent.KEYCODE_F3
                    -9 -> KeyEvent.KEYCODE_F4
                    -10 -> KeyEvent.KEYCODE_F5
                    -11 -> KeyEvent.KEYCODE_F6
                    -12 -> KeyEvent.KEYCODE_F7
                    -13 -> KeyEvent.KEYCODE_F8
                    -14 -> KeyEvent.KEYCODE_F9
                    -15 -> KeyEvent.KEYCODE_F10
                    -16 -> KeyEvent.KEYCODE_F11
                    -17 -> KeyEvent.KEYCODE_F12
                    -18 -> KeyEvent.KEYCODE_MOVE_HOME
                    -19 -> KeyEvent.KEYCODE_MOVE_END
                    -20 -> KeyEvent.KEYCODE_INSERT
                    -21 -> KeyEvent.KEYCODE_FORWARD_DEL
                    -22 -> KeyEvent.KEYCODE_PAGE_UP
                    -23 -> KeyEvent.KEYCODE_PAGE_DOWN
                    // 방향 조이스틱처럼 inputConnection 밖으로 점프 가능
                    5000 -> KeyEvent.KEYCODE_DPAD_LEFT
                    5001 -> KeyEvent.KEYCODE_DPAD_DOWN
                    5002 -> KeyEvent.KEYCODE_DPAD_UP
                    5003 -> KeyEvent.KEYCODE_DPAD_RIGHT
                    else -> if (code.isLetter()) {
                        KeyEvent.keyCodeFromString("KEYCODE_" + code.uppercaseChar())
                    } else {
                        0
                    }
                }
                if (ke != 0) {
                    /*
                     * 스페이스 버튼에 ACTION_DOWN을 붙이지 않기 위한 분기.
                     * 스페이스가 롱프레스됐는지 먼저 확인한 뒤 올바른 출력을 내기 위함.
                     */
                    if (primaryCode != 32) {
                        ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, ke, 0, meta))
                    }
                    ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, ke, 0, meta))
                } else {
                    // 문자가 아닌 키들은 여기서 처리(수정자 미사용).
                    // 예: '0'(48)을 keyEvent로 처리하면 shift+0이 ')'가 되어버림
                    ic.commitText(code.toString(), 1)
                }
            }
        }
    }

    override fun onPress(primaryCode: Int) {
        if (soundOn) {
            val keypressSoundPlayer = MediaPlayer.create(this, R.raw.keypress_sound)
            keypressSoundPlayer.start()
            keypressSoundPlayer.setOnCompletionListener { mp -> mp.release() }
        }
        if (vibratorOn) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(vibrateLength.toLong())
        }

        clearLongPressTimer()
        timerLongPress = Timer()
        timerLongPress!!.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val uiHandler = Handler(Looper.getMainLooper())
                    uiHandler.post {
                        try {
                            onKeyLongPress(primaryCode)
                        } catch (e: Exception) {
                            Log.e(TAG, "uiHandler.run: " + e.message, e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Timer.run: " + e.message, e)
                }
            }
        }, ViewConfiguration.getLongPressTimeout().toLong())
    }

    override fun onExtractingInputChanged(ei: EditorInfo?) {
        Log.d(TAG, "onExtractingInputChanged: ")
    }

    override fun requestHideSelf(flags: Int) {
        val newFlag = flags
        Log.d(TAG, "requestHideSelf: $newFlag,$flags")
        // 아무것도 하지 않음
        super.requestHideSelf(newFlag)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        clearLongPressTimer()
    }

    override fun onViewClicked(focusChanged: Boolean) {
        super.onViewClicked(focusChanged)
        clearLongPressTimer()
    }

    override fun onRelease(primaryCode: Int) {
        /*
         * 스페이스 버튼이 떼어진 뒤 롱프레스였는지 확인한다.
         * 롱프레스였다면 아무것도 안 하고, 아니면 "space"를 출력한다.
         */
        if (primaryCode == 32 && !longPressedSpaceButton) {
            val ic = currentInputConnection
            ic.commitText(primaryCode.toChar().toString(), 1)
        }
        longPressedSpaceButton = false
        clearLongPressTimer()
    }

    fun onKeyLongPress(keyCode: Int) {
        // 롱클릭 처리(onKey 다음에 따라옴)
        val ic = currentInputConnection
        if (keyCode == 16) {
            shiftLock = !shiftLock
            if (shiftLock) {
                shift = true
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT))
            } else {
                shift = false
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT))
            }
            shiftKeyUpdateView()
        }

        if (keyCode == 17) {
            ctrlLock = !ctrlLock
            if (ctrlLock) {
                ctrl = true
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT))
            } else {
                ctrl = false
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
            }
            controlKeyUpdateView()
        }

        if (keyCode == 32) {
            longPressedSpaceButton = true
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        if (vibratorOn) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(vibrateLength.toLong())
        }
    }

    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection
        ic.commitText(text, 1)
        clearLongPressTimer()
    }

    override fun swipeLeft() {}

    override fun swipeRight() {}

    override fun swipeDown() {}

    override fun swipeUp() {}

    override fun onCreateInputView(): View? {
        if (mKeyboardUiFactory == null) {
            mKeyboardUiFactory = KeyboardUiFactory(this)
        }
        val factory = mKeyboardUiFactory!!
        val sharedPreferences = KeyboardPreferences(this)
        setNotification(sharedPreferences.getNotification())
        if (sharedPreferences.getCustomTheme()) {
            factory.theme = getDefaultThemeInfo()
            factory.theme.foregroundColor = sharedPreferences.getFgColor()
            factory.theme.backgroundColor = sharedPreferences.getBgColor()
        } else {
            factory.theme = setThemeByIndex(sharedPreferences, sharedPreferences.getThemeIndex())
        }
        // 키보드 기능 설정
        vibrateLength = sharedPreferences.getVibrateLength()
        vibratorOn = sharedPreferences.isVibrateEnabled()
        soundOn = sharedPreferences.isSoundEnabled()
        factory.theme.enablePreview = sharedPreferences.isPreviewEnabled()
        factory.theme.enableBorder = sharedPreferences.isBorderEnabled()
        factory.theme.fontSize = sharedPreferences.getFontSizeAsSp()
        val mSize = sharedPreferences.getPortraitSize()
        val sizeLandscape = sharedPreferences.getLandscapeSize()
        factory.theme.size = mSize / 100.0f
        factory.theme.sizeLandscape = sizeLandscape / 100.0f
        if (sharedPreferences.getNavBarDark()) {
            window.window?.navigationBarColor =
                ColorUtils.blendARGB(factory.theme.backgroundColor, Color.BLACK, 0.2f)
        } else if (sharedPreferences.getNavBar()) {
            window.window?.navigationBarColor = factory.theme.backgroundColor
        }
        // 키 레이아웃
        val mToprow = sharedPreferences.getTopRowActions()
        val mCustomSymbolsMain = sharedPreferences.getCustomSymbolsMain() ?: ""
        val mCustomSymbolsMain2 = sharedPreferences.getCustomSymbolsMain2() ?: ""
        val mCustomSymbolsSym = sharedPreferences.getCustomSymbolsSym() ?: ""
        val mCustomSymbolsSym2 = sharedPreferences.getCustomSymbolsSym2() ?: ""
        val mCustomSymbolsSym3 = sharedPreferences.getCustomSymbolsSym3() ?: ""
        val mCustomSymbolsSym4 = sharedPreferences.getCustomSymbolsSym4() ?: ""
        val mCustomSymbolsMainBottom = sharedPreferences.getCustomSymbolsMainBottom() ?: ""
        val mLayout = sharedPreferences.getLayoutIndex()

        // drawable 리소스를 얻기 위해 필요
        val definitions = Definitions(this)
        try {
            val builder = KeyboardLayoutBuilder(this)
            builder.setBox(Box.create(0f, 0f, 1f, 1f))

            if (mToprow) {
                definitions.addCopyPasteRow(builder)
            } else {
                definitions.addArrowsRow(builder)
            }

            if (mKeyboardState == R.integer.keyboard_sym) {
                if (mCustomSymbolsSym.isNotEmpty()) {
                    Definitions.addCustomRow(builder, mCustomSymbolsSym)
                }
                if (mCustomSymbolsSym2.isNotEmpty()) {
                    Definitions.addCustomRow(builder, mCustomSymbolsSym2)
                }
                if (mCustomSymbolsSym3.isNotEmpty()) {
                    Definitions.addCustomRow(builder, mCustomSymbolsSym3)
                }
                if (mCustomSymbolsSym4.isNotEmpty()) {
                    Definitions.addCustomRow(builder, mCustomSymbolsSym4)
                }
                if (mCustomSymbolsSym3.isEmpty() && mCustomSymbolsSym4.isEmpty()) {
                    definitions.addSymbolRows(builder)
                } else {
                    definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom)
                }
            } else if (mKeyboardState == R.integer.keyboard_normal) {
                if (koreanMode) {
                    // 한글 모드: 숫자열/기호열 + 두벌식 자판
                    if (mCustomSymbolsMain.isNotEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain)
                    }
                    if (mCustomSymbolsMain2.isNotEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain2)
                    }
                    Definitions.addKoreanRows(builder)
                } else {
                    if (mCustomSymbolsMain.isNotEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain)
                    }
                    if (mCustomSymbolsMain2.isNotEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain2)
                    }
                    when (mLayout) {
                        1 -> Definitions.addAzertyRows(builder)
                        2 -> Definitions.addDvorakRows(builder)
                        3 -> Definitions.addQwertzRows(builder)
                        else -> Definitions.addQwertyRows(builder)
                    }
                }
                definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom)
            } else if (mKeyboardState == R.integer.keyboard_clipboard) {
                definitions.addClipboardActions(builder)

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clipboard.hasPrimaryClip() &&
                    clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                ) {
                    val pr = clipboard.primaryClip
                    // 안드로이드는 클립보드에 한 항목만 허용
                    val s = pr!!.getItemAt(0).text.toString()
                    builder.newRow().addKey(s)
                } else {
                    builder.newRow().addKey("Nothing copied").withOutputText("")
                }
                builder.addKey(sharedPreferences.getPin1() ?: "")
                builder.newRow()
                    .addKey(sharedPreferences.getPin2() ?: "")
                    .addKey(sharedPreferences.getPin3() ?: "")
                builder.newRow()
                    .addKey(sharedPreferences.getPin4() ?: "")
                    .addKey(sharedPreferences.getPin5() ?: "")
                builder.newRow()
                    .addKey(sharedPreferences.getPin6() ?: "")
                    .addKey(sharedPreferences.getPin7() ?: "")
            }

            val keyboardLayout: Collection<Key> = builder.build()
            mCurrentKeyboardLayoutView = factory.createKeyboardView(this, keyboardLayout)
            return mCurrentKeyboardLayoutView
        } catch (e: KeyboardLayoutException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        ei.imeOptions = ei.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        super.onUpdateExtractingVisibility(ei)
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        hangulComposer.reset() // 새 입력 필드에서는 조합 상태 초기화
        setInputView(onCreateInputView())
        sEditorInfo = attribute
    }

    fun controlKeyUpdateView() {
        mCurrentKeyboardLayoutView?.applyCtrlModifier(ctrl)
    }

    fun shiftKeyUpdateView() {
        mCurrentKeyboardLayoutView?.applyShiftModifier(shift)
    }

    // 호환 자모(U+3131~U+3163) 범위인지
    private fun isHangulJamo(code: Int): Boolean = code in 0x3131..0x3163

    // 쉬프트가 눌렸을 때 된소리/이중모음으로 매핑
    private fun koreanShift(c: Char): Char = when (c) {
        'ㅂ' -> 'ㅃ'; 'ㅈ' -> 'ㅉ'; 'ㄷ' -> 'ㄸ'; 'ㄱ' -> 'ㄲ'; 'ㅅ' -> 'ㅆ'
        'ㅐ' -> 'ㅒ'; 'ㅔ' -> 'ㅖ'; else -> c
    }

    private fun clearLongPressTimer() {
        timerLongPress?.cancel()
        timerLongPress = null
    }

    private fun setThemeByIndex(keyboardPreferences: KeyboardPreferences, index: Int): ThemeInfo {
        var themeInfo = ThemeDefinitions.Default()
        when (index) {
            0 -> {
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> themeInfo = ThemeDefinitions.MaterialDark()
                    Configuration.UI_MODE_NIGHT_NO -> themeInfo = ThemeDefinitions.MaterialWhite()
                }
            }
            1 -> themeInfo = ThemeDefinitions.MaterialDark()
            2 -> themeInfo = ThemeDefinitions.MaterialWhite()
            3 -> themeInfo = ThemeDefinitions.PureBlack()
            4 -> themeInfo = ThemeDefinitions.White()
            5 -> themeInfo = ThemeDefinitions.Blue()
            6 -> themeInfo = ThemeDefinitions.Purple()
            else -> themeInfo = ThemeDefinitions.Default()
        }
        keyboardPreferences.setBgColor(themeInfo.backgroundColor.toString())
        keyboardPreferences.setFgColor(themeInfo.foregroundColor.toString())
        return themeInfo
    }

    private fun getDefaultThemeInfo(): ThemeInfo = ThemeDefinitions.Default()

    private fun createNotificationChannel() {
        // NotificationChannel은 API 26+ 에서만 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    // Hacker keyboard 소스 기반 코드
    @SuppressLint("MissingPermission")
    private fun setNotification(visible: Boolean) {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (visible && mNotificationReceiver == null) {
            val text: CharSequence = "Keyboard notification enabled."
            Log.i(TAG, "setNotification:$text")

            createNotificationChannel()
            val receiver = NotificationReceiver(this)
            mNotificationReceiver = receiver
            val pFilter = IntentFilter(NotificationReceiver.ACTION_SHOW)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(receiver, pFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, pFilter)
            }

            // 자기 앱으로 한정한 명시적 인텐트(암시적 인텐트가 아니므로 unsafe 플래그 불필요)
            val imeIntent = Intent(NotificationReceiver.ACTION_SHOW).setPackage(packageName)
            val imePendingIntent = PendingIntent.getBroadcast(
                applicationContext, 1, imeIntent, PendingIntent.FLAG_IMMUTABLE
            )

            // 액션 인텐트로 전달되는 문자열 키
            val keyTextReply = "key_text_reply"
            val remoteInput = RemoteInput.Builder(keyTextReply)
                .setLabel("Now click first icon")
                .build()

            // RemoteInput이 답장 텍스트를 채워야 하므로 MUTABLE. 명시적 인텐트라 안전함
            val replyPendingIntent = PendingIntent.getBroadcast(
                applicationContext, 2, imeIntent, PendingIntent.FLAG_MUTABLE
            )

            val action = NotificationCompat.Action.Builder(
                R.drawable.icon_large,
                getString(R.string.notification_action_open_keyboard_workaround),
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            val settingsIntent = Intent(this, MainActivity::class.java)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val settingsPendingIntent = PendingIntent.getActivity(
                this, 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE
            )
            val title = "Show Codeboard Keyboard"
            val body = "Select this to open the keyboard. Disable in settings. You may have to fix open the fix as a workaround for newer Android versions"

            val mBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_large)
                .setColor(0xff220044.toInt())
                .setAutoCancel(false)
                .setTicker(text)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(imePendingIntent)
                .setOngoing(true)
                .addAction(
                    R.drawable.icon_large,
                    getString(R.string.notification_action_open_keyboard),
                    imePendingIntent
                )
                .addAction(
                    R.drawable.icon_large,
                    getString(R.string.notification_action_settings),
                    settingsPendingIntent
                )
                .addAction(action)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(NOTIFICATION_ONGOING_ID, mBuilder.build())
        } else if (!visible && mNotificationReceiver != null) {
            mNotificationManager.cancel(NOTIFICATION_ONGOING_ID)
            unregisterReceiver(mNotificationReceiver)
            mNotificationReceiver = null
        }
    }

    override fun onCreateInputMethodInterface(): AbstractInputMethodImpl {
        return MyInputMethodImpl()
    }

    inner class MyInputMethodImpl : InputMethodImpl() {
        override fun attachToken(token: IBinder?) {
            super.attachToken(token)
            Log.i(TAG, "attachToken $token")
            if (mToken == null) {
                mToken = token
            }
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Codeboard"
        private const val NOTIFICATION_ONGOING_ID = 1001
        private const val TAG = "CodeBoardIME"
    }
}
