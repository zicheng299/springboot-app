package com.example.demo.dto

data class PdfConversionResponse(
    val success: Boolean,
    val message: String,
    val totalPages: Int,
    val images: List<PdfPageImage>
)
