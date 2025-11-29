package com.ucb.deliveryapp.ui.screens.packages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.data.repository.PackageRepositoryImpl
import com.ucb.deliveryapp.ui.adapter.PackageAdapter
import com.ucb.deliveryapp.util.Result
import kotlinx.coroutines.launch

class PackageListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddPackage: FloatingActionButton
    private lateinit var packageAdapter: PackageAdapter
    private lateinit var packageRepository: PackageRepositoryImpl

    // LoginDataStore para obtener userId
    private lateinit var loginDataStore: LoginDataStore

    private val createPackageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadPackages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_package_list)

        supportActionBar?.title = "Mis Paquetes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // CORREGIDO: Usar Firebase sin Room
        packageRepository = PackageRepositoryImpl()

        // Inicializar LoginDataStore
        loginDataStore = LoginDataStore(this)

        setupRecyclerView()
        setupFab()

        loadPackages()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewPackages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        packageAdapter = PackageAdapter(emptyList()) { pkg ->
            val intent = Intent(this, PackageDetailActivity::class.java).apply {
                putExtra("PACKAGE_ID", pkg.id) // CORREGIDO: Usar String ID
            }
            startActivity(intent)
        }
        recyclerView.adapter = packageAdapter
    }

    private fun setupFab() {
        fabAddPackage = findViewById(R.id.fabAddPackage)
        fabAddPackage.setOnClickListener {
            val intent = Intent(this, CreatePackageActivity::class.java)
            createPackageLauncher.launch(intent)
        }
    }

    private fun loadPackages() {
        lifecycleScope.launch {
            try {
                // Obtener userId dinámico del DataStore
                val userId = loginDataStore.getUserId() ?: "default_user"

                // DEBUG: Verificar el userId que se está usando
                println("🔄 DEBUG: Buscando paquetes para userId: $userId")

                val result = packageRepository.getUserPackages(userId)
                when (result) {
                    is Result.Success -> {
                        val packages = result.data
                        println("✅ DEBUG: Se encontraron ${packages.size} paquetes")
                        packageAdapter.updatePackages(packages)

                        if (packages.isEmpty()) {
                            Toast.makeText(
                                this@PackageListActivity,
                                "No tienes paquetes registrados. ¡Añade uno nuevo!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is Result.Error -> {
                        println("❌ DEBUG: Error al cargar: ${result.exception.message}")
                        Toast.makeText(
                            this@PackageListActivity,
                            "Error al cargar los paquetes: ${result.exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        packageAdapter.updatePackages(emptyList())
                    }
                    else -> {
                        packageAdapter.updatePackages(emptyList())
                    }
                }
            } catch (e: Exception) {
                println("❌ DEBUG: Excepción: ${e.message}")
                Toast.makeText(
                    this@PackageListActivity,
                    "Error inesperado: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                packageAdapter.updatePackages(emptyList())
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}