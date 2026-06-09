package com.example.demo.service

import com.example.demo.dto.EmailRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    @Value("\${spring.mail.username}")
    private lateinit var from: String

    fun sendSimpleEmail(request: EmailRequest): Boolean {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(request.to)
        message.subject = request.subject
        message.text = request.text
        mailSender.send(message)
        return true
    }
}
