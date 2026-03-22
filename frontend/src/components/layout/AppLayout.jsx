import React, { useState } from 'react';
import AppSidebar from './AppSidebar';
import AppNavbar from './AppNavbar';

/**
 * Main application layout wrapper.
 * Provides fixed sidebar (desktop) + responsive overlay sidebar (mobile)
 * and a sticky top navbar.
 *
 * All authenticated pages render inside this layout.
 */
const AppLayout = ({ children }) => {
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background">
      {/* Desktop sidebar — fixed, always visible */}
      <div className="hidden lg:block">
        <AppSidebar />
      </div>

      {/* Mobile sidebar — overlay */}
      {isMobileSidebarOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-30 bg-black/60 backdrop-blur-sm lg:hidden"
            onClick={() => setIsMobileSidebarOpen(false)}
          />
          {/* Sidebar */}
          <div className="fixed inset-y-0 left-0 z-50 lg:hidden animate-fade-in-up">
            <AppSidebar />
          </div>
        </>
      )}

      {/* Main content area — offset by sidebar width on desktop */}
      <div className="lg:ml-[240px] min-h-screen flex flex-col">
        <AppNavbar
          onToggleMobileSidebar={() => setIsMobileSidebarOpen(!isMobileSidebarOpen)}
          isMobileSidebarOpen={isMobileSidebarOpen}
        />

        {/* Page content */}
        <main className="flex-1 p-4 lg:p-6 overflow-x-hidden overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
