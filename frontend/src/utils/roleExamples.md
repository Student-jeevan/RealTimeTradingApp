# Role-Based Access Control Guide

This guide shows how to classify and handle admin vs user access in the React application.

## Overview

The backend uses `USER_ROLE` enum with two values:
- `ROLE_ADMIN` - Administrator users
- `ROLE_CUSTOMER` - Regular customer users

The user object (from Redux `auth.user`) contains a `role` field that you can use to check permissions.

## Utility Functions

Located in `frontend/src/utils/roleUtils.js`:

```javascript
import { isAdmin, isCustomer, getUserRole } from '@/utils/roleUtils';

// Check if user is admin
const user = useSelector(state => state.auth.user);
if (isAdmin(user)) {
  // Admin-only code
}

// Check if user is customer
if (isCustomer(user)) {
  // Customer-only code
}

// Get role as string
const role = getUserRole(user); // Returns 'ROLE_ADMIN' or 'ROLE_CUSTOMER'
```

## Using in Components

### 1. Conditional Rendering

```jsx
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';

function MyComponent() {
  const user = useSelector(state => state.auth.user);
  
  return (
    <div>
      {isAdmin(user) && (
        <Button>Admin Only Button</Button>
      )}
      
      {isAdmin(user) ? (
        <AdminPanel />
      ) : (
        <UserDashboard />
      )}
    </div>
  );
}
```

### 2. Protected Routes

```jsx
import ProtectedRoute from '@/components/ProtectedRoute';

// Require authentication only
<Route 
  path="/profile" 
  element={
    <ProtectedRoute requireAuth={true}>
      <Profile />
    </ProtectedRoute>
  }
/>

// Require admin role
<Route 
  path="/admin/dashboard" 
  element={
    <ProtectedRoute requireAuth={true} requireRole="admin">
      <AdminDashboard />
    </ProtectedRoute>
  }
/>

// Require customer role
<Route 
  path="/customer/orders" 
  element={
    <ProtectedRoute requireAuth={true} requireRole="customer">
      <Orders />
    </ProtectedRoute>
  }
/>
```

### 3. Navigation Links (Navbar/Sidebar)

```jsx
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';

function Navbar() {
  const user = useSelector(state => state.auth.user);
  
  return (
    <nav>
      <Link to="/home">Home</Link>
      <Link to="/portfolio">Portfolio</Link>
      
      {isAdmin(user) && (
        <>
          <Link to="/admin/withdrawals">Withdrawals</Link>
          <Link to="/admin/users">Users</Link>
        </>
      )}
    </nav>
  );
}
```

### 4. Redirect Based on Role

```jsx
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';

function WithdrawalPage() {
  const user = useSelector(state => state.auth.user);
  
  // Redirect admin to admin page
  if (isAdmin(user)) {
    return <Navigate to="/admin/withdrawals" replace />;
  }
  
  // Show user withdrawal page
  return <UserWithdrawal />;
}
```

### 5. API Calls with Role Checks

```jsx
import { useSelector } from 'react-redux';
import { isAdmin } from '@/utils/roleUtils';
import { getAllWithdrawals } from './WithdrawalService';

function WithdrawalsList() {
  const user = useSelector(state => state.auth.user);
  const [withdrawals, setWithdrawals] = useState([]);
  
  useEffect(() => {
    const loadData = async () => {
      try {
        if (isAdmin(user)) {
          // Admin sees all withdrawals
          const data = await getAllWithdrawals();
          setWithdrawals(data);
        } else {
          // User sees only their withdrawals
          const data = await getUserWithdrawalHistory();
          setWithdrawals(data);
        }
      } catch (error) {
        console.error('Error loading withdrawals:', error);
      }
    };
    
    if (user) {
      loadData();
    }
  }, [user]);
  
  return <div>{/* Render withdrawals */}</div>;
}
```

## Current Implementation

### Withdrawal Pages

- **User Withdrawal** (`/withdrawals`): 
  - Accessible to all authenticated users
  - Automatically redirects admins to admin page
  
- **Admin Withdrawal** (`/admin/withdrawals`):
  - Protected route requiring admin role
  - Shows all withdrawal requests
  - Allows approve/reject actions

### Route Structure

```jsx
// In App.jsx
<Route path='/withdrawals' element={<UserWithdrawal/>} />
<Route 
  path='/admin/withdrawals' 
  element={
    <ProtectedRoute requireAuth={true} requireRole="admin">
      <AdminWithdrawal/>
    </ProtectedRoute>
  }
/>
```

## Best Practices

1. **Always check for user existence** before checking role:
   ```jsx
   if (user && isAdmin(user)) { /* ... */ }
   ```

2. **Use ProtectedRoute** for route-level access control

3. **Handle loading states** when user data is being fetched

4. **Show appropriate fallbacks** when access is denied

5. **Log access attempts** for security auditing (optional)

## Testing Roles

To test admin access, you need to manually update a user's role in the database:

```sql
UPDATE User SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';
```

Or create a backend endpoint to change user roles (for development only).


