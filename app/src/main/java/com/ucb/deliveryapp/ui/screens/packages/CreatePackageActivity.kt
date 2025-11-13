// CreatePackageActivity.kt - CORREGIDO
package com.ucb.deliveryapp.ui.screens.packages

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.db.AppDatabase // CORREGIDO: era data.db.AppDatabase
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.repository.PackageRepository // CORREGIDO: era repository.PackageRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreatePackageActivity : AppCompatActivity() {

    private lateinit var etTrackingNumber: TextInputEditText
    private lateinit var etSenderName: TextInputEditText
    private lateinit var etRecipientName: TextInputEditText
    private lateinit var etRecipientAddress: TextInputEditText
    private lateinit var etRecipientPhone: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var tvEstimatedDate: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var spinnerPriority: Spinner
    private lateinit var btnCreatePackage: Button

    private lateinit var packageRepository: PackageRepository
    private var selectedDate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_package)

        supportActionBar?.title = "Nuevo Paquete"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val database = AppDatabase.getDatabase(this)
        packageRepository = PackageRepository(database.packageDao())

        initViews()
        setupSpinner()

        btnSelectDate.setOnClickListener { showDatePicker() }
        btnCreatePackage.setOnClickListener { createPackage() }
    }

    private fun initViews() {
        etTrackingNumber = findViewById(R.id.etTrackingNumber)
        etSenderName = findViewById(R.id.etSenderName)
        etRecipientName = findViewById(R.id.etRecipientName)
        etRecipientAddress = findViewById(R.id.etRecipientAddress)
        etRecipientPhone = findViewById(R.id.etRecipientPhone)
        etWeight = findViewById(R.id.etWeight)
        etNotes = findViewById(R.id.etNotes)
        tvEstimatedDate = findViewById(R.id.tvEstimatedDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        btnCreatePackage = findViewById(R.id.btnCreatePackage)
    }

    private fun setupSpinner() {
        val priorities = arrayOf("Normal", "Express", "Urgente")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                selectedDate = selectedCalendar.timeInMillis

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvEstimatedDate.text = dateFormat.format(Date(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun createPackage() {
        if (!validateFields()) return

        val priority = when (spinnerPriority.selectedItemPosition) {
            0 -> PackagePriority.NORMAL
            1 -> PackagePriority.EXPRESS
            2 -> PackagePriority.URGENT
            else -> PackagePriority.NORMAL
        }

        val newPackage = Package(
            trackingNumber = etTrackingNumber.text.toString().trim(),
            senderName = etSenderName.text.toString().trim(),
            recipientName = etRecipientName.text.toString().trim(),
            recipientAddress = etRecipientAddress.text.toString().trim(),
            recipientPhone = etRecipientPhone.text.toString().trim(),
            weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0,
            status = PackageStatus.PENDING,
            priority = priority,
            estimatedDeliveryDate = selectedDate,
            notes = etNotes.text.toString().trim().ifEmpty { null },
            userId = 1 // TODO: Reemplazar con el ID del usuario logueado
        )

        lifecycleScope.launch {
            try {
                packageRepository.createPackage(newPackage) // CORREGIDO: era createPackage
                Toast.makeText(
                    this@CreatePackageActivity,
                    "Paquete creado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreatePackageActivity,
                    "Error al crear el paquete: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true
        if (etTrackingNumber.text.isNullOrBlank()) {
            etTrackingNumber.error = "Este campo es requerido"
            isValid = false
        }
        if (etSenderName.text.isNullOrBlank()) {
            etSenderName.error = "Este campo es requerido"
            isValid = false
        }
        if (etRecipientName.text.isNullOrBlank()) {
            etRecipientName.error = "Este campo es requerido"
            isValid = false
        }
        if (etRecipientAddress.text.isNullOrBlank()) {
            etRecipientAddress.error = "Este campo es requerido"
            isValid = false
        }
        if (etRecipientPhone.text.isNullOrBlank()) {
            etRecipientPhone.error = "Este campo es requerido"
            isValid = false
        }
        if (etWeight.text.isNullOrBlank()) {
            etWeight.error = "Este campo es requerido"
            isValid = false
        }
        if (selectedDate == 0L) {
            Toast.makeText(this, "Debe seleccionar una fecha de entrega", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        return isValid
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}