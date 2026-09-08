package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransactionTemplateConfigTest {

    private final TransactionTemplateConfig config = new TransactionTemplateConfig();
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @Test
    void transactionTemplate_isPrimaryAndKeepsRequiredPropagation() throws Exception {
        TransactionTemplate template = config.transactionTemplate(transactionManager);

        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
        assertThat(template.getTransactionManager()).isSameAs(transactionManager);
        assertThat(TransactionTemplateConfig.class
                .getMethod("transactionTemplate", PlatformTransactionManager.class)
                .isAnnotationPresent(Primary.class)).isTrue();
    }

    @Test
    void requiresNewTransactionTemplate_usesRequiresNewPropagation() {
        TransactionTemplate template = config.requiresNewTransactionTemplate(transactionManager);

        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(template.getTransactionManager()).isSameAs(transactionManager);
    }
}
