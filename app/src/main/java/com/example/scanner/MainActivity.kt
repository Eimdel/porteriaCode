package com.example.scanner

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.btnScanner.setOnClickListener { initScanner() }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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

    private fun enviarConsulta(codigo: String) { //Declarar función y definir consulta como string
        val url = "https://192.168.0.40" //DEFINIR IP

        val queue = Volley.newRequestQueue(this) //Petición

        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Method.POST, url, //Metodo a usar Post
            { response -> //Si la solicitud es respondida:

                Log.d("API Success", "Respuesta de la API: $response") //Mostrar respuesta

                //Mensaje emergente
                Toast.makeText(this, "Respuesta recibida: ${response.take(50)}...", Toast.LENGTH_LONG).show()
            },
            { error: VolleyError ->
                // Manejar errores de red o del servidor
                Log.e("API Error", "Error en la API: $error")
                Log.e("API Error Detail", "Respuesta de error: ${String(error.networkResponse?.data ?: ByteArray(0))}")
                Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_LONG).show()
            })
        {
            //Sobreescribir el metodo para incluir parametros para el servidor
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["pdf417_code"] = codigo
                return params
            }

        }
        //Agregar solicitud a la cola de peticiones
        queue.add(stringRequest)
    }
}
