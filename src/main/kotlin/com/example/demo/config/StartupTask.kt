package com.example.demo.config

import com.example.demo.dto.EmailRequest
import com.example.demo.service.CpolarService
import com.example.demo.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

//@Component
//@Order(1)
class StartupTask(
    private val cpolarService: CpolarService,
    private val emailService: EmailService
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(StartupTask::class.java)

    override fun run(vararg args: String?) {
        logger.info("===== 应用启动发送邮件任务开始 =====")
        val content = cpolarService.getCpolarHost()
        val bool = emailService.sendSimpleEmail(
            EmailRequest(
                to = "980643279@qq.com",
                subject = "Cpolar Website Url",
                text = content
            )
        )
        logger.info("===== 应用启动发送邮件任务完成: $bool =====")
    }
}
