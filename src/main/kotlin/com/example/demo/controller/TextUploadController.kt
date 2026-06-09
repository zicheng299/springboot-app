package com.example.demo.controller

import com.example.demo.dto.RecentTextFileDto
import com.example.demo.dto.TextUploadRequest
import com.example.demo.dto.TextUploadResponse
import com.example.demo.service.TextStorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/text")
class TextUploadController(private val textStorageService: TextStorageService) {

    @DeleteMapping("/{fileName}")
    fun deleteTextFile(@PathVariable fileName: String): ResponseEntity<Map<String, Any>> {
        return try {
            val deleted = textStorageService.deleteFile(fileName)
            if (deleted) {
                ResponseEntity.ok(mapOf("success" to true, "message" to "删除成功"))
            } else {
                ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "文件不存在或无法删除"))
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to "删除失败: ${e.message}"))
        }
    }

    @DeleteMapping
    fun clearAllTextFiles(): ResponseEntity<Map<String, Any>> {
        return try {
            val count = textStorageService.clearAllFiles()
            ResponseEntity.ok(mapOf("success" to true, "message" to "已清空 $count 个文件", "count" to count))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to "清空失败: ${e.message}"))
        }
    }

    @GetMapping("/recent")
    fun getRecentFiles(): ResponseEntity<List<RecentTextFileDto>> {
        return try {
            val files = textStorageService.getRecentFiles(limit = 100)
            ResponseEntity.ok(files)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(emptyList())
        }
    }

    @PostMapping("/upload")
    fun uploadText(@RequestBody request: TextUploadRequest): ResponseEntity<TextUploadResponse> {
        return try {
            require(request.content.isNotBlank()) { "上传文本内容不能为空" }
            val savedPath = textStorageService.saveTextAsJson(request)
            ResponseEntity.ok(
                TextUploadResponse(
                    success = true,
                    message = "文本已成功保存为 JSON 文件",
                    savedFilePath = savedPath
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                TextUploadResponse(
                    success = false,
                    message = e.message ?: "请求参数错误"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                TextUploadResponse(
                    success = false,
                    message = "保存失败: ${e.message}"
                )
            )
        }
    }
}
