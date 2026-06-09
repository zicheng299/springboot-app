package com.example.demo.dto

data class FileUploadResult(
    val originalFilename: String,
    val savedFilename: String,
    val savedFilePath: String,
    val fileSize: Long,
    val success: Boolean,
    val message: String? = null
)

data class FileUploadResponse(
    val success: Boolean,
    val message: String,
    val totalCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val results: List<FileUploadResult>
)
