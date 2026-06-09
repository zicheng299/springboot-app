package com.example.demo.controller

import com.example.demo.dto.TextUploadRequest
import com.example.demo.service.CpolarService
import com.example.demo.service.TextStorageService
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Base64

@RestController
@RequestMapping("/api/web")
class HttpController(
    private val cpolarService: CpolarService,
    private val textStorageService: TextStorageService,
    private val env: Environment
) {

    @GetMapping("/refresh")
    fun refresh(): ResponseEntity<Map<String, Any>> {
        val urls = cpolarService.getUrls()
        return if (urls.isNotEmpty()) {
            urls.forEach { (name, url) ->
                val formatUrl = url.replace("tcp://", "")
                textStorageService.saveTextAsJson(TextUploadRequest(formatUrl, name))
            }
            ResponseEntity.ok(mapOf("success" to true, "message" to "已刷新 ${urls.size} 个隧道"))
        } else {
            ResponseEntity.ok(mapOf("success" to true, "message" to "未获取到隧道数据，请确认 cpolar 已启动"))
        }
    }

    @GetMapping("/remote-desktop")
    fun getRemoteDesktopHost(): ResponseEntity<String> {
        return try {
            val host = cpolarService.getRemoteDesktopHost()
            ResponseEntity.ok(host)
        } catch (e: Exception) {
            ResponseEntity.ok(e.message ?: "获取远程桌面地址失败")
        }
    }

    @GetMapping("/local-ip-qrcode")
    fun getLocalIpQrCode(): ResponseEntity<Map<String, Any>> {
        return try {
            val ip = getLocalIpAddress()
            val port = env.getProperty("server.port") ?: "28080"
            val url = "http://$ip:$port"
            val qrBase64 = generateQRCode(url)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "ip" to ip,
                "port" to port,
                "url" to url,
                "qrBase64" to qrBase64
            ))
        } catch (e: Exception) {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "message" to (e.message ?: "获取局域网地址失败")
            ))
        }
    }

    private fun getLocalIpAddress(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    return address.hostAddress
                }
            }
        }
        return "127.0.0.1"
    }

    private fun generateQRCode(text: String, width: Int = 200, height: Int = 200): String {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
        val output = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", output)
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }
}
