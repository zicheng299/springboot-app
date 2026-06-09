package com.example.demo.controller

import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

@RestController
@RequestMapping("/api/files")
class DownloadController {

    private val downloadsDir: Path = Paths.get("C:/Users/bryce/Downloads").toAbsolutePath()

    data class FileItem(
        val name: String,
        val size: Long,
        val lastModified: String
    )

    @GetMapping("/list")
    fun listFiles(): ResponseEntity<List<FileItem>> {
        val dir = downloadsDir.toFile()
        if (!dir.isDirectory) {
            return ResponseEntity.ok(emptyList())
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val files = dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                FileItem(
                    name = file.name,
                    size = file.length(),
                    lastModified = dateFormat.format(Date(file.lastModified()))
                )
            }
            ?: emptyList()

        return ResponseEntity.ok(files)
    }

    @GetMapping("/download")
    fun downloadFile(@RequestParam("name") fileName: String): ResponseEntity<InputStreamResource> {
        val filePath = downloadsDir.resolve(fileName).normalize()

        // 防目录穿越
        if (!filePath.startsWith(downloadsDir)) {
            return ResponseEntity.badRequest().build()
        }
        if (!Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build()
        }

        val resource = InputStreamResource(Files.newInputStream(filePath))
        val encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedName")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(Files.size(filePath))
            .body(resource)
    }

    @DeleteMapping("/delete")
    fun deleteFile(@RequestParam("name") fileName: String): ResponseEntity<Map<String, Any>> {
        val filePath = downloadsDir.resolve(fileName).normalize()

        if (!filePath.startsWith(downloadsDir)) {
            return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "非法路径"))
        }
        if (!Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build()
        }

        return try {
            Files.delete(filePath)
            ResponseEntity.ok(mapOf("success" to true, "message" to "删除成功"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to "删除失败: ${e.message}"))
        }
    }
}
