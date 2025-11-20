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

        // Get data from intent
        val legajo = intent.getIntExtra("legajo", 0)
        val nombres = intent.getStringExtra("nombres") ?: ""
        val dni = intent.getStringExtra("dni") ?: ""
        val pertenece = intent.getBooleanExtra("pertenece", false)
        val fotoUrl = intent.getStringExtra("foto_url") ?: ""
        val baseUrl = intent.getStringExtra("base_url") ?: ""

        // Set data to views
        binding.tvLegajo.text = legajo.toString()
        binding.tvNombres.text = nombres
        binding.tvDni.text = dni
        binding.tvPertenece.text = if (pertenece) "Sí" else "No"

        // Change text color based on pertenece status
        if (pertenece) {
            binding.tvPertenece.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvPertenece.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // Load photo if URL is available
        if (fotoUrl.isNotEmpty() && baseUrl.isNotEmpty()) {
            loadPhoto(baseUrl, fotoUrl)
        } else {
            binding.tvLoadingFoto.visibility = View.GONE
        }

        // Back button
        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun loadPhoto(baseUrl: String, fotoUrl: String) {
        binding.tvLoadingFoto.visibility = View.VISIBLE
        
        // Construct full photo URL, handling potential double slashes
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val cleanFotoUrl = fotoUrl.trimStart('/')
        val fullPhotoUrl = "$cleanBaseUrl/$cleanFotoUrl"
        Log.d("ResultActivity", "Loading photo from: $fullPhotoUrl")

        val queue = Volley.newRequestQueue(this)

        // Create ImageRequest with custom headers
        val imageRequest = object : ImageRequest(
            fullPhotoUrl,
            { bitmap ->
                Log.d("ResultActivity", "Photo loaded successfully")
                binding.ivFoto.setImageBitmap(bitmap)
                binding.tvLoadingFoto.visibility = View.GONE
            },
            0, // maxWidth (0 = no limit)
            0, // maxHeight (0 = no limit)
            android.widget.ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            { error ->
                Log.e("ResultActivity", "Error loading photo: $error")
                binding.tvLoadingFoto.text = "Error al cargar foto"
                binding.tvLoadingFoto.visibility = View.VISIBLE
                Toast.makeText(this, "Error al cargar la foto", Toast.LENGTH_SHORT).show()
            }
        ) {
            // Override getHeaders to add custom headers
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Client-Id"] = "test123"
                headers["X-Secret-Key"] = "123456"
                return headers
            }
        }

        // Add request to queue
        queue.add(imageRequest)
    }
}
