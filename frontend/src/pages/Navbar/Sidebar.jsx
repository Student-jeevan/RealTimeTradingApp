import React from 'react';
import { useSelector } from 'react-redux';
import { Button } from '@/components/ui/button';
import { SheetClose } from '@/components/ui/sheet';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { logout } from '@/State/Auth/Action';
import { isAdmin } from '@/utils/roleUtils';
import { 
  BookmarkIcon, 
  CreditCardIcon, 
  HomeIcon, 
  LandmarkIcon, 
  WalletIcon
} from 'lucide-react';
import { 
  ActivityLogIcon, 
  DashboardIcon, 
  ExitIcon, 
  PersonIcon 
} from '@radix-ui/react-icons';

// Base menu items for all users
const baseMenu = [
  { name: "Home", path: "/", icon: <HomeIcon className="h-6 w-6" /> },
  { name: "Portfolio", path: "/portfolio", icon: <DashboardIcon className="h-6 w-6" /> },
  { name: "Watchlist", path: "/watchlist", icon: <BookmarkIcon className="h-6 w-6" /> },
  { name: "Activity", path: "/activity", icon: <ActivityLogIcon className="h-6 w-6" /> },
  { name: "Wallet", path: "/wallet", icon: <WalletIcon className="h-6 w-6" /> },
  { name: "Payment Details", path: "/payment-details", icon: <LandmarkIcon className="h-6 w-6" /> },
  { name: "Withdrawal", path: "/withdrawals", icon: <CreditCardIcon className="h-6 w-6" /> },
  { name: "Profile", path: "/profile", icon: <PersonIcon className="h-6 w-6" /> },
];



const Sidebar = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const auth = useSelector(state => state.auth);
  const user = auth.user;
  const isUserAdmin = isAdmin(user);

  const handleLogout = () => {
    dispatch(logout(navigate));
  };

  // Build menu
  const menu = [
    ...baseMenu,
    { name: "Logout", path: "/", icon: <ExitIcon className="h-6 w-6" /> }
  ];

  return (
    <div className="mt-10 space-y-5">
      {menu.map((item, index) => (
        <div key={item.name}>
          <SheetClose className="w-full">
            <Button
              variant="outline"
              className="flex items-center gap-5 py-6 w-full"
              onClick={() => {
                if(item.name === "Logout"){
                  handleLogout();
                } else {
                  navigate(item.path);
                }
              }}
            >
              <span className="w-8">{item.icon}</span>
              <p>{item.name}</p>
            </Button>
          </SheetClose>
        </div>
      ))}
    </div>
  );
};

export default Sidebar;
