package com.beacontap.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var connectivityManager: ConnectivityManager

    private var boundNetwork: Network? = null

    private val locationPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        val retryButton = findViewById<View>(R.id.retryButton)

        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        setupWebView()

        retryButton.setOnClickListener {
            retryButton.visibility = View.GONE
            connectToDevice()
        }

        connectToDevice()
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                statusText.text = getString(R.string.status_connected)
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                showManualConnectOption()
            }
        }
    }

    /**
     * Entry point: figure out the right way to join the ESP32's WiFi AP
     * for this Android version, then load the dashboard once connected.
     */
    private fun connectToDevice() {
        statusText.text = getString(R.string.status_connecting)
        progressBar.visibility = View.VISIBLE
        findViewById<View>(R.id.retryButton).visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectViaNetworkSpecifier()
        } else {
            // Below Android 10: programmatic WiFi join is unreliable/deprecated.
            // Ask the user to connect manually, then load the dashboard.
            promptManualConnection()
        }
    }

    /** Android 10+ (API 29+): request the specific local WiFi network directly. */
    private fun connectViaNetworkSpecifier() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
            return
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(getString(R.string.device_ssid))
            .setWpa2Passphrase(getString(R.string.device_password))
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                boundNetwork = network
                connectivityManager.bindProcessToNetwork(network)
                runOnUiThread { webView.loadUrl(getString(R.string.device_url)) }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                runOnUiThread { showManualConnectOption() }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                connectivityManager.bindProcessToNetwork(null)
            }
        }

        connectivityManager.requestNetwork(request, callback)
    }

    /** Below Android 10: point the user at WiFi settings, then load once they're back. */
    private fun promptManualConnection() {
        Toast.makeText(
            this,
            "Connect to WiFi network \"${getString(R.string.device_ssid)}\" then reopen the app",
            Toast.LENGTH_LONG
        ).show()
        showManualConnectOption()
    }

    private fun showManualConnectOption() {
        progressBar.visibility = View.GONE
        statusText.text = getString(R.string.status_failed)
        val retryButton = findViewById<View>(R.id.retryButton)
        retryButton.visibility = View.VISIBLE
        retryButton.setOnClickListener {
            val ssid = getString(R.string.device_ssid)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectToDevice()
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                Toast.makeText(this, "Connect to $ssid, then come back", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            connectToDevice()
        } else {
            Toast.makeText(
                this,
                "Location permission is needed to auto-connect to the device WiFi",
                Toast.LENGTH_LONG
            ).show()
            showManualConnectOption()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (boundNetwork != null) {
            connectivityManager.bindProcessToNetwork(null)
        }
    }
}
