package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;

/**
 * An admin account waiting on the platform tenant was added to a company, which is the moment it
 * can sign in (GH-419). Published inside the assignment's transaction, so the welcome email goes
 * out only once it commits, and never for an assignment that rolled back.
 *
 * @param admin   the account, now on the company's tenant
 * @param company the company it was added to
 */
public record AdminTenantAssignedEvent(AppUser admin, Tenant company) {
}
