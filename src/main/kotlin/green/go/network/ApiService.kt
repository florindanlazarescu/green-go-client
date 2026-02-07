package green.go.network

import green.go.model.DeliveryResponse
import green.go.model.DeliverySearchRequest
import green.go.model.LoginRequest
import green.go.model.LoginResponse
import green.go.model.StatusUpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/deliveries")
    suspend fun getPendingDeliveries(@Query("status") status: String = "PENDING"): Response<DeliveryResponse>

    @POST("api/deliveries/search")
    suspend fun searchDeliveries(@Body request: DeliverySearchRequest): Response<DeliveryResponse>

    @PUT("api/delivery")
    suspend fun updateDeliveryStatus(
        @Query("order-id") orderId: Long,
        @Query("status") status: String,
        @Query("courier-id") courierId: Long
    ): Response<StatusUpdateResponse>

    @GET("api/deliveries")
    suspend fun getDeliveriesByCourier(@Query("courier-id") courierId: Long): Response<DeliveryResponse>

    @PUT("api/users/password")
    suspend fun changePassword(@Body request: green.go.model.ChangePasswordRequest): Response<StatusUpdateResponse>
}
