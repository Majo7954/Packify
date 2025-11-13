package com.ucb.deliveryapp.ui.screens.packages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
// 1. CORRECCIÓN: Cambiado a AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.db.AppDatabase
import com.ucb.deliveryapp.repository.PackageRepository
import com.ucb.deliveryapp.ui.adapter.PackageAdapter
import kotlinx.coroutines.launch

// 1. CORRECCIÓN: Heredar de AppCompatActivity para tener soporte para la ActionBar
class PackageListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddPackage: FloatingActionButton
    private lateinit var packageAdapter: PackageAdapter
    private lateinit var packageRepository: PackageRepository

    // 2. RECOMENDACIÓN: Usar el nuevo Activity Result API
    private val createPackageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Si el paquete fue creado exitosamente, recargamos la lista
            loadPackages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_package_list)

        // Ahora estas líneas funcionarán correctamente
        supportActionBar?.title = "Mis Paquetes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // La inicialización del repositorio está bien aquí, pero idealmente debería
        // estar en un ViewModel para separar la lógica de la UI.
        val database = AppDatabase.getDatabase(this)
        packageRepository = PackageRepository(database.packageDao())

        setupRecyclerView()
        setupFab()

        loadPackages()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewPackages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        packageAdapter = PackageAdapter(emptyList()) { pkg ->
            // Al hacer clic en un paquete, abrimos la pantalla de detalles
            val intent = Intent(this, PackageDetailActivity::class.java).apply {
                putExtra("PACKAGE_ID", pkg.id)
            }
            startActivity(intent)
        }
        recyclerView.adapter = packageAdapter
    }

    private fun setupFab() {
        fabAddPackage = findViewById(R.id.fabAddPackage)
        fabAddPackage.setOnClickListener {
            // 2. RECOMENDACIÓN: Lanzar la actividad con el nuevo API
            val intent = Intent(this, CreatePackageActivity::class.java)
            createPackageLauncher.launch(intent)
        }
    }

    private fun loadPackages() {
        lifecycleScope.launch {
            try {
                // TODO: Reemplazar el userId '1' por el ID del usuario que ha iniciado sesión
                val userId = 1
                val packages = packageRepository.getUserPackages(userId)
                packageAdapter.updatePackages(packages)

                if (packages.isEmpty()) {
                    Toast.makeText(
                        this@PackageListActivity,
                        "No tienes paquetes registrados. ¡Añade uno nuevo!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Es buena práctica registrar el error para depuración
                // Log.e("PackageListActivity", "Error al cargar paquetes", e)
                Toast.makeText(
                    this@PackageListActivity,
                    "Error al cargar los paquetes: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Se elimina onResume() para evitar recargas múltiples. La carga se maneja
    // en onCreate y cuando vuelve de CreatePackageActivity.

    // 3. CORRECCIÓN: onSupportNavigateUp se usa para manejar el botón de "atrás" en la ActionBar
    override fun onSupportNavigateUp(): Boolean {
        // Cierra la actividad actual y vuelve a la pantalla anterior
        finish()
        return true
    }
}
