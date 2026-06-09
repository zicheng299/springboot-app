package com.example.demo.dto

data class TextUploadResponse(
    val success: Boolean,
    val message: String,
    val savedFilePath: String? = null
)
