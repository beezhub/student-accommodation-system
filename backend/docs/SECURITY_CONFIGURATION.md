# Security Configuration - Role Hierarchy

## Overview
This document describes the role-based access control (RBAC) configuration implemented in the Student Accommodation System.

## Role Hierarchy

The application uses a three-tier role hierarchy:

```
ADMIN > LANDLORD > STUDENT
```

### Role Privileges

1. **ADMIN**
   - Has all privileges of LANDLORD and STUDENT
   - Can manage all users, properties, and system settings
   - Full access to admin endpoints

2. **LANDLORD**
   - Has all privileges of STUDENT
   - Can manage their own properties
   - Can view and manage applications for their properties

3. **STUDENT**
   - Basic user privileges
   - Can view properties
   - Can submit applications

### How Role Hierarchy Works

With role hierarchy configured, Spring Security automatically grants lower-level permissions to higher-level roles:

- If an endpoint requires `ROLE_STUDENT`, users with `ROLE_LANDLORD` or `ROLE_ADMIN` can also access it
- If an endpoint requires `ROLE_LANDLORD`, users with `ROLE_ADMIN` can also access it
- If an endpoint requires `ROLE_ADMIN`, only users with `ROLE_ADMIN` can access it

## Security Features Implemented

### 1. Method Security
- `@EnableMethodSecurity(prePostEnabled = true)` is enabled
- Use `@PreAuthorize` and `@PostAuthorize` annotations on controller methods
- Use `@Secured` annotation for role-based access control

### 2. JWT Authentication
- JWT tokens contain user roles in the `roles` claim
- Roles are automatically prefixed with `ROLE_` for Spring Security compatibility
- Stateless authentication using JWT bearer tokens

### 3. CORS Configuration
- CORS is configured to allow cross-origin requests
- Default allowed origins:
  - http://localhost:3000
  - http://localhost:4200
  - http://localhost:8080
- Credentials are allowed
- All standard HTTP methods are supported

### 4. Security Filter Chain
- Public endpoints:
  - `/api/auth/**` - Authentication endpoints
  - `/v3/api-docs/**` - OpenAPI documentation
  - `/swagger-ui/**` - Swagger UI
- All other endpoints require authentication

## Usage Examples

### Using @PreAuthorize Annotation

```java
// Admin only
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnly() { }

// Admin or Landlord
@PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
public ResponseEntity<?> adminOrLandlord() { }

// All authenticated users (Student, Landlord, Admin)
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<?> allUsers() { }

// Complex expression
@PreAuthorize("hasRole('ADMIN') or (hasRole('LANDLORD') and #id == authentication.principal.id)")
public ResponseEntity<?> complexSecurity(@PathVariable Long id) { }
```

### Using @Secured Annotation

```java
@Secured("ROLE_ADMIN")
public ResponseEntity<?> adminOnly() { }

@Secured({"ROLE_ADMIN", "ROLE_LANDLORD"})
public ResponseEntity<?> adminOrLandlord() { }
```

## Configuration Files

### SecurityConfig.java
- Configures role hierarchy
- Sets up CORS
- Configures security filter chain
- Enables method security

### JwtAuthenticationFilter.java
- Extracts JWT from Authorization header
- Validates token
- Extracts roles and sets authentication context
- Ensures roles have `ROLE_` prefix

### JwtUtil.java
- Generates JWT tokens with roles claim
- Extracts username and roles from tokens
- Validates token expiration

## Testing Role-Based Access

### Obtain JWT Token
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin@example.com",
  "password": "password"
}
```

### Use JWT Token in Requests
```bash
GET http://localhost:8080/api/admin/dashboard
Authorization: Bearer <your-jwt-token>
```

## Customization

### Update CORS Origins
Edit the `corsConfigurationSource()` method in `SecurityConfig.java`:

```java
configuration.setAllowedOrigins(Arrays.asList(
    "https://your-frontend-domain.com",
    "http://localhost:3000"
));
```

### Modify Role Hierarchy
Edit the `roleHierarchy()` method in `SecurityConfig.java`:

```java
String hierarchy = "ROLE_ADMIN > ROLE_LANDLORD \n ROLE_LANDLORD > ROLE_STUDENT";
```

### Add Custom Roles
1. Add new role to `UserRole` enum
2. Update role hierarchy in `SecurityConfig`
3. Use `@PreAuthorize` with new role name

## Best Practices

1. **Always use hasRole() with role name only** (without ROLE_ prefix)
   - ✅ `@PreAuthorize("hasRole('ADMIN')")`
   - ❌ `@PreAuthorize("hasRole('ROLE_ADMIN')")`

2. **Use hasAuthority() when you need the full authority name**
   - ✅ `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`

3. **Leverage role hierarchy** - Protect endpoints with the minimum required role

4. **Test security** - Write tests for each secured endpoint with different roles

5. **Keep CORS origins restrictive** - Only allow trusted domains in production

## Troubleshooting

### Issue: Access Denied despite having correct role
- Check if roles in JWT have `ROLE_` prefix
- Verify role hierarchy configuration
- Ensure `@EnableMethodSecurity` is enabled

### Issue: CORS errors
- Check allowed origins in CORS configuration
- Verify credentials are allowed if using cookies
- Check browser console for specific CORS error

### Issue: JWT token not being extracted
- Verify Authorization header format: `Bearer <token>`
- Check token expiration
- Ensure JWT secret matches between token generation and validation
