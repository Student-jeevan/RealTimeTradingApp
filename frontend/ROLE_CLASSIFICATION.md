# Admin and User Classification Guide

This document explains how to classify and handle admin vs user roles in the Trading Application.

## Overview

The application uses a role-based access control (RBAC) system with two roles:
- **ROLE_ADMIN** - Administrator users with full access
- **ROLE_CUSTOMER** - Regular customer users with limited access

## Backend Role Structure

### User Model
```java
@Entity
public class User {
    private USER_ROLE role = USER_ROLE.ROLE_CUSTOMER; // Default role
    // ... other fields
}
```

### Role Enum
```java
public enum USER_ROLE {
    ROLE_ADMIN, ROLE_CUSTOMER
}
```

The user's role is returned in the user profile response (`GET /api/users/profile`).

## Frontend Implementation

### 1. Role Utility Functions

**Location:** `frontend/src/utils/roleUtils.js`

```javascript
import { isAdmin, isCustomer, getUserRole } from '@/utils/roleUtils';

// Check if user is admin
const user = useSelector(state => state.auth.user);
if (isAdmin(user)) {
  // Admin code
}
```

**Functions:**
- `isAdmin(user)` - Returns `true` if user role is `ROLE_ADMIN`
- `isCustomer(user)` - Returns `true` if user role is `ROLE_CUSTOMER`
- `getUserRole(user)` - Returns the role as string

### 2. Protected Route Component

**Location:** `frontend/src/components/ProtectedRoute.jsx`

Use this component to protect routes based on authentication and role:

```jsx
import ProtectedRoute from '@/components/ProtectedRoute';

<Route 
  path="/admin/withdrawals" 
  element={
    <ProtectedRoute requireAuth={true} requireRole="admin">
      <AdminWithdrawal />
    </ProtectedRoute>
  }
/>
```

**Props:**
- `requireAuth` - Require authentication (default: `true`)
- `requireRole` - Required role: `'admin'`, `'customer'`, or `null` for any authenticated user
- `redirectTo` - Route to redirect if access denied (default: `'/'`)

### 3. Using in Components

#### Conditional Rendering
```jsx
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';

function MyComponent() {
  const user = useSelector(state => state.auth.user);
  
  return (
    <div>
      {isAdmin(user) && <AdminPanel />}
      {isAdmin(user) ? <AdminView /> : <UserView />}
    </div>
  );
}
```

#### Redirect Based on Role
```jsx
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';

function WithdrawalPage() {
  const user = useSelector(state => state.auth.user);
  
  if (isAdmin(user)) {
    return <Navigate to="/admin/withdrawals" replace />;
  }
  
  return <UserWithdrawal />;
}
```

### 4. Route Configuration

**Location:** `frontend/src/App.jsx`

```jsx
<Routes>
  {/* User routes */}
  <Route path='/withdrawals' element={<UserWithdrawal />} />
  
  {/* Admin routes */}
  <Route 
    path='/admin/withdrawals' 
    element={
      <ProtectedRoute requireAuth={true} requireRole="admin">
        <AdminWithdrawal />
      </ProtectedRoute>
    }
  />
</Routes>
```

### 5. Navigation Menu

**Location:** `frontend/src/pages/Navbar/Sidebar.jsx`

The sidebar automatically shows/hides menu items based on user role:

```jsx
const menu = [
  ...baseMenu, // Available to all users
  ...(isAdmin(user) ? adminMenu : []), // Admin-only items
];
```

## Current Implementation Examples

### Withdrawal Pages

1. **UserWithdrawal.jsx** (`/withdrawals`)
   - Automatically redirects admins to admin page
   - Shows withdrawal request form and user's history
   - Accessible to all authenticated users

2. **AdminWithdrawal.jsx** (`/admin/withdrawals`)
   - Protected route requiring admin role
   - Shows all withdrawal requests from all users
   - Allows approve/reject actions
   - Includes summary statistics

### Role Check Flow

```
User Login → Fetch User Profile → Store in Redux (auth.user)
                                    ↓
Check role using isAdmin(user) or isCustomer(user)
                                    ↓
Conditionally render UI or redirect
```

## Testing Roles

### Method 1: Database Update
```sql
-- Make a user admin
UPDATE User SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';

-- Make a user customer
UPDATE User SET role = 'ROLE_CUSTOMER' WHERE email = 'user@example.com';
```

### Method 2: Backend Endpoint (Development Only)
Create a temporary endpoint to change roles for testing:

```java
@PatchMapping("/api/admin/users/{userId}/role/{role}")
public ResponseEntity<User> changeUserRole(
    @PathVariable Long userId,
    @PathVariable USER_ROLE role,
    @RequestHeader("Authorization") String jwt
) throws Exception {
    // Implementation
}
```

## Security Best Practices

1. **Always check role on frontend AND backend**
   - Frontend checks are for UX only
   - Backend must enforce actual security

2. **Never trust client-side role checks**
   - Always verify role in backend API endpoints
   - Use JWT claims or database lookups

3. **Handle loading states**
   ```jsx
   if (!user) {
     return <Loading />;
   }
   ```

4. **Provide clear error messages**
   ```jsx
   if (!isAdmin(user)) {
     return <AccessDenied message="Admin access required" />;
   }
   ```

## File Structure

```
frontend/src/
├── utils/
│   └── roleUtils.js          # Role checking utilities
├── components/
│   └── ProtectedRoute.jsx    # Route protection component
├── pages/
│   ├── Withdrawals/
│   │   ├── UserWithdrawal.jsx    # User withdrawal page
│   │   └── AdminWithdrawal.jsx   # Admin withdrawal page
│   └── Navbar/
│       └── Sidebar.jsx           # Role-based menu
└── App.jsx                       # Route configuration
```

## Quick Reference

| Task | Code |
|------|------|
| Check if admin | `isAdmin(user)` |
| Check if customer | `isCustomer(user)` |
| Get role string | `getUserRole(user)` |
| Protect route | `<ProtectedRoute requireRole="admin">...</ProtectedRoute>` |
| Conditional render | `{isAdmin(user) && <Component />}` |
| Redirect admin | `if (isAdmin(user)) return <Navigate to="/admin/..." />` |

## Troubleshooting

**Problem:** Role check always returns false
- **Solution:** Ensure user profile is loaded: `auth.user` should not be null

**Problem:** Admin routes not accessible
- **Solution:** Check if user.role === 'ROLE_ADMIN' in Redux state

**Problem:** Menu items not showing
- **Solution:** Verify `isAdmin(user)` is working and user data is loaded

For more examples, see `frontend/src/utils/roleExamples.md`

