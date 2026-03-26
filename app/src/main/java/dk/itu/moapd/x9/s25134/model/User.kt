package dk.itu.moapd.x9.s25134.model

data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String = ""
)
