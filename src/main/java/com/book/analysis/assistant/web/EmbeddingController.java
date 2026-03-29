package com.book.analysis.assistant.web;

import com.book.analysis.assistant.emdedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author GB
 * @desc
 * @since 2026-03-11
 */
@Slf4j
@RestController
@RequestMapping("/api/embedding")
@CrossOrigin(origins = "*")
public class EmbeddingController {
    @Autowired
    private EmbeddingService embeddingService;



    /**
     * 浏览器上传 PDF：写入临时文件后走本地解析入库（标准 Web 无法传递客户端磁盘路径）。
     * 上传成功后会自动通过 AI 提取文档的元数据信息（domain、category、keywords、summary）。
     */
    @PostMapping(value = "/import/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importUpload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String originalName = file.getOriginalFilename();

        if (!originalName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().build();
        }

        // 去掉 .pdf 后缀，作为书名
        String bookName = originalName.substring(0, originalName.lastIndexOf('.'));

        // ⚠️ 清理非法字符（非常重要）
        bookName = bookName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5-_]", "_");

        // 限制长度（createTempFile 要求至少3个字符）
        if (bookName.length() < 3) {
            bookName = "book_" + bookName;
        }

        // 创建临时文件
        Path temp = null;
        try {
            temp = Files.createTempFile(bookName + "-", ".pdf");
            file.transferTo(temp.toFile());
            embeddingService.embeddingFile(temp.toAbsolutePath().toString());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("文件上传导入报错", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ex) {
                    log.warn("删除临时文件失败: {}", temp, ex);
                }
            }
        }
    }

}
