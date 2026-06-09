package com.example.demo.domain

data class LoginResponse(
    val data: LoginData = LoginData()
)

data class LoginData(
    val token: String = ""
)
