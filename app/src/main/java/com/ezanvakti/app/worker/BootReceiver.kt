package com.ezanvakti.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ezanvakti.app.data.repository.PrayerTimesRepository
import com.ezanvakti.app.location.LocationHelper
import com.ezanvakti.app.notification.PrayerAlarmManager

/**
 * Problem 5 Çözümü:
 * Cihaz yeniden başladığında (Reboot) silinen alarmları tekrar kurar.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            val locationHelper = LocationHelper(context)
            val coords = locationHelper.getLastSavedCoordinates()
            val prayerTimesRepo = PrayerTimesRepository()
            val times = prayerTimesRepo.getPrayerTimesForCoordinates(coords)

            val alarmManager = PrayerAlarmManager(context)
            alarmManager.scheduleAllPrayers(times)
        }
    }
}
