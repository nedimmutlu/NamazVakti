package com.ezanvakti.app.worker

import android.content.Context
import androidx.work.*
import com.ezanvakti.app.data.repository.PrayerTimesRepository
import com.ezanvakti.app.location.LocationHelper
import com.ezanvakti.app.notification.PrayerAlarmManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager altyapısı ile günde bir kez arka planda çalışarak vakitleri ve alarmları senkronize eder.
 */
class DailyPrayerSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val locationHelper = LocationHelper(applicationContext)
            val coords = locationHelper.getLastSavedCoordinates()

            val repo = PrayerTimesRepository()
            val times = repo.getPrayerTimesForCoordinates(coords)

            val alarmManager = PrayerAlarmManager(applicationContext)
            alarmManager.scheduleAllPrayers(times)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyPrayerSyncWork"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<DailyPrayerSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
