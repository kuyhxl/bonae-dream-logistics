package com.bonae.logistics.company.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    /**
     * 외부 API 호출과 DB 트랜잭션 범위를 분리하기 위해
     * 코드 블록 단위로 트랜잭션을 제어할 수 있도록 TransactionTemplate 빈 등록
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
