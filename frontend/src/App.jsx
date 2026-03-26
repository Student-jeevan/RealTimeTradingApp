import './App.css'
import React, { lazy, Suspense, useEffect } from 'react';
import { Route, Routes } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { getUser } from './State/Auth/Action'
import { Toaster } from 'sonner';

// Layout
import AppLayout from './components/layout/AppLayout'

// Lazy loaded pages
const Home = lazy(() => import('./pages/Home/Home'));
const LandingPage = lazy(() => import('./pages/Home/LandingPage'));
const Withdrawal = lazy(() => import('./pages/Withdrawal/Withdrawal'));
const PaymentDetails = lazy(() => import('./pages/Payment Details/PaymentDetails'));
const StockDetails = lazy(() => import('./pages/Stock Detials/StockDetails'));
const Watchlist = lazy(() => import('./pages/Watchlist/Watchlist'));
const Profile = lazy(() => import('./pages/Profile/Profile'));
const SearchCoin = lazy(() => import('./pages/Search/SearchCoin'));
const Notfound = lazy(() => import('./pages/Notfound/Notfound'));
const Portfolio = lazy(() => import('./pages/Portfolio/Portfolio'));
const Wallet = lazy(() => import('./pages/Wallet/Wallet'));
const Activity = lazy(() => import('./pages/Activity/Activity'));
const Auth = lazy(() => import('./pages/Auth/Auth'));
const PriceAlertsPage = lazy(() => import('./pages/Alerts/PriceAlertsPage'));
const Analytics = lazy(() => import('./pages/AnalyticsView/Analytics'));
const WalletLedgerPage = lazy(() => import('./pages/Wallet/WalletLedgerPage'));
const OrderHistoryPage = lazy(() => import('./pages/Order/OrderHistoryPage'));

// Loader component for Suspense
const PageLoader = () => (
  <div className="flex h-[50vh] w-full items-center justify-center">
    <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
  </div>
);

function App() {
  const auth = useSelector(state => state.auth);
  const dispatch = useDispatch();

  useEffect(() => {
    const jwt = auth.jwt || localStorage.getItem("jwt");
    if (jwt) {
      dispatch(getUser(jwt));
    }
  }, [auth.jwt, dispatch])

  return (
    <>
      {auth.user ? (
        <AppLayout>
          <Suspense fallback={<PageLoader />}>
            <Routes>
              <Route path='/' element={<Home />} />
              <Route path='/portfolio' element={<Portfolio />} />
              <Route path='/activity' element={<Activity />} />
              <Route path='/wallet' element={<Wallet />} />
              <Route path='/wallet/ledger' element={<WalletLedgerPage />} />
              <Route path='/orders' element={<OrderHistoryPage />} />
              <Route path='/withdrawals' element={<Withdrawal />} />
              <Route path='/payment-details' element={<PaymentDetails />} />
              <Route path='/market/:id' element={<StockDetails />} />
              <Route path='/watchlist' element={<Watchlist />} />
              <Route path='/profile' element={<Profile />} />
              <Route path='/search' element={<SearchCoin />} />
              <Route path='/alerts' element={<PriceAlertsPage />} />
              <Route path='/analytics' element={<Analytics />} />
              <Route path='*' element={<Notfound />} />
            </Routes>
          </Suspense>
        </AppLayout>
      ) : (
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/signin" element={<Auth />} />
            <Route path="/signup" element={<Auth />} />
            <Route path="/forgot-password" element={<Auth />} />
            <Route path="*" element={<LandingPage />} />
          </Routes>
        </Suspense>
      )}
      <Toaster richColors position="top-right" />
    </>
  )
}
export default App;
