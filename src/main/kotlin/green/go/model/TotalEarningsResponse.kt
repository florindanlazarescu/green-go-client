package green.go.model

import com.google.gson.annotations.SerializedName

data class TotalEarningsResponse(
    @SerializedName("totalDeliveries")
    val count: Int,
    @SerializedName("totalAmount")
    val total: Double
)
