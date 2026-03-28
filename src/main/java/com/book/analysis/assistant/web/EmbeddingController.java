package com.book.analysis.assistant.web;

import com.book.analysis.assistant.emdedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author GB
 * @desc
 * @since 2026-03-11
 */
@Slf4j
@RestController
@RequestMapping("/api/embedding")
public class EmbeddingController {
    @Autowired
    private EmbeddingService embeddingService;


    /**
     * 文件导入接口
     * 从上传的文件导入文档
     *
     * @return 导入结果
     */
    @GetMapping("/import/file")
    public ResponseEntity<Void> importFile(@RequestParam("file") String filePath) {
        try {
            embeddingService.embeddingFile(filePath);
            return ResponseEntity.ok(Void.class.newInstance());
        } catch (Exception e) {
            log.error("文件导入报错", e);
            return ResponseEntity.ok(null);
        }
    }

}
