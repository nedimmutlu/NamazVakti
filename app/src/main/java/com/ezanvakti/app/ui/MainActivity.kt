package com.ezanvakti.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.firstnamazvakti.R
import com.ezanvakti.app.data.model.DailyContent
import com.ezanvakti.app.data.model.Hadith
import com.ezanvakti.app.data.model.PrayerTimes
import com.ezanvakti.app.data.repository.DailyContentRepository
import com.ezanvakti.app.data.repository.PrayerTimesRepository
import com.ezanvakti.app.location.Coordinates
import com.ezanvakti.app.location.LocationHelper
import com.ezanvakti.app.notification.PrayerAlarmManager
import com.ezanvakti.app.worker.DailyPrayerSyncWorker
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var prayerTimesRepo: PrayerTimesRepository
    private lateinit var dailyContentRepo: DailyContentRepository
    private lateinit var alarmManager: PrayerAlarmManager

    private lateinit var tvLocationTitle: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvFajr: TextView
    private lateinit var tvDhuhr: TextView
    private lateinit var tvAsr: TextView
    private lateinit var tvMaghrib: TextView
    private lateinit var tvIsha: TextView
    private lateinit var tvContentHeader: TextView
    private lateinit var tvContentBody: TextView
    private lateinit var tvContentSource: TextView
    private lateinit var btnRefreshLocation: Button
    private lateinit var btnRefreshContent: Button

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            loadDynamicLocationAndTimes()
        } else {
            Toast.makeText(this, "Konum izni verilmedi; varsayılan (Malatya) baz alınıyor.", Toast.LENGTH_LONG).show()
            loadFallbackLocationAndTimes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initServices()
        checkAndRequestPermissions()
        displayRandomDailyContent()
    }

    private fun initViews() {
        tvLocationTitle = findViewById(R.id.tvLocationTitle)
        tvCoordinates = findViewById(R.id.tvCoordinates)
        tvFajr = findViewById(R.id.tvFajr)
        tvDhuhr = findViewById(R.id.tvDhuhr)
        tvAsr = findViewById(R.id.tvAsr)
        tvMaghrib = findViewById(R.id.tvMaghrib)
        tvIsha = findViewById(R.id.tvIsha)
        tvContentHeader = findViewById(R.id.tvContentHeader)
        tvContentBody = findViewById(R.id.tvContentBody)
        tvContentSource = findViewById(R.id.tvContentSource)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        btnRefreshContent = findViewById(R.id.btnRefreshContent)

        btnRefreshLocation.setOnClickListener {
            loadDynamicLocationAndTimes()
        }

        btnRefreshContent.setOnClickListener {
            displayRandomDailyContent()
        }
    }

    private fun initServices() {
        locationHelper = LocationHelper(this)
        prayerTimesRepo = PrayerTimesRepository()
        dailyContentRepo = DailyContentRepository.getInstance(this)
        alarmManager = PrayerAlarmManager(this)

        DailyPrayerSyncWorker.enqueuePeriodicSync(this)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            loadDynamicLocationAndTimes()
        }
    }

    private fun loadDynamicLocationAndTimes() {
        tvLocationTitle.text = "📍 Konum tespiti yapılıyor..."
        locationHelper.fetchCurrentLocation { coords ->
            updateUIWithLocationAndTimes(coords)
        }
    }

    private fun loadFallbackLocationAndTimes() {
        val fallback = locationHelper.getLastSavedCoordinates()
        updateUIWithLocationAndTimes(fallback)
    }

    private fun updateUIWithLocationAndTimes(coords: Coordinates) {
        tvLocationTitle.text = "📍 ${coords.cityName}"
        tvCoordinates.text = String.format(
            Locale.getDefault(),
            "Enlem: %.4f | Boylam: %.4f",
            coords.latitude,
            coords.longitude
        )

        val times: PrayerTimes = prayerTimesRepo.getPrayerTimesForCoordinates(coords)

        tvFajr.text = times.fajr
        tvDhuhr.text = times.dhuhr
        tvAsr.text = times.asr
        tvMaghrib.text = times.maghrib
        tvIsha.text = times.isha

        alarmManager.scheduleAllPrayers(times)
    }

    private fun displayRandomDailyContent() {
        val content: DailyContent = dailyContentRepo.getRandomContentForPrayer(null)

        if (content is Hadith) {
            tvContentHeader.text = "Günün Hadis-i Şerifi"
        } else {
            tvContentHeader.text = "Günün Ayet-i Kerimesi"
        }

        tvContentBody.text = "${content.text}"
        tvContentSource.text = content.getFullSourceText()
    }
}