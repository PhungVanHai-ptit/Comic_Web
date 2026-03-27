# 📝 Authentication Refactoring - Important Note

## Correction to Refactoring

During the refactoring, I consolidated authentication functionality into `AuthService` and `AuthController`. However, there's an important distinction regarding `UserService`:

### ✅ Corrected Design

**UserService** has TWO responsibilities:
1. **User Management (Admin)** - Create, read, update, delete users
2. **Authentication Support** - Registration during signup is delegated to AuthService

However, for **admin user creation** (via `/users/new` endpoint), we need `UserService.createUser()` because:
- Administrators should be able to create users through the admin panel
- This is separate from user self-registration

### Architecture

```
AUTHENTICATION FLOW (User Registration):
User → GET /register → ViewController
        POST /auth/register → AuthController → AuthService.registerUser()

ADMIN USER MANAGEMENT:
Admin → GET /users/new → UserController
        POST /users → UserController → UserService.createUser()
```

### Clarification

**Two different `createUser()` methods:**

1. **AuthService.registerUser()** - User self-registration
   - Called when users sign up themselves
   - Uses `/auth/register` endpoint
   - Part of public authentication flow

2. **UserService.createUser()** - Admin user creation
   - Called when administrators create users
   - Uses `/users` endpoint (POST)
   - Part of admin management interface

### Updated UserService

The `UserService` still contains:
- ✅ `createUser()` - For admin user creation
- ✅ `findAll()` - List all users
- ✅ `findById()` - Get user details
- ✅ `updateUser()` - Edit user
- ✅ `deleteById()` - Delete user

### Why Both Exist?

| Context | Use | Method | Endpoint |
|---------|-----|--------|----------|
| **Self Registration** | User signs up themselves | AuthService.registerUser() | POST /auth/register |
| **Admin Creation** | Admin creates user | UserService.createUser() | POST /users |

Both use the same underlying logic but are called from different contexts with different authorization levels.

---

**Updated:** March 19, 2026  
**Status:** ✅ Compilation error fixed
