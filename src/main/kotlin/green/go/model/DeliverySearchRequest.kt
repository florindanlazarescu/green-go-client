package green.go.model

data class DeliverySearchRequest(
    val queryFilter: QueryFilter,
    val merchants: List<Any> = emptyList(),
    val statuses: List<String>
)

data class QueryFilter(
    val ByCourier: ByCourier
)

data class ByCourier(
    val courierId: Long
)
