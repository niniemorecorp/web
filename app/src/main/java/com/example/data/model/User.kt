package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    OWNER,   // Full authority: Omzet, Sheet sync settings, Admin features, Export to Excel/PDF
    STAFF,   // Creation & Prints: Create receipts, Print thermal receipts, view lists
    COURIER  // Quick field updates: Update tracking status, add logistics checkpoints
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String, // Store plaintext or simple sha for mock, we will use basic password match for demo ease
    val fullName: String,
    val role: UserRole
)
