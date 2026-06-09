package com.example.demo.controller

import com.example.demo.service.PdfConversionService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@RestController
@RequestMapping("/api/pdf")
class PdfConversionController(private val pdfConversionService: PdfConversionService) {

    @PostMapping("/to-images")
    fun convertPdfToImages(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<InputStreamResource> {
        require(!file.isEmpty) { "PDF 文件不能为空" }
        val originalFilename = file.originalFilename ?: ""
        require(originalFilename.endsWith(".pdf", ignoreCase = true)) { "请上传 PDF 格式文件" }

        val zipPath = pdfConversionService.convertPdfToZip(file)

        val resource = InputStreamResource(Files.newInputStream(zipPath))
        val zipFileName = zipPath.fileName.toString()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$zipFileName\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(Files.size(zipPath))
            .body(resource)
    }
}
