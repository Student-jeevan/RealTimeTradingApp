/**
 * Utility functions for role-based access control
 */

/**
 * Check if the current user is an admin
 * @param {Object} user - User object from Redux auth state
 * @returns {boolean} True if user is admin, false otherwise
 */
export const isAdmin = (user) => {
  if (!user || !user.role) {
    return false;
  }
  return user.role === 'ROLE_ADMIN' || user.role === 'ROLE_ADMIN';
};

/**
 * Check if the current user is a regular customer
 * @param {Object} user - User object from Redux auth state
 * @returns {boolean} True if user is customer, false otherwise
 */
export const isCustomer = (user) => {
  if (!user || !user.role) {
    return false;
  }
  return user.role === 'ROLE_CUSTOMER';
};

/**
 * Get user role as string
 * @param {Object} user - User object from Redux auth state
 * @returns {string} User role or 'UNKNOWN'
 */
export const getUserRole = (user) => {
  if (!user || !user.role) {
    return 'UNKNOWN';
  }
  return user.role;
};



