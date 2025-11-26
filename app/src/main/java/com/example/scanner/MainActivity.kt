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

// Para el lector
import com.google.zxing.integration.android.IntentIntegrator

// Para conectar con la API de php
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

// Procesar datos
import org.json.JSONObject
import android.util.Log

class MainActivity : AppCompatActivity() {

    // lateinit declara propiedades sin datos, pero obtienen valor antes de ejecutarse
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences // Guardar URL de la API

    private val PREFS_NAME = "ScannerPrefs" // Nombre del archivo
    private val API_URL_KEY = "api_url" // CLave para guardar URL
    private val DEFAULT_API_URL = "http://localhost" // URL predeterminada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de la interfaz
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        
        // Ejecutar SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Cargar la URL guardada de la API y establecerla en un EditText
        val savedUrl = sharedPreferences.getString(API_URL_KEY, DEFAULT_API_URL)
        binding.etApiUrl.setText(savedUrl)

        // Establecer listener para el boton del escaneo
        binding.btnScanner.setOnClickListener { 
            // Guarda URL antes de escanear
            saveApiUrl()
            // Inicio de escaner
            initScanner() 
        }

        // Que no se oculte o se solape el contenido de las vistas
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Guardar la URL de la API en SharedPreferences
    private fun saveApiUrl() {
        val apiUrl = binding.etApiUrl.text.toString().trim()
        if (apiUrl.isNotEmpty()) {
            sharedPreferences.edit().putString(API_URL_KEY, apiUrl).apply()
        }
    }

    // Inicializa el escaneo con IntentIntegrator (clase de utilidad) de zxing
    private fun initScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(
            listOf(
                //Establecer formatos de codigos de barras
                IntentIntegrator.PDF_417,
                IntentIntegrator.QR_CODE
            )
        )
        //Ejecutar el escaner
        integrator.initiateScan()
    }

    // Ejecucion luego de dar por terminado el escaneo, verifica los datos
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        // Identificar resultados e identificar estructura
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // En caso de cancelar escaneo
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                // Escaneo exitoso
                val contenido = result.contents
                // Enviar codigo a la API
                enviarConsulta(contenido)
            }
        } else {
            // Manejar resultados de otras actividades
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    // Enviar token a la API para buscar al individuo
    private fun enviarConsulta(codigo: String) {
        // Obtener URL desde el EditText
        val baseUrl = binding.etApiUrl.text.toString().trim()
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la URL de la API", Toast.LENGTH_LONG).show()
            return // En caso de no haber URL, detener ejecucion
        }
        
        // Construir la URL
        val url = "$baseUrl/api29-main/alumnos"
        
        // Crear la peticion POST con el token
        val jsonBody = JSONObject()
        jsonBody.put("token", codigo)
        
        Log.d("API Request", "URL: $url")
        Log.d("API Request", "Body: $jsonBody")
        
        val queue = Volley.newRequestQueue(this)
        
        // Crear la peticion POST de JSON
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                // Recibir respuesta del servidor
                Log.d("API Success", "Respuesta de la API: $response")
                procesarRespuesta(response, baseUrl)
            },
            { error: VolleyError ->
                //  Error al conectar
                Log.e("API Error", "Error en la API: $error")
                // Intenta mostrar la respuesta de error de la red si está disponible
                Log.e("API Error Detail", "Respuesta de error: ${String(error.networkResponse?.data ?: ByteArray(0))}")
                Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_LONG).show()
            })
        {
            // Sobreescribir getHeaders para agregarles encabezados HTTP
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["X-Client-Id"] = "test123"
                headers["X-Secret-Key"] = "123456"
                return headers
            }
        }
        
        // Incorporar peticion a la cola para ejecucion
        queue.add(jsonObjectRequest)
    }

    // Interpreta la respuesta del JSON de la API y ejecuta un ResultActivity con los datos3
    private fun procesarRespuesta(response: JSONObject, baseUrl: String) {
        try {
            val status = response.getInt("status")
            
            if (status == 200) {
                // Extraccion de datos anidados
                val detalle = response.getJSONObject("detalle")
                val alumno = detalle.getJSONObject("alumno")
                
                val legajo = alumno.getInt("legajo")
                val nombres = alumno.getString("nombres")
                val dni = alumno.getString("dni")
                val pertenece = alumno.getBoolean("pertenece")
                val fotoUrl = alumno.optString("foto", "")
                
                // Adjuntar todos los datos de los alumnos
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("legajo", legajo)
                intent.putExtra("nombres", nombres)
                intent.putExtra("dni", dni)
                intent.putExtra("pertenece", pertenece)
                intent.putExtra("foto_url", fotoUrl)
                intent.putExtra("base_url", baseUrl)

                startActivity(intent) // Iniciar actividad
            } else {
                // Manejo de estados de errores devueltos por la API
                Toast.makeText(this, "Error: Estado $status", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // Captura errores si la estructura del JSON es inesperada
            Log.e("API Parse", "Error al parsear respuesta: $e")
            Toast.makeText(this, "Error al procesar respuesta", Toast.LENGTH_LONG).show()
        }
    }
}
