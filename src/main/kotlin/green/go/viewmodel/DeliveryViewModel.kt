package green.go.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import green.go.model.ByCourier
import green.go.model.Delivery
import green.go.model.DeliverySearchRequest
import green.go.model.DeliveryState
import green.go.model.QueryFilter
import green.go.network.DeliveryRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyStats(val count: Int, val earnings: Double)

class DeliveryViewModel(private val repository: DeliveryRepository) : ViewModel() {

    private val _deliveryState = MutableLiveData<DeliveryState>()
    val deliveryState: LiveData<DeliveryState> = _deliveryState

    private val _statusUpdateResult = MutableLiveData<Boolean>()
    val statusUpdateResult: LiveData<Boolean> = _statusUpdateResult

    private val _todayStats = MutableLiveData<DailyStats>()
    val todayStats: LiveData<DailyStats> = _todayStats

    private val _pendingCount = MutableLiveData<Int>()
    val pendingCount: LiveData<Int> = _pendingCount

    fun fetchPendingDeliveries() {
        _deliveryState.value = DeliveryState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getPendingDeliveries()
                if (response.isSuccessful) {
                    val deliveries = response.body()?.deliveries ?: emptyList()
                    _pendingCount.value = deliveries.size
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

    fun silentFetchPendingCount() {
        viewModelScope.launch {
            try {
                val response = repository.getPendingDeliveries()
                if (response.isSuccessful) {
                    _pendingCount.value = response.body()?.deliveries?.size ?: 0
                }
            } catch (e: Exception) { }
        }
    }

    fun updateDeliveryStatus(delivery: Delivery, status: String, courierId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.updateDeliveryStatus(delivery.orderId, status, courierId)
                if (response.isSuccessful) {
                    _statusUpdateResult.value = true
                    fetchPendingDeliveries()
                } else {
                    _statusUpdateResult.value = false
                }
            } catch (e: Exception) {
                _statusUpdateResult.value = false
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

    fun fetchTodayStats(courierId: Long) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val dateParam = "$today 00:00:00"
                
                val response = repository.getTotalEarnings(courierId, dateParam)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _todayStats.value = DailyStats(body.count, body.total)
                    }
                }
            } catch (e: Exception) { }
        }
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
