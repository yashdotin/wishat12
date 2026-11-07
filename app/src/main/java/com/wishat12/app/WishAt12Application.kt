package com.wishat12.app

import android.app.Application
import com.wishat12.app.data.database.AppDatabase
import com.wishat12.app.data.entity.MessageTemplateEntity
import com.wishat12.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WishAt12Application : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(this@WishAt12Application).templateDao()
            if (dao.getAll().isEmpty()) {
                dao.insertAll(
                    listOf(
                        MessageTemplateEntity(title = "Warm birthday", category = "Birthday", content = "Happy Birthday, {name}! Wishing you a year full of happiness, good health and success. Have an amazing day! 🎂"),
                        MessageTemplateEntity(title = "Simple and sweet", category = "Birthday", content = "Happy Birthday, {name}! Hope your day is as special as you are. Keep smiling and keep shining! ❤️"),
                        MessageTemplateEntity(title = "Anniversary", category = "Anniversary", content = "Happy Anniversary, {name}! Wishing you both many more years of love, laughter and beautiful memories. ❤️"),
                        MessageTemplateEntity(title = "Festival greeting", category = "Festival", content = "Wishing you and your family happiness, peace and prosperity. Have a wonderful celebration! ✨"),
                        MessageTemplateEntity(title = "Professional", category = "Professional", content = "Wishing you continued success and all the very best. Have a wonderful day!")
                    )
                )
            }
        }
    }
}
