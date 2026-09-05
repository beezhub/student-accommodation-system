package com.beezhub.student_accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Programmatic transaction templates.
 *
 * <p>The primary template keeps Spring Boot's default semantics (PROPAGATION_REQUIRED), so a
 * callback joins any transaction already open on the thread. The {@link #REQUIRES_NEW} template
 * always suspends the caller's transaction and commits or rolls back on its own. Use it for
 * per-item work inside a batch, where one item's failure must not roll back the others, and for
 * database work triggered from an AFTER_COMMIT event listener, where the original transaction is
 * already committed and cannot be joined.
 */
@Configuration
public class TransactionTemplateConfig {

    /** Bean name of the template that always opens its own transaction. */
    public static final String REQUIRES_NEW = "requiresNewTransactionTemplate";

    @Bean
    @Primary
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean(REQUIRES_NEW)
    public TransactionTemplate requiresNewTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
