package com.worldofcurrency.currencyconverter

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private var requestQueue: RequestQueue? = null
    private var spinner1: Spinner? = null
    private var spinner2: Spinner? = null
    private var input: EditText? = null
    private var output: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinner1 = findViewById(R.id.first_currency_spinner)
        spinner2 = findViewById(R.id.second_currency_spinner)
        input = findViewById(R.id.currency_input)
        output = findViewById(R.id.currency_output)
        requestQueue = Volley.newRequestQueue(this)


        val getCurrencyRequest = JsonObjectRequest(
            Request.Method.GET,
            "https://api.fastforex.io/currencies?api_key=demo",
            null,
            { jsonResponse ->

                val currencyList =
                    jsonResponse.getJSONObject("currencies").keys().asSequence().toMutableList()
                currencyList.remove("EUR")
                currencyList.add(0, "EUR")
                currencyList.remove("USD")
                currencyList.add(0, "USD")
                currencyList.remove("RUB")
                currencyList.add(0, "RUB")

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
            { })

        requestQueue?.add(getCurrencyRequest)

        val mediaPlayer = MediaPlayer.create(this, R.raw.button_click);
        val button = findViewById<Button>(R.id.button_convert)
        button.setOnClickListener {
            mediaPlayer.start()
            val amount = input?.text.toString().toDouble()
            val from = spinner1?.selectedItem.toString()
            val to = spinner2?.selectedItem.toString()
            val convertRequest = JsonObjectRequest(
                Request.Method.GET,
                "https://api.fastforex.io/convert?from=$from&to=$to&amount=$amount&api_key=demo",
                null,
                { jsonResponse ->
                    output?.text = jsonResponse.getJSONObject("result").getString(to)
                },
                { })
            requestQueue?.add(convertRequest)
        }
    }
}