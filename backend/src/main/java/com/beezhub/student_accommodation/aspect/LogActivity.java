package com.beezhub.student_accommodation.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically log method invocations to the activity log.
 *
 * Usage:
 * @LogActivity(action = "APPROVE", entityType = "APPLICATION")
 * public void approveApplication(Long id) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {

    /**
     * The action being performed (e.g., CREATE, UPDATE, DELETE, APPROVE, REJECT)
     */
    String action();

    /**
     * The type of entity being acted upon (e.g., APPLICATION, DOCUMENT, USER, ROOM)
     */
    String entityType();

    /**
     * Description template. Use {0}, {1}, etc. for method parameters.
     * Example: "Approved application {0}"
     */
    String description() default "";

    /**
     * Index of the entity ID parameter in the method signature
     * -1 means no entity ID will be logged
     */
    int entityIdParamIndex() default 0;
}
