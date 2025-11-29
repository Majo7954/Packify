package com.ucb.deliveryapp.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackageStatus
import java.text.SimpleDateFormat
import java.util.*

class PackageAdapter(
    private var packages: List<Package>,
    private val onPackageClick: (Package) -> Unit
) : RecyclerView.Adapter<PackageAdapter.PackageViewHolder>() {

    class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardPackage)
        val tvTrackingNumber: TextView = itemView.findViewById(R.id.tvTrackingNumber)
        val tvRecipientName: TextView = itemView.findViewById(R.id.tvRecipientName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvEstimatedDate: TextView = itemView.findViewById(R.id.tvEstimatedDate)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val pkg = packages[position]

        holder.tvTrackingNumber.text = "Nº ${pkg.trackingNumber}"
        holder.tvRecipientName.text = pkg.recipientName
        holder.tvAddress.text = pkg.recipientAddress

        // CORREGIDO: Usar Timestamp de Firebase
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = if (pkg.estimatedDeliveryDate.seconds > 0) {
            Date(pkg.estimatedDeliveryDate.seconds * 1000)
        } else {
            Date()
        }
        holder.tvEstimatedDate.text = "Entrega: ${dateFormat.format(date)}"

        when (pkg.status) {
            PackageStatus.PENDING -> {
                holder.tvStatus.text = "⏳ Pendiente"
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"))
            }
            PackageStatus.IN_TRANSIT -> {
                holder.tvStatus.text = "🚚 En tránsito"
                holder.tvStatus.setTextColor(Color.parseColor("#2196F3"))
            }
            PackageStatus.DELIVERED -> {
                holder.tvStatus.text = "✓ Entregado"
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            }
            PackageStatus.CANCELLED -> {
                holder.tvStatus.text = "✗ Cancelado"
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"))
            }
        }

        holder.cardView.setOnClickListener {
            onPackageClick(pkg)
        }
    }

    override fun getItemCount(): Int = packages.size

    fun updatePackages(newPackages: List<Package>) {
        packages = newPackages
        notifyDataSetChanged()
    }
}