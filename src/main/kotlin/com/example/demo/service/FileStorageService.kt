package com.example.demo.service

import com.example.demo.dto.*
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class FileStorageService {

    private val fileStorageDir: Path = Paths.get("C:\\Users\\bryce\\Downloads").toAbsolutePath()
    private val tempDir: Path = fileStorageDir.resolve("upload-temp")

    // 用于跟踪正在合并的上传任务，防止并发合并冲突
    private val mergingTasks = ConcurrentHashMap.newKeySet<String>()

    init {
        if (!Files.exists(fileStorageDir)) {
            Files.createDirectories(fileStorageDir)
        }
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir)
        }
    }

    fun saveFiles(files: List<MultipartFile>): List<FileUploadResult> {
        return files.map { file -> saveSingleFile(file) }
    }

    private fun saveSingleFile(file: MultipartFile): FileUploadResult {
        val originalFilename = file.originalFilename ?: "unknown"

        return try {
            if (file.isEmpty) {
                return FileUploadResult(
                    originalFilename = originalFilename,
                    savedFilename = "",
                    savedFilePath = "",
                    fileSize = 0,
                    success = false,
                    message = "文件为空"
                )
            }

            val safeName = originalFilename.replace(Regex("[^a-zA-Z0-9.\\u4e00-\\u9fa5_-]"), "_")
            val extension = safeName.substringAfterLast(".", "")
            val baseName = safeName.substringBeforeLast(".", safeName)
            val savedFilename = if (extension.isNotEmpty()) {
                "${baseName}.$extension"
            } else {
                "$baseName"
            }

            val targetPath = fileStorageDir.resolve(savedFilename)
            file.inputStream.use { input ->
                Files.copy(input, targetPath)
            }

            FileUploadResult(
                originalFilename = originalFilename,
                savedFilename = savedFilename,
                savedFilePath = targetPath.toString(),
                fileSize = file.size,
                success = true,
                message = "上传成功"
            )
        } catch (e: Exception) {
            FileUploadResult(
                originalFilename = originalFilename,
                savedFilename = "",
                savedFilePath = "",
                fileSize = 0,
                success = false,
                message = "上传失败: ${e.message}"
            )
        }
    }

    // ==================== 分片上传相关方法 ====================

    /**
     * 保存单个分片到临时目录
     */
    fun saveChunk(
        file: MultipartFile,
        uploadId: String,
        chunkIndex: Int,
        totalChunks: Int
    ): ChunkUploadResult {
        return try {
            if (file.isEmpty) {
                return ChunkUploadResult(
                    success = false,
                    uploadId = uploadId,
                    chunkIndex = chunkIndex,
                    message = "分片文件为空"
                )
            }

            val chunkDir = tempDir.resolve(uploadId)
            if (!Files.exists(chunkDir)) {
                Files.createDirectories(chunkDir)
            }

            val chunkPath = chunkDir.resolve("chunk_$chunkIndex")
            file.inputStream.use { input ->
                Files.copy(input, chunkPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }

            val isLastChunk = chunkIndex == totalChunks - 1

            ChunkUploadResult(
                success = true,
                uploadId = uploadId,
                chunkIndex = chunkIndex,
                message = "分片 $chunkIndex 上传成功",
                isLastChunk = isLastChunk
            )
        } catch (e: Exception) {
            ChunkUploadResult(
                success = false,
                uploadId = uploadId,
                chunkIndex = chunkIndex,
                message = "分片上传失败: ${e.message}"
            )
        }
    }

    /**
     * 查询分片上传状态
     */
    fun getChunkStatus(uploadId: String, totalChunks: Int): ChunkStatusResponse {
        val chunkDir = tempDir.resolve(uploadId)
        val uploadedChunks = mutableListOf<Int>()

        if (Files.exists(chunkDir)) {
            Files.list(chunkDir).use { stream ->
                stream.forEach { path ->
                    val filename = path.fileName.toString()
                    if (filename.startsWith("chunk_")) {
                        val index = filename.substringAfter("chunk_").toIntOrNull()
                        if (index != null) {
                            uploadedChunks.add(index)
                        }
                    }
                }
            }
        }

        val uploadedSet = uploadedChunks.toSortedSet()
        val allIndices = (0 until totalChunks).toSet()
        val missingChunks = (allIndices - uploadedSet).toList().sorted()

        return ChunkStatusResponse(
            uploadId = uploadId,
            totalChunks = totalChunks,
            uploadedChunks = uploadedChunks.sorted(),
            isComplete = missingChunks.isEmpty(),
            missingChunks = missingChunks
        )
    }

    /**
     * 合并所有分片为完整文件
     */
    fun mergeChunks(
        uploadId: String,
        totalChunks: Int,
        originalFilename: String
    ): ChunkMergeResponse {
        // 防止并发合并同一uploadId
        if (!mergingTasks.add(uploadId)) {
            return ChunkMergeResponse(
                success = false,
                message = "该上传任务正在合并中，请勿重复请求"
            )
        }

        try {
            val chunkDir = tempDir.resolve(uploadId)

            // 检查所有分片是否都已上传
            val status = getChunkStatus(uploadId, totalChunks)
            if (!status.isComplete) {
                return ChunkMergeResponse(
                    success = false,
                    message = "分片不完整，缺失分片: ${status.missingChunks}"
                )
            }

            // 生成安全的文件名
            val safeName = originalFilename.replace(Regex("[^a-zA-Z0-9.\\u4e00-\\u9fa5_-]"), "_")
            val extension = safeName.substringAfterLast(".", "")
            val baseName = safeName.substringBeforeLast(".", safeName)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val savedFilename = if (extension.isNotEmpty()) {
                "${baseName}_${timestamp}.$extension"
            } else {
                "${baseName}_${timestamp}"
            }

            val targetPath = fileStorageDir.resolve(savedFilename)

            // 按顺序合并分片
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { output ->
                for (i in 0 until totalChunks) {
                    val chunkPath = chunkDir.resolve("chunk_$i")
                    if (!Files.exists(chunkPath)) {
                        throw IllegalStateException("分片 $i 不存在")
                    }
                    Files.newInputStream(chunkPath).use { input ->
                        input.copyTo(output)
                    }
                }
            }

            val fileSize = Files.size(targetPath)

            // 合并成功后删除临时分片
            deleteChunkDir(uploadId)

            return ChunkMergeResponse(
                success = true,
                message = "文件合并成功",
                filename = savedFilename,
                filePath = targetPath.toString(),
                fileSize = fileSize
            )
        } catch (e: Exception) {
            return ChunkMergeResponse(
                success = false,
                message = "文件合并失败: ${e.message}"
            )
        } finally {
            mergingTasks.remove(uploadId)
        }
    }

    /**
     * 清理指定上传任务的临时分片
     */
    fun cleanupChunks(uploadId: String): Boolean {
        return try {
            deleteChunkDir(uploadId)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteChunkDir(uploadId: String) {
        val chunkDir = tempDir.resolve(uploadId)
        if (Files.exists(chunkDir)) {
            Files.walk(chunkDir).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
