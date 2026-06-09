package com.example.demo.dto

/**
 * 分片上传请求参数
 */
data class ChunkUploadRequest(
    val uploadId: String,        // 上传任务唯一标识
    val chunkIndex: Int,         // 当前分片索引（从0开始）
    val totalChunks: Int,        // 总分片数
    val originalFilename: String // 原始文件名
)

/**
 * 分片上传结果
 */
data class ChunkUploadResult(
    val success: Boolean,
    val uploadId: String,
    val chunkIndex: Int,
    val message: String,
    val isLastChunk: Boolean = false
)

/**
 * 分片合并请求
 */
data class ChunkMergeRequest(
    val uploadId: String,
    val totalChunks: Int,
    val originalFilename: String
)

/**
 * 分片合并响应
 */
data class ChunkMergeResponse(
    val success: Boolean,
    val message: String,
    val filename: String? = null,
    val filePath: String? = null,
    val fileSize: Long? = null
)

/**
 * 分片上传状态查询响应
 */
data class ChunkStatusResponse(
    val uploadId: String,
    val totalChunks: Int,
    val uploadedChunks: List<Int>,  // 已上传的分片索引列表
    val isComplete: Boolean,
    val missingChunks: List<Int>    // 缺失的分片索引列表
)
