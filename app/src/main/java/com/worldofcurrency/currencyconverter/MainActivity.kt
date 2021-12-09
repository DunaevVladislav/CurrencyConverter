package com.worldofcurrency.currencyconverter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.text)
        requestQueue = Volley.newRequestQueue(this)
        val url = "https://api.fastforex.io/currencies?api_key=demo"

        val stringRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { jsonResponse ->
                val currenciesMap = jsonResponse.getJSONObject("currencies")
                var text = ""
                for (key in currenciesMap.keys()) {
                    val value = currenciesMap.get(key).toString()
                    text += "$key -> $value\n"
                }
                textView.text = text
            },
            { textView.text = "That didn't work!" })

        requestQueue?.add(stringRequest)
    }
}