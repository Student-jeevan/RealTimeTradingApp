import React from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Bell, Search, Menu, X } from 'lucide-react';

const AppNavbar = ({ onToggleMobileSidebar, isMobileSidebarOpen }) => {
  const auth = useSelector((state) => state.auth);
  const navigate = useNavigate();

  return (
    <header
      className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 lg:px-6 border-b border-border/40"
      style={{ background: 'hsla(222, 47%, 5.5%, 0.85)', backdropFilter: 'blur(12px)' }}
    >
      {/* Left: Mobile menu + Search */}
      <div className="flex items-center gap-3">
        {/* Mobile hamburger */}
        <button
          onClick={onToggleMobileSidebar}
          className="lg:hidden p-2 rounded-lg hover:bg-secondary transition-colors"
        >
          {isMobileSidebarOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>

        {/* Search bar */}
        <button
          onClick={() => navigate('/search')}
          className="flex items-center gap-2 px-3 py-2 rounded-xl bg-secondary/50 hover:bg-secondary transition-colors text-muted-foreground text-sm min-w-[180px] lg:min-w-[280px]"
        >
          <Search className="w-4 h-4" />
          <span>Search coins...</span>
          <kbd className="hidden md:inline-flex ml-auto px-1.5 py-0.5 text-[10px] font-medium rounded bg-muted text-muted-foreground">
            Ctrl+K
          </kbd>
        </button>
      </div>

      {/* Right: Notifications + Profile */}
      <div className="flex items-center gap-2">
        {/* Notifications */}
        <Button
          variant="ghost"
          size="icon"
          className="rounded-xl relative hover:bg-secondary"
          onClick={() => navigate('/alerts')}
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-blue-500 ring-2 ring-background" />
        </Button>

        {/* Profile */}
        <button
          onClick={() => navigate('/profile')}
          className="flex items-center gap-2 px-2 py-1.5 rounded-xl hover:bg-secondary transition-colors"
        >
          <Avatar className="h-8 w-8 ring-2 ring-primary/20">
            <AvatarFallback className="bg-gradient-to-br from-blue-500 to-purple-600 text-white text-xs font-semibold">
              {auth.user?.fullName?.[0]?.toUpperCase() || 'U'}
            </AvatarFallback>
          </Avatar>
          <div className="hidden md:flex flex-col text-left">
            <span className="text-xs font-medium leading-tight">
              {auth.user?.fullName || 'User'}
            </span>
            <span className="text-[10px] text-muted-foreground leading-tight">
              {auth.user?.email?.split('@')[0] || 'account'}
            </span>
          </div>
        </button>
      </div>
    </header>
  );
};

export default AppNavbar;
