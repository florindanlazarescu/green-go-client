package green.go.network

import green.go.model.DeliveryResponse
import green.go.model.DeliverySearchRequest
import green.go.model.StatusUpdateResponse
import green.go.model.TotalEarningsResponse
import retrofit2.Response

class DeliveryRepository(private val apiService: ApiService) {

    suspend fun getPendingDeliveries(): Response<DeliveryResponse> {
        return apiService.getPendingDeliveries()
    }

    suspend fun searchDeliveries(request: DeliverySearchRequest): Response<DeliveryResponse> {
        return apiService.searchDeliveries(request)
    }

    suspend fun updateDeliveryStatus(orderId: Long, status: String, courierId: Long): Response<StatusUpdateResponse> {
        return apiService.updateDeliveryStatus(orderId, status, courierId)
    }

    suspend fun getDeliveriesByCourier(courierId: Long): Response<DeliveryResponse> {
        return apiService.getDeliveriesByCourier(courierId)
    }

    suspend fun getTotalEarnings(courierId: Long, date: String): Response<TotalEarningsResponse> {
        return apiService.getTotalEarnings(courierId, date)
    }
}
