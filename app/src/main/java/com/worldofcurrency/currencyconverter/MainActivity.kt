package com.worldofcurrency.currencyconverter

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val NET_PERMISSION_CODE = 100
        private const val LOCATION_PERMISSION_CODE = 101
    }

    private var requestQueue: RequestQueue? = null
    private var spinner1: Spinner? = null
    private var spinner2: Spinner? = null
    private var input: EditText? = null
    private var output: TextView? = null
    private var currentCurrency: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestQueue = Volley.newRequestQueue(this)
        spinner1 = findViewById(R.id.first_currency_spinner)
        spinner2 = findViewById(R.id.second_currency_spinner)
        input = findViewById(R.id.currency_input)
        output = findViewById(R.id.currency_output)

        checkPermission(arrayOf(Manifest.permission.INTERNET), NET_PERMISSION_CODE)
        determineCurrentCurrency()
        determineListOfCurrencies()

        val mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
        val button = findViewById<Button>(R.id.button_convert)
        button.setOnClickListener {
            mediaPlayer.start()
            val amount = input?.text.toString().toDouble()
            val from = spinner1?.selectedItem.toString()
            val to = spinner2?.selectedItem.toString()
            val convertRequest = JsonObjectRequest(
                Request.Method.GET,
                getString(R.string.api_fastforex, "convert", "from=$from&to=$to&amount=$amount", "demo"),
                null,
                { jsonResponse ->
                    output?.text = jsonResponse.getJSONObject("result").getString(to)
                },
                { })
            requestQueue?.add(convertRequest)
        }
    }

    private fun checkPermission(permissions: Array<String>, requestCode: Int) {
        var allPermissionsGranted = true
        for (permission in permissions) {
            allPermissionsGranted = allPermissionsGranted && ActivityCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NET_PERMISSION_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showToast("Without net permission this application cannot receive exchange rates")
                }
            }
            LOCATION_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    || (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                ) {
                    determineCurrentCurrency()
                } else {
                    showToast("Without location permission this application cannot determine your current currency")
                }
            }
        }

    }

    private fun determineCurrentCurrency() {
        //permission request if necessary
        checkPermission(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_CODE
        )

        //check permission grant
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationManger = getSystemService(LOCATION_SERVICE) as LocationManager
        val geocoder = Geocoder(applicationContext)
        for (provider in locationManger.allProviders) {
            val location = locationManger.getLastKnownLocation(provider)
            try {
                val addresses: List<Address>? =
                    geocoder.getFromLocation(location!!.latitude, location.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val currency = Currency.getInstance(addresses[0].locale)
                    currentCurrency = currency.currencyCode
                    break
                }
            } catch (e: Exception) {
            }
        }

    }

    private fun determineListOfCurrencies() {
        val getCurrencyRequest = JsonObjectRequest(
            Request.Method.GET,
            getString(R.string.api_fastforex, "currencies", "", "demo"),
            null,
            { jsonResponse ->

                if (!jsonResponse.has("currencies")) {
                    var msg = "Cannot receive list of currencies."
                    if (jsonResponse.has("Error")) {
                        msg += "Error: " + jsonResponse.getString("Error")
                    }
                    showToast(msg)
                    return@JsonObjectRequest
                }

                val currencyList =
                    jsonResponse.getJSONObject("currencies").keys().asSequence().toMutableList()
                currencyList.remove("EUR")
                currencyList.add(0, "EUR")
                currencyList.remove("USD")
                currencyList.add(0, "USD")
                if (currentCurrency != null) {
                    if (currentCurrency != "USD" && currentCurrency != "EUR") {
                        currencyList.remove(currentCurrency)
                        currencyList.add(0, currentCurrency)
                    } else if (currentCurrency == "EUR") {
                        currencyList[0] = "EUR"
                        currencyList[1] = "USD"
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    currencyList.toTypedArray()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner1?.adapter = adapter
                spinner1?.setSelection(1)
                spinner2?.adapter = adapter
            },
            {
                showToast(it.localizedMessage ?: "Error: $it")
            })

        requestQueue?.add(getCurrencyRequest)
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}