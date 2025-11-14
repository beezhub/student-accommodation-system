# Quick Reference: Role-Based Access Control

## Role Hierarchy
```
ADMIN > LANDLORD > STUDENT
```

## Common Annotations

### Single Role
```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasRole('LANDLORD')")
@PreAuthorize("hasRole('STUDENT')")
```

### Multiple Roles
```java
@PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
@PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD', 'STUDENT')")
```

### Complex Expressions
```java
// Admin or owner of resource
@PreAuthorize("hasRole('ADMIN') or (#id == authentication.principal.id)")

// Admin or Landlord with specific condition
@PreAuthorize("hasRole('ADMIN') or (hasRole('LANDLORD') and #property.ownerId == authentication.principal.id)")
```

## CORS Origins (Development)
- http://localhost:3000
- http://localhost:4200
- http://localhost:8080

## JWT Token Usage
```bash
# Get token
POST /api/auth/login
Content-Type: application/json
{
  "username": "user@example.com",
  "password": "password"
}

# Use token
GET /api/admin/dashboard
Authorization: Bearer <your-jwt-token>
```

## Key Files
- `SecurityConfig.java` - Main configuration
- `JwtAuthenticationFilter.java` - JWT processing
- `SECURITY_CONFIGURATION.md` - Full documentation
- `AdminController.java` - Usage examples

## Remember
- Use `hasRole('ADMIN')` NOT `hasRole('ROLE_ADMIN')`
- Role hierarchy is automatic
- All roles need ROLE_ prefix in JWT
- CORS is enabled for all endpoints
