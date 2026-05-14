package com.nvr.yoosee.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nvr.yoosee.R
import com.nvr.yoosee.databinding.ActivityMainBinding
import com.nvr.yoosee.service.RecordingService

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var recordingService: RecordingService? = null
    var serviceBound = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            recordingService = (b as RecordingService.LocalBinder).getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(n: ComponentName) { serviceBound = false }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPerms()
        loadFragment(LiveFragment())
        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.nav_live -> { loadFragment(LiveFragment()); true }
                R.id.nav_recordings -> { loadFragment(RecordingsFragment()); true }
                R.id.nav_settings -> { loadFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    fun loadFragment(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, f).commit()
    }

    private fun requestPerms() {
        val perms = mutableListOf(Manifest.permission.INTERNET)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty())
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, RecordingService::class.java), conn, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) { unbindService(conn); serviceBound = false }
    }

    fun getService() = recordingService
}
