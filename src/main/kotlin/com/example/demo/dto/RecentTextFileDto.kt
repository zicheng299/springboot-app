package com.example.demo.dto

data class RecentTextFileDto(
    val filename: String,
    val savedFileName: String,
    val content: String,
    val contentPreview: String,
    val timestamp: String,
    val fileSize: Long
)
