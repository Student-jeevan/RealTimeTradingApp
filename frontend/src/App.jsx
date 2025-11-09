import './App.css'
import Navbar from './pages/Navbar/Navbar'
import Home from './pages/Home/Home'
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
function App() {
  return (
   <>
      <Auth/>
      {true && <div>
      <Navbar/>
      <Routes>
        <Route path='/' element={<Home/>}/>
        <Route path='/portfolio' element={<Portfolio/>}/>
        <Route path='/activity' element={<Activity/>}/>
        <Route path='/wallet' element={<Wallet/>}/>
        <Route path='/withdrawal' element={<Withdrawal/>}/>
        <Route path='/payment-details' element={<PaymentDetails/>}/>
        <Route path='/market/:id' element={<StockDetails/>}/>
        <Route path='/watchlist' element={<Watchlist/>}/>
        <Route path='/profile' element={<Profile/>}/>
        <Route path='/search' element={<SearchCoin/>}/>
        <Route path='*' element={<Notfound/>}/>
      </Routes>
      </div>}
   </>  
  )
}
export default App;
