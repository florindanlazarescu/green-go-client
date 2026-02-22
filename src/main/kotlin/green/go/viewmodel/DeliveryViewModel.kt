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

    // Stari separate pentru fiecare ecran
    private val _pendingState = MutableLiveData<DeliveryState>()
    val pendingState: LiveData<DeliveryState> = _pendingState

    private val _inProgressState = MutableLiveData<DeliveryState>()
    val inProgressState: LiveData<DeliveryState> = _inProgressState

    private val _pickedUpState = MutableLiveData<DeliveryState>()
    val pickedUpState: LiveData<DeliveryState> = _pickedUpState

    private val _historyState = MutableLiveData<DeliveryState>()
    val historyState: LiveData<DeliveryState> = _historyState

    private val _statusUpdateResult = MutableLiveData<Boolean>()
    val statusUpdateResult: LiveData<Boolean> = _statusUpdateResult

    private val _todayStats = MutableLiveData<DailyStats>()
    val todayStats: LiveData<DailyStats> = _todayStats

    private val _pendingCount = MutableLiveData<Int>()
    val pendingCount: LiveData<Int> = _pendingCount

    fun fetchPendingDeliveries() {
        _pendingState.value = DeliveryState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getPendingDeliveries()
                if (response.isSuccessful) {
                    val deliveries = response.body()?.deliveries ?: emptyList()
                    _pendingCount.value = deliveries.size
                    _pendingState.value = if (deliveries.isEmpty()) DeliveryState.Empty else DeliveryState.Success(deliveries)
                } else {
                    _pendingState.value = DeliveryState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _pendingState.value = DeliveryState.Error(e.message ?: "Unknown error")
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
        fetchByStatus(courierId, listOf("IN_PROGRESS"), _inProgressState)
    }

    fun fetchPickedUpDeliveries(courierId: Long) {
        fetchByStatus(courierId, listOf("PICKED_UP"), _pickedUpState)
    }

    fun fetchHistory(courierId: Long) {
        fetchByStatus(courierId, listOf("DELIVERED"), _historyState)
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

    private fun fetchByStatus(courierId: Long, statuses: List<String>, stateLiveData: MutableLiveData<DeliveryState>) {
        stateLiveData.value = DeliveryState.Loading
        viewModelScope.launch {
            try {
                val request = DeliverySearchRequest(
                    queryFilter = QueryFilter(ByCourier(courierId)),
                    statuses = statuses
                )
                val response = repository.searchDeliveries(request)
                if (response.isSuccessful) {
                    val deliveries = response.body()?.deliveries ?: emptyList()
                    stateLiveData.value = if (deliveries.isEmpty()) DeliveryState.Empty else DeliveryState.Success(deliveries)
                } else {
                    stateLiveData.value = DeliveryState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                stateLiveData.value = DeliveryState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
