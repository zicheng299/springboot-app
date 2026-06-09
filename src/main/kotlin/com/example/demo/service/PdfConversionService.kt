package com.example.demo.service

import com.example.demo.dto.PdfPageImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

@Service
class PdfConversionService {

    private val outputDir: Path = Paths.get("pdfImages").toAbsolutePath()
    private val dpi: Float = 150f

    init {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    fun convertPdfToImages(file: MultipartFile): List<PdfPageImage> {
        if (file.isEmpty) {
            throw IllegalArgumentException("PDF 文件不能为空")
        }

        val originalFilename = file.originalFilename ?: "document.pdf"
        val baseName = originalFilename.substringBeforeLast(".", "document")
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val uuid = UUID.randomUUID().toString().substring(0, 8)

        val tempFile = File.createTempFile("pdf_upload_", ".pdf")
        file.transferTo(tempFile)

        val results = mutableListOf<PdfPageImage>()

        PDDocument.load(tempFile).use { document ->
            val renderer = PDFRenderer(document)
            val pageCount = document.numberOfPages

            for (pageIndex in 0 until pageCount) {
                val image: BufferedImage = renderer.renderImageWithDPI(pageIndex, dpi)
                val savedFileName = "${baseName}_page${pageIndex + 1}_${timestamp}_${uuid}.png"
                val targetPath = outputDir.resolve(savedFileName)

                ImageIO.write(image, "PNG", targetPath.toFile())

                results.add(
                    PdfPageImage(
                        pageNumber = pageIndex + 1,
                        savedFileName = savedFileName,
                        savedFilePath = targetPath.toString(),
                        fileSize = Files.size(targetPath)
                    )
                )
            }
        }

        tempFile.delete()
        return results
    }

    fun convertPdfToZip(file: MultipartFile): Path {
        val images = convertPdfToImages(file)
        if (images.isEmpty()) {
            throw IllegalStateException("未生成任何图片")
        }

        val zipFileName = "pdf_images_${System.currentTimeMillis()}.zip"
        val zipPath = outputDir.resolve(zipFileName)

        ZipOutputStream(Files.newOutputStream(zipPath)).use { zos ->
            for (image in images) {
                zos.putNextEntry(ZipEntry(image.savedFileName))
                Files.copy(Paths.get(image.savedFilePath), zos)
                zos.closeEntry()
            }
        }

        return zipPath
    }
}
