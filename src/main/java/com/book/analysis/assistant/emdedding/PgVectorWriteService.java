package com.book.analysis.assistant.emdedding;

import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 将已计算的 embedding 向量写入 PG Vector 表.
 *
 * @author guoyb
 * @since 2026-03-12
 */
@Service
@Slf4j
public class PgVectorWriteService {

    private final JdbcTemplate vectorJdbcTemplate;
    private static final String SCHEMA_NAME = "public";
    private static final String TABLE_NAME = "vector_store";
    private static final String INSERT_SQL = "INSERT INTO %s.%s (content, metadata, embedding) VALUES (?, ?::json, ?)";

    public PgVectorWriteService(@Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate) {
        this.vectorJdbcTemplate = vectorJdbcTemplate;
    }

    /**
     * 写入单条文档及其向量
     */
    public void add(String content, Map<String, Object> metadata, float[] embedding) {
        String sql = String.format(INSERT_SQL, SCHEMA_NAME, TABLE_NAME);
        String metadataJson = metadata == null || metadata.isEmpty() ? "{}" : toJson(metadata);
        vectorJdbcTemplate.update(sql, content, metadataJson, new PGvector(embedding));
    }

    /**
     * 批量写入（逐条插入，保证顺序与文档一致）
     */
    public void addBatch(String[] contents, Map<String, Object>[] metadataArray, float[][] embeddings) {
        if (contents == null || embeddings == null || contents.length != embeddings.length) {
            throw new IllegalArgumentException("contents 与 embeddings 长度须一致");
        }
        String sql = String.format(INSERT_SQL, SCHEMA_NAME, TABLE_NAME);
        for (int i = 0; i < contents.length; i++) {
            Map<String, Object> meta = (metadataArray != null && i < metadataArray.length) ? metadataArray[i] : null;
            String metaJson = meta == null || meta.isEmpty() ? "{}" : toJson(meta);
            vectorJdbcTemplate.update(sql, contents[i], metaJson, new PGvector(embeddings[i]));
        }
        log.info("已写入 PG Vector {} 条", contents.length);
    }

    private static String toJson(Map<String, Object> metadata) {
        if (metadata == null) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : metadata.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof String) {
                sb.append("\"").append(escapeJson((String) v)).append("\"");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
