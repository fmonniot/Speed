package eu.monniot.speed

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import eu.monniot.speed.service.RaceRecordingService

class RaceLoggerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Race Recording"
        val descriptionText = "Notifications for active race data recording"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(RaceRecordingService.CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
