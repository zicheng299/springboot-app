package com.example.demo.controller

import com.example.demo.dto.EmailRequest
import com.example.demo.service.EmailService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/email")
class EmailController(private val emailService: EmailService) {

    @PostMapping("/send")
    fun sendEmail(@RequestBody request: EmailRequest): ResponseEntity<Map<String, Any>> {
        return try {
            require(request.to.isNotBlank()) { "收件人邮箱不能为空" }
            require(request.subject.isNotBlank()) { "邮件主题不能为空" }
            require(request.text.isNotBlank()) { "邮件内容不能为空" }

            val sent = emailService.sendSimpleEmail(request)
            if (sent) {
                ResponseEntity.ok(mapOf("success" to true, "message" to "邮件发送成功"))
            } else {
                ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "邮件发送失败"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to (e.message ?: "请求参数错误")))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("success" to false, "message" to "邮件发送失败: ${e.message}"))
        }
    }
}
