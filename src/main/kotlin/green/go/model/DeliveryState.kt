package green.go.model

sealed class DeliveryState {
    object Loading : DeliveryState()
    data class Success(val deliveries: List<Delivery>) : DeliveryState()
    data class Error(val message: String) : DeliveryState()
    object Empty : DeliveryState()
}
