package com.gazlaws.codeboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.inputmethod.InputMethodManager

// 알림에서 키보드를 강제로 띄우라는 브로드캐스트를 받아 IME에 전달한다
class NotificationReceiver(private val mIME: CodeBoardIME) : BroadcastReceiver() {

    init {
        Log.i(javaClass.simpleName, "NotificationReceiver created, ime=$mIME")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(javaClass.simpleName, "NotificationReceiver.onReceive called, action=$action")

        if (action == ACTION_SHOW) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Log.i(javaClass.simpleName, "Version >= P" + Build.VERSION.SDK_INT)
                    mIME.requestShowSelf(InputMethodManager.SHOW_FORCED)
                } else {
                    Log.i(javaClass.simpleName, "Version < P" + Build.VERSION.SDK_INT)
                    imm.showSoftInputFromInputMethod(mIME.mToken, InputMethodManager.SHOW_FORCED)
                }
            }
            // 안드로이드 12에서는 비활성화된 것으로 보임
            // val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            // context.sendBroadcast(it)
        }
    }

    companion object {
        const val ACTION_SHOW = "com.gazlaws.codeboard.SHOW"
    }
}
