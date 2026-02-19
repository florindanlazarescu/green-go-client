package green.go.model

data class Delivery(
    val orderId: Long,
    val merchantId: Long,
    val userId: Long,
    val deliveryAddress: String,
    val pickupAddress: String,
    val items: Int,
    val cost: Double,
    val status: String,
    val pickUpTime: String,
    val deliveredTime: String? = null
)

data class DeliveryResponse(
    val deliveries: List<Delivery>,
    val cursor: Long
)
