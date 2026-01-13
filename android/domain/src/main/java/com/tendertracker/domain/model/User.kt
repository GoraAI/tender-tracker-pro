package com.tendertracker.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String?,
    val companyName: String?,
    val isActive: Boolean = true
)
