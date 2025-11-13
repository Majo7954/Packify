package com.ucb.deliveryapp.ui.screens.packages

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.db.AppDatabase
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.repository.PackageRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PackageDetailActivity : AppCompatActivity() {

    private lateinit var tvTrackingNumber: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSenderName: TextView
    private lateinit var tvRecipientName: TextView
    private lateinit var tvRecipientAddress: TextView
    private lateinit var tvRecipientPhone: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvEstimatedDate: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvNotes: TextView
    private lateinit var btnUpdateStatus: Button
    private lateinit var btnMarkDelivered: Button
    private lateinit var btnDelete: Button

    private lateinit var packageRepository: PackageRepository
    private var currentPackage: Package? = null
    private var packageId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_package_detail)

        supportActionBar?.title = "Detalle del Paquete"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        packageId = intent.getIntExtra("PACKAGE_ID", 0)
        if (packageId == 0) {
            Toast.makeText(this, "Error: No se encontró el paquete", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val database = AppDatabase.getDatabase(this)
        packageRepository = PackageRepository(database.packageDao())

        initViews()

        btnUpdateStatus.setOnClickListener { showUpdateStatusDialog() }
        btnMarkDelivered.setOnClickListener { markAsDelivered() }
        btnDelete.setOnClickListener { confirmDelete() }

        loadPackageDetails()
    }

    private fun initViews() {
        tvTrackingNumber = findViewById(R.id.tvTrackingNumber)
        tvStatus = findViewById(R.id.tvStatus)
        tvSenderName = findViewById(R.id.tvSenderName)
        tvRecipientName = findViewById(R.id.tvRecipientName)
        tvRecipientAddress = findViewById(R.id.tvRecipientAddress)
        tvRecipientPhone = findViewById(R.id.tvRecipientPhone)
        tvWeight = findViewById(R.id.tvWeight)
        tvPriority = findViewById(R.id.tvPriority)
        tvEstimatedDate = findViewById(R.id.tvEstimatedDate)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        tvNotes = findViewById(R.id.tvNotes)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus)
        btnMarkDelivered = findViewById(R.id.btnMarkDelivered)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun loadPackageDetails() {
        lifecycleScope.launch {
            try {
                currentPackage = packageRepository.getPackageById(packageId)
                currentPackage?.let { displayPackage(it) }
                    ?: run {
                        Toast.makeText(
                            this@PackageDetailActivity,
                            "Paquete no encontrado",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PackageDetailActivity,
                    "Error al cargar detalles: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayPackage(pkg: Package) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        tvTrackingNumber.text = pkg.trackingNumber
        tvSenderName.text = pkg.senderName
        tvRecipientName.text = pkg.recipientName
        tvRecipientAddress.text = pkg.recipientAddress
        tvRecipientPhone.text = pkg.recipientPhone
        tvWeight.text = "${pkg.weight} kg"

        tvPriority.text = when (pkg.priority) {
            "normal" -> "Normal"
            "express" -> "Express"
            "urgent" -> "Urgente"
            else -> pkg.priority
        }

        tvStatus.text = when (pkg.status) {
            PackageStatus.PENDING -> "⏳ Pendiente"
            PackageStatus.IN_TRANSIT -> "🚚 En tránsito"
            PackageStatus.DELIVERED -> "✓ Entregado"
            PackageStatus.CANCELLED -> "✗ Cancelado"
            else -> pkg.status
        }

        tvEstimatedDate.text = dateFormat.format(Date(pkg.estimatedDeliveryDate))
        tvCreatedAt.text = dateFormat.format(Date(pkg.createdAt))
        tvNotes.text = pkg.notes ?: "Sin notas"

        btnMarkDelivered.isEnabled = pkg.status != PackageStatus.DELIVERED
    }

    private fun showUpdateStatusDialog() {
        val estados = arrayOf("Pendiente", "En tránsito", "Entregado", "Cancelado")
        val values = arrayOf(
            PackageStatus.PENDING,
            PackageStatus.IN_TRANSIT,
            PackageStatus.DELIVERED,
            PackageStatus.CANCELLED
        )

        AlertDialog.Builder(this)
            .setTitle("Actualizar Estado")
            .setItems(estados) { _, which ->
                lifecycleScope.launch {
                    packageRepository.updatePackageStatus(packageId, values[which])
                    Toast.makeText(
                        this@PackageDetailActivity,
                        "Estado actualizado",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPackageDetails()
                }
            }
            .show()
    }

    private fun markAsDelivered() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Entrega")
            .setMessage("¿Estás seguro de que quieres marcar este paquete como entregado?")
            .setPositiveButton("Sí, Entregado") { _, _ ->
                lifecycleScope.launch {
                    packageRepository.markAsDelivered(packageId)
                    Toast.makeText(
                        this@PackageDetailActivity,
                        "Paquete marcado como entregado",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPackageDetails()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Realmente quieres eliminar este paquete? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    currentPackage?.let {
                        packageRepository.deletePackage(it)
                        Toast.makeText(
                            this@PackageDetailActivity,
                            "Paquete eliminado",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
