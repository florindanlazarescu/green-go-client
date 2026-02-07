package green.go.model

data class LoginResponse(
    val id: Long,
    val email: String,
    val token: String,
    val expires: Long,
    // Keep message for error cases if needed, but make it nullable
    val message: String? = null
)
