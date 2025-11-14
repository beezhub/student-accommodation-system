# Database Role Integration Flow

## Complete Authentication & Authorization Flow

### 1. User Registration/Signup
```
SignupRequest (with role)
    ↓
AuthService.signup()
    ↓
AppUser entity saved to DB with UserRole enum
    ↓
AppUserDetails created with role.name() 
    ↓
JWT generated with role in claims (e.g., "ADMIN", "LANDLORD", "STUDENT")
    ↓
Token returned to client
```

### 2. User Login
```
Login credentials
    ↓
AuthenticationManager.authenticate()
    ↓
AppUserDetailsService.loadUserByUsername()
    ↓
Query DB: SELECT * FROM app_user WHERE email = ?
    ↓
AppUser retrieved with UserRole from DB
    ↓
AppUserDetails created:
    - getAuthorities() adds "ROLE_" prefix
    - Returns [ROLE_ADMIN] or [ROLE_LANDLORD] or [ROLE_STUDENT]
    ↓
Authentication successful
    ↓
JwtUtil.generateToken(UserDetails)
    - Extracts authorities: ["ROLE_ADMIN"]
    - Stores in JWT claim: "roles": ["ROLE_ADMIN"]
    ↓
Token returned to client
```

### 3. Authenticated Request
```
Client sends: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ↓
Extract JWT from header
    ↓
JwtUtil.validateToken(jwt, username)
    ↓
JwtUtil.extractRoles(jwt)
    - Retrieves roles from JWT claims
    - Returns ["ROLE_ADMIN"] or ["ADMIN"]
    ↓
JwtAuthenticationFilter adds ROLE_ prefix if missing
    - "ADMIN" → "ROLE_ADMIN"
    - "ROLE_ADMIN" → "ROLE_ADMIN" (no change)
    ↓
Create UsernamePasswordAuthenticationToken with authorities
    ↓
SecurityContextHolder.setAuthentication()
    ↓
RoleHierarchy applied:
    - ROLE_ADMIN → [ROLE_ADMIN, ROLE_LANDLORD, ROLE_STUDENT]
    - ROLE_LANDLORD → [ROLE_LANDLORD, ROLE_STUDENT]
    - ROLE_STUDENT → [ROLE_STUDENT]
    ↓
@PreAuthorize evaluation with hierarchy
    ↓
Access granted/denied based on role hierarchy
```

## Database Schema

### app_user table
```sql
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL,  -- 'ADMIN', 'LANDLORD', or 'STUDENT'
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Code Components

### 1. AppUser Entity
```java
@Entity(name = "app_user")
public class AppUser {
    @Column(nullable = false, name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole userRole;  // Stored as: ADMIN, LANDLORD, STUDENT
}
```

### 2. UserRole Enum
```java
public enum UserRole {
    STUDENT("Student"),
    ADMIN("Admin"),
    LANDLORD("Landlord");
}
```

### 3. AppUserDetails (Converts DB role to Spring Security authority)
```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    // Converts: ADMIN → ROLE_ADMIN
    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    return Collections.singleton(new SimpleGrantedAuthority(authority));
}
```

### 4. JWT Token Structure
```json
{
  "sub": "user@example.com",
  "roles": ["ROLE_ADMIN"],
  "iat": 1699719600,
  "exp": 1699806000
}
```

### 5. JwtAuthenticationFilter (Extracts roles from JWT)
```java
var roles = jwtUtil.extractRoles(jwt);
var authorities = roles.stream()
    .map(role -> {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return new SimpleGrantedAuthority(roleWithPrefix);
    })
    .toList();
```

### 6. SecurityConfig (Applies role hierarchy)
```java
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_ADMIN > ROLE_LANDLORD \n ROLE_LANDLORD > ROLE_STUDENT"
    );
}
```

## Role Hierarchy Behavior

### Example 1: Admin User
```
DB: user_role = 'ADMIN'
    ↓
AppUserDetails: authorities = [ROLE_ADMIN]
    ↓
JWT: "roles": ["ROLE_ADMIN"]
    ↓
RoleHierarchy: [ROLE_ADMIN, ROLE_LANDLORD, ROLE_STUDENT]
    ↓
Can access:
    ✅ @PreAuthorize("hasRole('ADMIN')")
    ✅ @PreAuthorize("hasRole('LANDLORD')")
    ✅ @PreAuthorize("hasRole('STUDENT')")
```

### Example 2: Landlord User
```
DB: user_role = 'LANDLORD'
    ↓
AppUserDetails: authorities = [ROLE_LANDLORD]
    ↓
JWT: "roles": ["ROLE_LANDLORD"]
    ↓
RoleHierarchy: [ROLE_LANDLORD, ROLE_STUDENT]
    ↓
Can access:
    ❌ @PreAuthorize("hasRole('ADMIN')")
    ✅ @PreAuthorize("hasRole('LANDLORD')")
    ✅ @PreAuthorize("hasRole('STUDENT')")
```

### Example 3: Student User
```
DB: user_role = 'STUDENT'
    ↓
AppUserDetails: authorities = [ROLE_STUDENT]
    ↓
JWT: "roles": ["ROLE_STUDENT"]
    ↓
RoleHierarchy: [ROLE_STUDENT]
    ↓
Can access:
    ❌ @PreAuthorize("hasRole('ADMIN')")
    ❌ @PreAuthorize("hasRole('LANDLORD')")
    ✅ @PreAuthorize("hasRole('STUDENT')")
```

## Key Points

1. **Single Source of Truth**: User roles are stored in the database (`app_user.user_role`)

2. **Automatic Prefix Handling**: Both `AppUserDetails` and `JwtAuthenticationFilter` ensure the `ROLE_` prefix is added

3. **Stateless Authentication**: 
   - Initial login queries the database
   - Subsequent requests use JWT (no DB query)
   - Roles are embedded in JWT claims

4. **Role Hierarchy Applied**: Spring Security automatically applies hierarchy when evaluating `@PreAuthorize` annotations

5. **Backward Compatible**: System handles roles with or without `ROLE_` prefix

## Testing Role Changes

To change a user's role in the database:

```sql
-- Promote user to ADMIN
UPDATE app_user SET user_role = 'ADMIN', updated_at = NOW() 
WHERE email = 'user@example.com';

-- Demote admin to LANDLORD
UPDATE app_user SET user_role = 'LANDLORD', updated_at = NOW() 
WHERE email = 'admin@example.com';
```

**Note**: User must log in again to get a new JWT with the updated role.

## Security Considerations

1. **JWT Lifetime**: Roles in JWT don't update until token expires and user re-authenticates
2. **Role Changes**: Require user to log out/in or implement token refresh mechanism
3. **Database Constraints**: `user_role` is NOT NULL and must be valid enum value
4. **Immutable Tokens**: Once issued, JWT roles cannot be changed until expiration

## Integration Testing

```java
// Test user with ADMIN role from DB can access all endpoints
@Test
void adminFromDatabaseCanAccessAllEndpoints() {
    // Create user in DB with ADMIN role
    AppUser admin = createUserInDB("admin@test.com", UserRole.ADMIN);
    
    // Login and get JWT
    String token = loginAndGetToken("admin@test.com", "password");
    
    // Test access to all role-protected endpoints
    testEndpointAccess(token, "/api/admin/dashboard", 200);
    testEndpointAccess(token, "/api/landlord/properties", 200);
    testEndpointAccess(token, "/api/student/profile", 200);
}
```
