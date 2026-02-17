package green.go

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

    class DeliveryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvPickupAddress: TextView = itemView.findViewById(R.id.tvPickupAddress)
        val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        val tvPickUpTime: TextView = itemView.findViewById(R.id.tvPickUpTime)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery, parent, false)
        return DeliveryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {
        val delivery = deliveries[position]

        when (displayMode) {
            MODE_PENDING -> {
                holder.tvOrderId.visibility = View.GONE
                holder.tvAddress.visibility = View.GONE
                holder.tvStatus.visibility = View.GONE
                holder.tvItems.visibility = View.GONE
                holder.tvCost.visibility = View.GONE

                holder.tvPickupAddress.visibility = View.VISIBLE
                holder.tvPickupAddress.text = "Address: ${delivery.pickupAddress}"

                holder.tvPickUpTime.visibility = View.VISIBLE

                // Calculate minutes remaining
                val pickupDate = parseDate(delivery.pickUpTime)
                val now = Date()
                val diffHelper = pickupDate.time - now.time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffHelper)

                holder.tvPickUpTime.text = "Pickup in: $minutes min"

                // Highlight urgent orders (< 5 mins)
                if (minutes < 5) {
                    holder.itemView.setBackgroundColor(Color.parseColor("#FFCDD2")) // Light Red
                } else {
                    holder.itemView.setBackgroundColor(Color.WHITE) // Default
                }
            }
            MODE_ACTIVE -> {
                holder.tvOrderId.visibility = View.VISIBLE
                holder.tvAddress.visibility = View.VISIBLE
                holder.tvStatus.visibility = View.GONE     // Hide Status
                holder.tvItems.visibility = View.VISIBLE
                holder.tvCost.visibility = View.VISIBLE
                holder.tvPickupAddress.visibility = View.VISIBLE
                holder.tvPickUpTime.visibility = View.GONE // Hide Pickup Time

                holder.itemView.setBackgroundColor(Color.WHITE)

                holder.tvOrderId.text = "Order #${delivery.orderId}"
                holder.tvAddress.text = "Deliver to: ${delivery.deliveryAddress}"
                holder.tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
                holder.tvItems.text = "Items: ${delivery.items}"
                holder.tvCost.text = "Cost: ${delivery.cost} RON"
            }
            else -> { // MODE_STANDARD
                holder.tvOrderId.visibility = View.VISIBLE
                holder.tvAddress.visibility = View.VISIBLE
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvItems.visibility = View.VISIBLE
                holder.tvCost.visibility = View.VISIBLE
                holder.tvPickupAddress.visibility = View.VISIBLE
                holder.tvPickUpTime.visibility = View.VISIBLE

                holder.itemView.setBackgroundColor(Color.WHITE)

                holder.tvOrderId.text = "Order #${delivery.orderId}"
                holder.tvAddress.text = "Deliver to: ${delivery.deliveryAddress}"
                holder.tvStatus.text = "Status: ${delivery.status}"
                holder.tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
                holder.tvItems.text = "Items: ${delivery.items}"
                holder.tvPickUpTime.text = "Pickup Time: ${formatDate(delivery.pickUpTime)}"
                holder.tvCost.text = "Cost: ${delivery.cost} RON"
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(delivery)
        }
    }

    override fun getItemCount() = deliveries.size

    fun updateData(newDeliveries: List<Delivery>) {
        deliveries = newDeliveries
        notifyDataSetChanged()
    }

    private fun parseDate(dateString: String): Date {
        // Handle ISO 8601 format: 2023-10-27T10:00:00Z
        // If Z is present, it's UTC.
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getDefault()
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            // Fallback for date strings without Z or T if necessary, or just return now/epoch
            Date()
        }
    }

    private fun formatDate(dateString: String): String {
        // Simple string manipulation or use SimpleDateFormat if complex parsing needed.
        // Returning as is for now or replacing T with space
        return dateString.replace("T", " ").replace("Z", "")
    }
}
