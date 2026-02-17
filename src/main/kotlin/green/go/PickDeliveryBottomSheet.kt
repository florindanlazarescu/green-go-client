package green.go

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import green.go.model.Delivery

class PickDeliveryBottomSheet(
    private val delivery: Delivery,
    private val buttonText: String = "Accept",
    private val questionText: String = "Do you want to accept this delivery?",
    private val onConfirm: (Delivery) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_pick_delivery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvOrderTitle = view.findViewById<TextView>(R.id.tvOrderTitle)
        val tvPickupAddress = view.findViewById<TextView>(R.id.tvPickupAddress)
        val tvDeliveryAddress = view.findViewById<TextView>(R.id.tvDeliveryAddress)
        val tvCost = view.findViewById<TextView>(R.id.tvCost)
        val tvQuestion = view.findViewById<TextView>(R.id.tvQuestion)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        tvOrderTitle.text = "Order #${delivery.orderId}"
        tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
        tvDeliveryAddress.text = "Deliver to: ${delivery.deliveryAddress}"
        tvCost.text = "${delivery.cost} RON"
        tvQuestion.text = questionText
        btnAccept.text = buttonText

        btnAccept.setOnClickListener {
            onConfirm(delivery)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
