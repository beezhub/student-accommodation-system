package com.beezhub.student_accommodation.model.entity;

public interface TenantAware {
    void setTenantId(Long tenantId);
    Long getTenantId();
}
