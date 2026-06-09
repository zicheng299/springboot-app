package com.example.demo.dto

data class TextUploadRequest(
    val content: String,
    val filename: String? = null
)
