package green.go

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
        val btnNavigate = view.findViewById<Button>(R.id.btnNavigate)
        val btnCall = view.findViewById<Button>(R.id.btnCall)

        tvOrderTitle.text = "Order #${delivery.orderId}"
        tvPickupAddress.text = "Pickup: ${delivery.pickupAddress}"
        tvDeliveryAddress.text = "Deliver to: ${delivery.deliveryAddress}"
        tvCost.text = "${delivery.cost} RON"
        tvQuestion.text = questionText
        btnAccept.text = buttonText

        // Logica pentru Navigație optimizată pentru Greenfield
        btnNavigate.setOnClickListener {
            // Adăugăm contextul locației pentru a restrânge aria de căutare
            val fullAddress = "${delivery.deliveryAddress}, Greenfield, Bucuresti"
            val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(fullAddress)}")
            
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Alternativă pentru orice alt browser/hartă
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(fullAddress)}"))
                startActivity(genericIntent)
            }
        }

        btnCall.setOnClickListener {
            val phoneNumber = delivery.phoneNumber
            if (!phoneNumber.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Phone number not available for this order.", Toast.LENGTH_SHORT).show()
            }
        }

        btnAccept.setOnClickListener {
            onConfirm(delivery)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
