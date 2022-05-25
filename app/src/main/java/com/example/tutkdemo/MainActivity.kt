package com.example.tutkdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tutkdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                scanUID()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            buildTimeLabel.text = BuildConfig.BUILD_TIME
            scanUIDButton.setOnClickListener { requestCameraPermissionAndStartScanner() }
            exoPlayerButton.setOnClickListener { openExoPlayer() }
            ijkPlayerButton.setOnClickListener { openIjkPlayer() }
            uidEditText.setText(
                getSharedPreferences(SETTINGS_FILE_NAME, MODE_PRIVATE).getString(
                    UID_KEY,
                    UID_DEFAULT_VALUE
                )
            )
            uidEditText.setOnEditorActionListener(TextView.OnEditorActionListener { textView, i, keyEvent ->
                val settings =
                    getSharedPreferences(SETTINGS_FILE_NAME, MODE_PRIVATE).edit()
                settings.putString(
                    UID_KEY,
                    textView.text.toString()
                )
                settings.apply()
                return@OnEditorActionListener false
            })
        }
    }

    private fun requestCameraPermissionAndStartScanner() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                scanUID()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {}
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            uidEditText.setText(
                getSharedPreferences(SETTINGS_FILE_NAME, MODE_PRIVATE).getString(
                    UID_KEY,
                    UID_DEFAULT_VALUE
                )
            )
        }
    }

    private fun scanUID() {
        startActivity(Intent(this, ScannerActivity::class.java))
    }

    private fun openExoPlayer() {
        initAVProvider()
        startActivity(Intent(this, ExoActivity::class.java))
    }

    private fun openIjkPlayer() {
        initAVProvider()
        startActivity(Intent(this, IjkActivity::class.java))
    }

    private fun initAVProvider() {
        val avProvider = (applicationContext as TUTKDemoApplication).avProvider
        avProvider.initAV(
            uid = binding.uidEditText.text.toString(),
            licenceKay = getString(R.string.licenseKey)
        )
    }

    private fun stopAVProvider(){
        val avProvider = (applicationContext as TUTKDemoApplication).avProvider
        avProvider.deinit()
    }

    override fun onDestroy() {
        stopAVProvider()
        super.onDestroy()
    }

    companion object {
        const val SETTINGS_FILE_NAME = "settings"
        const val UID_KEY = "UID"
        const val UID_DEFAULT_VALUE = ""
        const val FULLSCREEN_KEY = "fullscreen"
    }

}