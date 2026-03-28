package com.book.analysis.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class BookAnalysisAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookAnalysisAssistantApplication.class, args);
    }

}
