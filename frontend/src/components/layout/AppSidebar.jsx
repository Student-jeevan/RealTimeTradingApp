import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '@/State/Auth/Action';
import { isAdmin } from '@/utils/roleUtils';
import {
  HomeIcon,
  WalletIcon,
  Bell,
  BookmarkIcon,
  CreditCardIcon,
  LandmarkIcon,
  Settings,
  LogOut,
  TrendingUp,
  BarChart3,
  Clock,
  Receipt,
  Search,
} from 'lucide-react';
import {
  ActivityLogIcon,
  DashboardIcon,
  PersonIcon,
} from '@radix-ui/react-icons';

const navGroups = [
  {
    label: 'Main',
    items: [
      { name: 'Dashboard', path: '/', icon: HomeIcon },
      { name: 'Portfolio', path: '/portfolio', icon: DashboardIcon },
      { name: 'Watchlist', path: '/watchlist', icon: BookmarkIcon },
    ],
  },
  {
    label: 'Trading',
    items: [
      { name: 'Activity', path: '/activity', icon: ActivityLogIcon },
      { name: 'Orders', path: '/orders', icon: Receipt },
      { name: 'Alerts', path: '/alerts', icon: Bell },
    ],
  },
  {
    label: 'Finance',
    items: [
      { name: 'Wallet', path: '/wallet', icon: WalletIcon },
      { name: 'Ledger', path: '/wallet/ledger', icon: BarChart3 },
      { name: 'Withdrawals', path: '/withdrawals', icon: CreditCardIcon },
      { name: 'Payment', path: '/payment-details', icon: LandmarkIcon },
    ],
  },
  {
    label: 'Account',
    items: [
      { name: 'Profile', path: '/profile', icon: PersonIcon },
    ],
  },
];

const AppSidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const auth = useSelector((state) => state.auth);

  const handleLogout = () => {
    dispatch(logout(navigate));
  };

  return (
    <aside className="fixed left-0 top-0 z-40 h-screen w-[240px] flex flex-col border-r border-border/40"
           style={{ background: 'hsl(var(--sidebar-bg))' }}>
      
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-border/30">
        <div className="w-9 h-9 rounded-xl gradient-primary flex items-center justify-center">
          <TrendingUp className="w-5 h-5 text-white" />
        </div>
        <div className="flex flex-col">
          <span className="font-bold text-sm gradient-text">CryptEx</span>
          <span className="text-[10px] text-muted-foreground tracking-widest uppercase">Trading</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
        {navGroups.map((group) => (
          <div key={group.label}>
            <p className="px-4 mb-2 text-[10px] font-semibold tracking-widest uppercase text-muted-foreground/60">
              {group.label}
            </p>
            <div className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname === item.path;
                return (
                  <button
                    key={item.name}
                    onClick={() => navigate(item.path)}
                    className={`sidebar-link w-full ${isActive ? 'active' : ''}`}
                  >
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* User / Logout */}
      <div className="border-t border-border/30 p-3">
        <button
          onClick={handleLogout}
          className="sidebar-link w-full text-red-400 hover:text-red-300 hover:bg-red-500/10"
        >
          <LogOut className="w-4 h-4" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};

export default AppSidebar;
