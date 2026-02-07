package green.go

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import green.go.model.Delivery

class DeliveryAdapter(
    private var deliveries: List<Delivery>,
    private val onItemClick: (Delivery) -> Unit
) : RecyclerView.Adapter<DeliveryAdapter.DeliveryViewHolder>() {

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
        holder.tvOrderId.text = "Order #${delivery.orderId}"
        holder.tvAddress.text = "Deliver to: ${delivery.deliveryAddress}"
        holder.tvStatus.text = "Status: ${delivery.status}"
        holder.tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
        holder.tvItems.text = "Items: ${delivery.items}"
        holder.tvPickUpTime.text = "Pickup Time: ${formatDate(delivery.pickUpTime)}"
        holder.tvCost.text = "Cost: $${delivery.cost}"

        holder.itemView.setOnClickListener {
            onItemClick(delivery)
        }
    }

    override fun getItemCount() = deliveries.size

    fun updateData(newDeliveries: List<Delivery>) {
        deliveries = newDeliveries
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        // Simple string manipulation or use SimpleDateFormat if complex parsing needed.
        // Returning as is for now or replacing T with space
        return dateString.replace("T", " ").replace("Z", "")
    }
}
