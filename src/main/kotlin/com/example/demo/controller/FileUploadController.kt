package com.example.demo.controller

import com.example.demo.dto.*
import com.example.demo.service.FileStorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/file")
class FileUploadController(private val fileStorageService: FileStorageService) {

    /**
     * 传统单文件/多文件上传（保留原有接口）
     */
    @PostMapping("/upload")
    fun uploadFiles(
        @RequestParam("files") files: List<MultipartFile>
    ): ResponseEntity<FileUploadResponse> {
        return try {
            require(files.isNotEmpty()) { "上传文件列表不能为空" }

            val results = fileStorageService.saveFiles(files)
            val successCount = results.count { it.success }
            val failedCount = results.size - successCount
            val allSuccess = failedCount == 0

            ResponseEntity.ok(
                FileUploadResponse(
                    success = allSuccess,
                    message = if (allSuccess) "所有文件上传成功" else "部分文件上传失败 (${successCount}/${files.size})",
                    totalCount = files.size,
                    successCount = successCount,
                    failedCount = failedCount,
                    results = results
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                FileUploadResponse(
                    success = false,
                    message = e.message ?: "请求参数错误",
                    totalCount = 0,
                    successCount = 0,
                    failedCount = 0,
                    results = emptyList()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                FileUploadResponse(
                    success = false,
                    message = "文件上传失败: ${e.message}",
                    totalCount = 0,
                    successCount = 0,
                    failedCount = 0,
                    results = emptyList()
                )
            )
        }
    }

    // ==================== 分片上传接口 ====================

    /**
     * 上传单个分片
     * @param file 分片文件
     * @param uploadId 上传任务唯一标识（前端生成，如UUID）
     * @param chunkIndex 当前分片索引（从0开始）
     * @param totalChunks 总分片数
     * @param originalFilename 原始文件名
     */
    @PostMapping("/chunk/upload")
    fun uploadChunk(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("uploadId") uploadId: String,
        @RequestParam("chunkIndex") chunkIndex: Int,
        @RequestParam("totalChunks") totalChunks: Int,
        @RequestParam("originalFilename") originalFilename: String
    ): ResponseEntity<ChunkUploadResult> {
        return try {
            require(uploadId.isNotBlank()) { "uploadId不能为空" }
            require(chunkIndex >= 0) { "chunkIndex不能为负数" }
            require(totalChunks > 0) { "totalChunks必须大于0" }
            require(chunkIndex < totalChunks) { "chunkIndex必须小于totalChunks" }
            require(originalFilename.isNotBlank()) { "originalFilename不能为空" }

            val result = fileStorageService.saveChunk(file, uploadId, chunkIndex, totalChunks)
            if (result.success) {
                ResponseEntity.ok(result)
            } else {
                ResponseEntity.badRequest().body(result)
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                ChunkUploadResult(
                    success = false,
                    uploadId = uploadId,
                    chunkIndex = chunkIndex,
                    message = e.message ?: "请求参数错误"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                ChunkUploadResult(
                    success = false,
                    uploadId = uploadId,
                    chunkIndex = chunkIndex,
                    message = "分片上传失败: ${e.message}"
                )
            )
        }
    }

    /**
     * 查询分片上传状态（用于断点续传）
     * @param uploadId 上传任务唯一标识
     * @param totalChunks 总分片数
     */
    @GetMapping("/chunk/status")
    fun getChunkStatus(
        @RequestParam("uploadId") uploadId: String,
        @RequestParam("totalChunks") totalChunks: Int
    ): ResponseEntity<ChunkStatusResponse> {
        return try {
            require(uploadId.isNotBlank()) { "uploadId不能为空" }
            require(totalChunks > 0) { "totalChunks必须大于0" }

            val status = fileStorageService.getChunkStatus(uploadId, totalChunks)
            ResponseEntity.ok(status)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                ChunkStatusResponse(
                    uploadId = uploadId,
                    totalChunks = totalChunks,
                    uploadedChunks = emptyList(),
                    isComplete = false,
                    missingChunks = emptyList()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                ChunkStatusResponse(
                    uploadId = uploadId,
                    totalChunks = totalChunks,
                    uploadedChunks = emptyList(),
                    isComplete = false,
                    missingChunks = emptyList()
                )
            )
        }
    }

    /**
     * 合并所有分片为完整文件
     */
    @PostMapping("/chunk/merge")
    fun mergeChunks(
        @RequestBody request: ChunkMergeRequest
    ): ResponseEntity<ChunkMergeResponse> {
        return try {
            require(request.uploadId.isNotBlank()) { "uploadId不能为空" }
            require(request.totalChunks > 0) { "totalChunks必须大于0" }
            require(request.originalFilename.isNotBlank()) { "originalFilename不能为空" }

            val result = fileStorageService.mergeChunks(
                request.uploadId,
                request.totalChunks,
                request.originalFilename
            )
            if (result.success) {
                ResponseEntity.ok(result)
            } else {
                ResponseEntity.badRequest().body(result)
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                ChunkMergeResponse(
                    success = false,
                    message = e.message ?: "请求参数错误"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                ChunkMergeResponse(
                    success = false,
                    message = "合并失败: ${e.message}"
                )
            )
        }
    }

    /**
     * 清理指定上传任务的临时分片
     */
    @PostMapping("/chunk/cleanup")
    fun cleanupChunks(
        @RequestParam("uploadId") uploadId: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            require(uploadId.isNotBlank()) { "uploadId不能为空" }
            val success = fileStorageService.cleanupChunks(uploadId)
            ResponseEntity.ok(
                mapOf(
                    "success" to success,
                    "uploadId" to uploadId,
                    "message" to if (success) "清理成功" else "清理失败"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "uploadId" to uploadId,
                    "message" to (e.message ?: "清理失败")
                )
            )
        }
    }
}
