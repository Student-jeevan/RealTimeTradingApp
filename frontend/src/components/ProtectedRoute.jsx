import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { isAdmin, isCustomer } from '@/utils/roleUtils';

/**
 * Protected Route Component
 * Controls access to routes based on user authentication and role
 * 
 * @param {Object} props
 * @param {React.ReactNode} props.children - Component to render if access is granted
 * @param {boolean} props.requireAuth - Whether authentication is required (default: true)
 * @param {string} props.requireRole - Required role: 'admin', 'customer', or null for any authenticated user
 * @param {string} props.redirectTo - Route to redirect to if access is denied (default: '/')
 */
const ProtectedRoute = ({ 
  children, 
  requireAuth = true, 
  requireRole = null,
  redirectTo = '/' 
}) => {
  const auth = useSelector((state) => state.auth);
  const user = auth.user;

  // Check if authentication is required
  if (requireAuth && !user) {
    return <Navigate to="/auth" replace />;
  }

  // Check role-based access
  if (requireRole === 'admin' && !isAdmin(user)) {
    return <Navigate to={redirectTo} replace />;
  }

  if (requireRole === 'customer' && !isCustomer(user)) {
    return <Navigate to={redirectTo} replace />;
  }

  return children;
};

export default ProtectedRoute;



