package com.example.demo.dto

data class PdfPageImage(
    val pageNumber: Int,
    val savedFileName: String,
    val savedFilePath: String,
    val fileSize: Long
)
