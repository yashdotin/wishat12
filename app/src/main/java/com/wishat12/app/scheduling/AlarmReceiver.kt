package com.wishat12.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (id <= 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ReminderProcessor.process(context.applicationContext, id) }
            finally { pending.finish() }
        }
    }
}
