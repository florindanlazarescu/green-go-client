package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import green.go.model.Delivery

class DeliveryDetailsBottomSheet(
    private val delivery: Delivery
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_delivery_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderTitle = view.findViewById<TextView>(R.id.tvOrderTitle)
        val tvPickupAddress = view.findViewById<TextView>(R.id.tvPickupAddress)
        val tvDeliveryAddress = view.findViewById<TextView>(R.id.tvDeliveryAddress)
        val tvItemsCount = view.findViewById<TextView>(R.id.tvItemsCount)
        val tvTotalEarned = view.findViewById<TextView>(R.id.tvTotalEarned)

        tvOrderTitle.text = "Order #${delivery.orderId}"
        tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
        tvDeliveryAddress.text = "Deliver to: ${delivery.deliveryAddress}"
        tvItemsCount.text = "${delivery.items} Items"
        tvTotalEarned.text = "${delivery.cost} RON"
    }
}
