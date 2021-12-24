package com.worldofcurrency.currencyconverter

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.util.*
import android.widget.ArrayAdapter


class MainActivity : AppCompatActivity() {

    companion object {
        private const val NET_PERMISSION_CODE = 100
        private const val LOCATION_PERMISSION_CODE = 101
        private const val FILES_PERMISSION_CODE = 102
    }

    private var requestQueue: RequestQueue? = null
    private var exchangeDbHelper: ExchangeDbHelper? = null

    private var spinner1: Spinner? = null
    private var spinner2: Spinner? = null
    private var input: EditText? = null
    private var output: TextView? = null
    private var exchangesView: ListView? = null
    private var adapterForExchangesView: ArrayAdapter<ExchangeModel>? = null
    private var listOfExchanges = mutableListOf<ExchangeModel>()

    private var currentCurrency: String? = null
    private var listOfAvailableCurrencies = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission(arrayOf(Manifest.permission.INTERNET), NET_PERMISSION_CODE)
        checkPermission(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ), FILES_PERMISSION_CODE
        )
        initial()
        setCovertButtonListener()
    }

    override fun onDestroy() {
        exchangeDbHelper?.close()
        super.onDestroy()
    }

    private fun initial(){
        requestQueue = Volley.newRequestQueue(this)
        exchangeDbHelper = ExchangeDbHelper(this)
        spinner1 = findViewById(R.id.first_currency_spinner)
        spinner2 = findViewById(R.id.second_currency_spinner)
        input = findViewById(R.id.currency_input)
        output = findViewById(R.id.currency_output)
        exchangesView = findViewById(R.id.exchangesView)

        determineCurrentCurrency()
        determineListOfCurrencies()
        loadAllExchanges()
    }

    private fun loadAllExchanges(){
        run {
            val allEntries = exchangeDbHelper?.getAllEntries()
            if (allEntries != null) {
                listOfExchanges = allEntries.asReversed().toMutableList()
            }
            adapterForExchangesView =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, listOfExchanges)
            exchangesView?.setAdapter(adapterForExchangesView)
        }
    }

    private fun setCovertButtonListener(){
        val mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
        val button = findViewById<Button>(R.id.button_convert)
        button.setOnClickListener {
            mediaPlayer.start()
            val amount = input?.text.toString().toDouble()
            val from = spinner1?.selectedItem.toString()
            val to = spinner2?.selectedItem.toString()
            val convertRequest = JsonObjectRequest(
                Request.Method.GET,
                getString(
                    R.string.api_fastforex,
                    "convert",
                    "from=$from&to=$to&amount=$amount",
                    "demo"
                ),
                null,
                { jsonResponse ->
                    if (!jsonResponse.has("result")) {
                        var msg = "Cannot exchange currencies."
                        if (jsonResponse.has("Error")) {
                            msg += " Error: " + jsonResponse.getString("Error")
                        }
                        showToast(msg)
                        return@JsonObjectRequest
                    }

                    val result = jsonResponse.getJSONObject("result").getDouble(to)
                    output?.text = result.toString()
                    val newExchange = ExchangeModel(from, to, amount, result, Date())
                    exchangeDbHelper?.addEntry(newExchange)
                    listOfExchanges.add(0, newExchange)
                    adapterForExchangesView?.notifyDataSetChanged()
                },
                {
                    showToast(it.localizedMessage ?: "Error: $it")
                })
            requestQueue?.add(convertRequest)
        }
    }

    private fun checkPermission(permissions: Array<String>, requestCode: Int) {
        var permissionsGranted = false
        for (permission in permissions) {
            permissionsGranted = permissionsGranted || ActivityCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionsGranted) {
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
                }
            }
            FILES_PERMISSION_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showToast("Without permission for work with files this application cannot save exchange history")
                }
            }
        }

    }

    private fun determineCurrentCurrency() {
        //permission request if necessary
        checkPermission(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
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
            determineCurrentCurrencyByResources()
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
                    assignListOfCurrencies()
                    break
                }
            } catch (e: Exception) {
            }
        }

        if (currentCurrency.isNullOrEmpty()) {
            determineCurrentCurrencyByResources()
        }

    }

    private fun determineCurrentCurrencyByResources(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales = resources.configuration.locales
            for(i in 0 until locales.size()){
                val locale = locales.get(i)
                val currency = Currency.getInstance(locale)
                currentCurrency = currency.currencyCode
                assignListOfCurrencies()
            }
        }
    }

    private fun assignListOfCurrencies(){
        if (listOfAvailableCurrencies.isNotEmpty() && currentCurrency != null){
            if (currentCurrency != "USD" && currentCurrency != "EUR") {
                listOfAvailableCurrencies.remove(currentCurrency)
                listOfAvailableCurrencies.add(0, currentCurrency!!)
            } else if (currentCurrency == "EUR") {
                listOfAvailableCurrencies[0] = "EUR"
                listOfAvailableCurrencies[1] = "USD"
            }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOfAvailableCurrencies.toTypedArray()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1?.adapter = adapter
        spinner1?.setSelection(1)
        spinner2?.adapter = adapter
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

                listOfAvailableCurrencies =
                    jsonResponse.getJSONObject("currencies").keys().asSequence().toMutableList()
                listOfAvailableCurrencies.remove("EUR")
                listOfAvailableCurrencies.add(0, "EUR")
                listOfAvailableCurrencies.remove("USD")
                listOfAvailableCurrencies.add(0, "USD")
                assignListOfCurrencies()
            },
            {
                showToast(it.localizedMessage ?: "Error: $it")
            })

        requestQueue?.add(getCurrencyRequest)
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}