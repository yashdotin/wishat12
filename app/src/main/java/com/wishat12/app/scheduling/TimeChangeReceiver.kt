package com.wishat12.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wallClockOrTimezoneChanged = true
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ReminderProcessor.recover(context.applicationContext, wallClockOrTimezoneChanged) }
            finally { pending.finish() }
        }
    }
}
