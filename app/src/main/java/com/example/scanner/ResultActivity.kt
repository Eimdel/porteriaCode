package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scanner.databinding.ActivityResultBinding
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del Intent
        val legajo = intent.getIntExtra("legajo", 0)
        val nombres = intent.getStringExtra("nombres") ?: ""
        val dni = intent.getStringExtra("dni") ?: ""
        val pertenece = intent.getBooleanExtra("pertenece", false)
        val fotoUrl = intent.getStringExtra("foto_url") ?: ""
        val baseUrl = intent.getStringExtra("base_url") ?: ""

        // Mostrar datos en la interfaz
        binding.tvLegajo.text = legajo.toString()
        binding.tvNombres.text = nombres
        binding.tvDni.text = dni
        binding.tvPertenece.text = if (pertenece) "Sí" else "No"

        // Cambiar el color del texto segun si se encontro (verde = verdadero, rojo = falso)
        if (pertenece) {
            binding.tvPertenece.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvPertenece.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // Cargar foto
        if (fotoUrl.isNotEmpty() && baseUrl.isNotEmpty()) {
            loadPhoto(baseUrl, fotoUrl)
        } else {
            binding.tvLoadingFoto.visibility = View.GONE
        }

        // Boton para volver
        binding.btnVolver.setOnClickListener {
            finish() // Cerrar actividad y volver al MainActivity
        }
    }

    private fun loadPhoto(baseUrl: String, fotoUrl: String) {
        binding.tvLoadingFoto.visibility = View.VISIBLE
        
        // Combinar la URL del servidor con la ruta de la foto
        val fullPhotoUrl = "$baseUrl/$fotoUrl"
        Log.d("ResultActivity", "Loading photo from: $fullPhotoUrl")

        // Inicializa la cola de peticiones Volley
        val queue = Volley.newRequestQueue(this)

        // Creacion de ImageRequest
        val imageRequest = object : ImageRequest(
            fullPhotoUrl, //URL de la imagen
            { bitmap ->
                // Exito
                Log.d("ResultActivity", "Photo loaded successfully")
                binding.ivFoto.setImageBitmap(bitmap)
                binding.tvLoadingFoto.visibility = View.GONE
            },
            0, // maxWidth (escala completa)
            0, // maxHeight (escala completa)
            android.widget.ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            { error ->
                // En caso de error
                Log.e("ResultActivity", "Error loading photo: $error")
                binding.tvLoadingFoto.text = "Error al cargar foto"
                binding.tvLoadingFoto.visibility = View.VISIBLE
                Toast.makeText(this, "Error al cargar la foto", Toast.LENGTH_SHORT).show()
            }
        ) {
            // Sobreescribir getHeaders para las claves de seguridad
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                // Autorizacion
                headers["X-Client-Id"] = "test123"
                headers["X-Secret-Key"] = "123456"
                return headers
            }
        }

        // Agregar peticion a la cola
        queue.add(imageRequest)
    }
}
