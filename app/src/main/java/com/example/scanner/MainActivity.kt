package com.example.scanner

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.databinding.ActivityMainBinding
import com.google.zxing.integration.android.IntentIntegrator

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import org.json.JSONObject
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "ScannerPrefs"
    private val API_URL_KEY = "api_url"
    private val DEFAULT_API_URL = "http://localhost"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved API URL or use default
        val savedUrl = sharedPreferences.getString(API_URL_KEY, DEFAULT_API_URL)
        binding.etApiUrl.setText(savedUrl)
        
        binding.btnScanner.setOnClickListener { 
            // Save API URL before scanning
            saveApiUrl()
            initScanner() 
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun saveApiUrl() {
        val apiUrl = binding.etApiUrl.text.toString().trim()
        if (apiUrl.isNotEmpty()) {
            sharedPreferences.edit().putString(API_URL_KEY, apiUrl).apply()
        }
    }

    private fun initScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(
            listOf(
                //Establecer formatos
                IntentIntegrator.PDF_417,
                IntentIntegrator.QR_CODE
            )
        )
        //Ejecutar
        integrator.initiateScan()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                //Procesar contenido
                //Toast.makeText(this, "${result.contents}", Toast.LENGTH_LONG).show()
                val contenido = result.contents
                enviarConsulta(contenido)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun enviarConsulta(codigo: String) {
        // Get API base URL from EditText
        val baseUrl = binding.etApiUrl.text.toString().trim()
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la URL de la API", Toast.LENGTH_LONG).show()
            return
        }
        
        // Construct full URL
        val url = "$baseUrl/api29-main/alumnos"
        
        // Create JSON object with token
        val jsonBody = JSONObject()
        jsonBody.put("token", codigo)
        
        Log.d("API Request", "URL: $url")
        Log.d("API Request", "Body: $jsonBody")
        
        val queue = Volley.newRequestQueue(this)
        
        // Create JsonObjectRequest with custom headers
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                Log.d("API Success", "Respuesta de la API: $response")
                Toast.makeText(this, "Respuesta recibida: ${response.toString().take(50)}...", Toast.LENGTH_LONG).show()
            },
            { error: VolleyError ->
                Log.e("API Error", "Error en la API: $error")
                Log.e("API Error Detail", "Respuesta de error: ${String(error.networkResponse?.data ?: ByteArray(0))}")
                Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_LONG).show()
            })
        {
            // Override getHeaders to add custom headers
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["X-Client-Id"] = "test123"
                headers["X-Secret-Key"] = "123456"
                return headers
            }
        }
        
        // Add request to queue
        queue.add(jsonObjectRequest)
    }
}
