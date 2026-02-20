package green.go

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import green.go.databinding.ItemDeliveryBinding
import green.go.model.Delivery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DeliveryAdapter(
    private var deliveries: List<Delivery>,
    private val displayMode: Int = MODE_STANDARD,
    private val onItemClick: (Delivery) -> Unit
) : RecyclerView.Adapter<DeliveryAdapter.DeliveryViewHolder>() {

    companion object {
        const val MODE_STANDARD = 0 // Shows everything (Delivered)
        const val MODE_PENDING = 1  // Shows address/pickup time only
        const val MODE_ACTIVE = 2   // Hides status and pickup time (In Progress / Picked Up)
    }

    inner class DeliveryViewHolder(val binding: ItemDeliveryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val binding = ItemDeliveryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeliveryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {
        val delivery = deliveries[position]
        val binding = holder.binding

        // Common Data
        binding.tvOrderId.text = "Order #${delivery.orderId}"
        binding.tvAddress.text = delivery.deliveryAddress
        binding.tvPickupAddress.text = delivery.pickupAddress
        binding.tvCost.text = "${delivery.cost} RON"
        binding.tvItems.text = "Items: ${delivery.items}"
        binding.tvStatus.text = delivery.status

        // Status Styling
        when (delivery.status) {
            "PENDING" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            "IN_PROGRESS", "PICKED_UP" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
            else -> binding.tvStatus.setBackgroundColor(Color.GRAY)
        }

        when (displayMode) {
            MODE_PENDING -> {
                binding.tvOrderId.visibility = View.GONE
                binding.tvAddress.visibility = View.GONE
                binding.tvStatus.visibility = View.GONE
                binding.tvItems.visibility = View.GONE
                binding.tvCost.visibility = View.GONE

                binding.tvPickupAddress.visibility = View.VISIBLE
                binding.tvPickUpTime.visibility = View.VISIBLE

                val pickupDate = parseDate(delivery.pickUpTime)
                val now = Date()
                val diffHelper = pickupDate.time - now.time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffHelper)

                binding.tvPickUpTime.text = "Pickup in: $minutes min"

                if (minutes < 5) {
                    binding.root.setBackgroundColor(Color.parseColor("#FFCDD2"))
                } else {
                    binding.root.setCardBackgroundColor(Color.WHITE)
                }
            }
            MODE_ACTIVE -> {
                binding.tvOrderId.visibility = View.VISIBLE
                binding.tvAddress.visibility = View.VISIBLE
                binding.tvStatus.visibility = View.GONE
                binding.tvItems.visibility = View.VISIBLE
                binding.tvCost.visibility = View.VISIBLE
                binding.tvPickupAddress.visibility = View.VISIBLE
                binding.tvPickUpTime.visibility = View.GONE

                binding.root.setCardBackgroundColor(Color.WHITE)
            }
            else -> { // MODE_STANDARD
                binding.tvOrderId.visibility = View.VISIBLE
                binding.tvAddress.visibility = View.VISIBLE
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvItems.visibility = View.VISIBLE
                binding.tvCost.visibility = View.VISIBLE
                binding.tvPickupAddress.visibility = View.VISIBLE
                binding.tvPickUpTime.visibility = View.VISIBLE

                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.tvPickUpTime.text = formatDate(delivery.pickUpTime)
            }
        }

        binding.root.setOnClickListener {
            onItemClick(delivery)
        }
    }

    override fun getItemCount() = deliveries.size

    fun updateData(newDeliveries: List<Delivery>) {
        deliveries = newDeliveries
        notifyDataSetChanged()
    }

    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getDefault()
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatDate(dateString: String): String {
        return dateString.replace("T", " ").replace("Z", "")
    }
}
