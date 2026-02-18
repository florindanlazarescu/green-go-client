package green.go.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import green.go.model.ByCourier
import green.go.model.DeliverySearchRequest
import green.go.model.DeliveryState
import green.go.model.QueryFilter
import green.go.network.DeliveryRepository
import kotlinx.coroutines.launch

class DeliveryViewModel(private val repository: DeliveryRepository) : ViewModel() {

    private val _deliveryState = MutableLiveData<DeliveryState>()
    val deliveryState: LiveData<DeliveryState> = _deliveryState

    fun fetchPendingDeliveries() {
        _deliveryState.value = DeliveryState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getPendingDeliveries()
                if (response.isSuccessful) {
                    val deliveries = response.body()?.deliveries ?: emptyList()
                    if (deliveries.isEmpty()) {
                        _deliveryState.value = DeliveryState.Empty
                    } else {
                        _deliveryState.value = DeliveryState.Success(deliveries)
                    }
                } else {
                    _deliveryState.value = DeliveryState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _deliveryState.value = DeliveryState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchInProgressDeliveries(courierId: Long) {
        fetchByStatus(courierId, listOf("IN_PROGRESS"))
    }

    fun fetchPickedUpDeliveries(courierId: Long) {
        fetchByStatus(courierId, listOf("PICKED_UP"))
    }

    fun fetchHistory(courierId: Long) {
        fetchByStatus(courierId, listOf("DELIVERED"))
    }

    private fun fetchByStatus(courierId: Long, statuses: List<String>) {
        _deliveryState.value = DeliveryState.Loading
        viewModelScope.launch {
            try {
                val request = DeliverySearchRequest(
                    queryFilter = QueryFilter(ByCourier(courierId)),
                    statuses = statuses
                )
                val response = repository.searchDeliveries(request)
                if (response.isSuccessful) {
                    val deliveries = response.body()?.deliveries ?: emptyList()
                    if (deliveries.isEmpty()) {
                        _deliveryState.value = DeliveryState.Empty
                    } else {
                        _deliveryState.value = DeliveryState.Success(deliveries)
                    }
                } else {
                    _deliveryState.value = DeliveryState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _deliveryState.value = DeliveryState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
