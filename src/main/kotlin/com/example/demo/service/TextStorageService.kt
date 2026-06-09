package com.example.demo.service

import com.example.demo.dto.RecentTextFileDto
import com.example.demo.dto.TextUploadRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors

@Service
class TextStorageService {

    private val storageDir: Path = Paths.get("textFile").toAbsolutePath()
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)

    init {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir)
        }
    }

    fun getRecentFiles(limit: Int = 10): List<RecentTextFileDto> {
        if (!Files.exists(storageDir)) {
            return emptyList()
        }

        return Files.list(storageDir)
            .filter { it.toString().endsWith(".json") }
            .sorted { p1, p2 ->
                Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1))
            }
            .limit(limit.toLong())
            .map { path ->
                try {
                    val node = objectMapper.readTree(path.toFile())
                    val content = node.get("content")?.asText() ?: ""
                    val timestamp = node.get("timestamp")?.asText() ?: ""
                    val originalFilename = node.get("originalFilename")?.asText() ?: ""
                    val savedFileName = node.get("savedFileName")?.asText() ?: path.fileName.toString()
                    RecentTextFileDto(
                        filename = originalFilename.ifBlank { savedFileName },
                        savedFileName = savedFileName,
                        content = content,
                        contentPreview = if (content.length > 80) content.take(80) + "..." else content,
                        timestamp = timestamp,
                        fileSize = Files.size(path)
                    )
                } catch (e: Exception) {
                    RecentTextFileDto(
                        filename = path.fileName.toString(),
                        savedFileName = path.fileName.toString(),
                        content = "",
                        contentPreview = "",
                        timestamp = "",
                        fileSize = Files.size(path)
                    )
                }
            }
            .collect(Collectors.toList())
    }

    fun deleteFile(fileName: String): Boolean {
        val target = storageDir.resolve(fileName).normalize()
        // 防止目录遍历攻击
        if (!target.startsWith(storageDir)) {
            return false
        }
        return if (Files.exists(target) && target.toString().endsWith(".json")) {
            Files.delete(target)
            true
        } else {
            false
        }
    }

    fun clearAllFiles(): Int {
        if (!Files.exists(storageDir)) {
            return 0
        }
        var count = 0
        Files.list(storageDir)
            .filter { it.toString().endsWith(".json") }
            .forEach { path ->
                try {
                    Files.delete(path)
                    count++
                } catch (e: Exception) {
                    // 忽略删除失败的文件
                }
            }
        return count
    }

    fun saveTextAsJson(request: TextUploadRequest): String {
        val timestamp = LocalDateTime.now()

        val fileName = if (!request.filename.isNullOrEmpty()) {
            "${request.filename}.json"
        } else {
            val formattedTime = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val uuid = UUID.randomUUID().toString().substring(0, 8)
            val baseName =
                request.filename?.takeIf { it.isNotBlank() }?.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
                    ?: "text"
            "${baseName}_${formattedTime}_$uuid.json"
        }


        val filePath = storageDir.resolve(fileName)

        val data = mapOf(
            "content" to request.content,
            "timestamp" to timestamp.toString(),
            "originalFilename" to (request.filename ?: ""),
            "savedFileName" to fileName
        )

        objectMapper.writeValue(filePath.toFile(), data)
        return filePath.toString()
    }
}
