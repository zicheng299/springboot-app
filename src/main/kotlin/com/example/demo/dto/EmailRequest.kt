package com.example.demo.dto

data class EmailRequest(
    val to: String,
    val subject: String,
    val text: String
)
