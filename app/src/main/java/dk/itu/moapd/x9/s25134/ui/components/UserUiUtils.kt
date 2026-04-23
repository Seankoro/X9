package dk.itu.moapd.x9.s25134.ui.components

fun userInitials(displayName: String, email: String): String =
    displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { email.first().uppercase() }
