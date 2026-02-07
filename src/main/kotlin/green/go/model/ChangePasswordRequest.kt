package green.go.model

data class ChangePasswordRequest(
    val email: String,
    val oldPassword: String,
    val newPassword: String
)
