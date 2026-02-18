package green.go.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val id: Long,
    val email: String,
    val token: String,
    val expires: Long,
    @SerializedName("user_role")
    val role: String? = null,
    val message: String? = null
)
