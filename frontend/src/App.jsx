import './App.css'
import Navbar from './pages/Navbar/Navbar'
import Home from './pages/Home/Home'
import LandingPage from './pages/Home/LandingPage'
import { Route, Routes } from 'react-router-dom'
import Withdrawal from './pages/Withdrawal/Withdrawal'
import PaymentDetails from './pages/Payment Details/PaymentDetails'
import StockDetails from './pages/Stock Detials/StockDetails'
import Watchlist from './pages/Watchlist/Watchlist'
import Profile from './pages/Profile/Profile'
import SearchCoin from './pages/Search/SearchCoin'
import Notfound from './pages/Notfound/Notfound'
import Portfolio from './pages/Portfolio/Portfolio'
import Wallet from './pages/Wallet/Wallet'
import Activity from './pages/Activity/Activity'
import Auth from './pages/Auth/Auth'
import ProtectedRoute from './components/ProtectedRoute'
import { useDispatch, useSelector } from 'react-redux'
import { useEffect } from "react";
import { getUser } from './State/Auth/Action'
import { Toaster } from 'sonner';
function App() {
  const auth = useSelector(state => state.auth);
  const dispatch = useDispatch();
  console.log("auth -- ", auth);
  useEffect(() => {
    const jwt = auth.jwt || localStorage.getItem("jwt");
    if (jwt) {
      dispatch(getUser(jwt));
    }
  }, [auth.jwt])
  return (
    <>
      {auth.user ? <div>
        <Navbar />
        <Routes>
          <Route path='/' element={<Home />} />
          <Route path='/portfolio' element={<Portfolio />} />
          <Route path='/activity' element={<Activity />} />
          <Route path='/wallet' element={<Wallet />} />
          <Route path='/withdrawals' element={<Withdrawal />} />
          <Route path='/payment-details' element={<PaymentDetails />} />
          <Route path='/market/:id' element={<StockDetails />} />
          <Route path='/watchlist' element={<Watchlist />} />
          <Route path='/profile' element={<Profile />} />
          <Route path='/search' element={<SearchCoin />} />
          <Route path='*' element={<Notfound />} />
        </Routes>
      </div> :
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/signin" element={<Auth />} />
          <Route path="/signup" element={<Auth />} />
          <Route path="/forgot-password" element={<Auth />} />
          <Route path="*" element={<LandingPage />} />
        </Routes>
      }
      <Toaster />
    </>
  )
}
export default App;
