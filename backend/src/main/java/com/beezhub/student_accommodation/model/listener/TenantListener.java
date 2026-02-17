package com.beezhub.student_accommodation.model.listener;

import com.beezhub.student_accommodation.model.entity.TenantAware;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantListener {

    @PrePersist
    @PreUpdate
    public void setTenantId(Object entity) {
        if (entity instanceof TenantAware) {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                TenantAware tenantAware = (TenantAware) entity;
                if (tenantAware.getTenantId() == null) {
                    tenantAware.setTenantId(tenantId);
                }
            }
        }
    }
}
