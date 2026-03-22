import React, { useEffect, useState, useRef, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import AssetTable from './AssetTable';
import StockChart from './StockChart';
import { Avatar, AvatarImage } from '@/components/ui/avatar';
import { Cross1Icon, DotIcon } from '@radix-ui/react-icons';
import { MessageCircleIcon, Send, TrendingUp, TrendingDown, Wallet, ShieldCheck } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { getCoinList, getTop50CoinList } from '@/State/Coin/Action';
import { useDispatch, useSelector } from 'react-redux';
import api from '@/config/api';
import StatCard from '@/components/ui/StatCard';
import { SkeletonPage } from '@/components/ui/SkeletonLoader';
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

const Home = () => {
  const [category, setCategory] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [inputValue, setInputValue] = useState("");
  const [isBotRelease, setIsBotRelease] = useState(false);
  const [messages, setMessages] = useState([
    {
      type: 'bot',
      content: 'Hi! I\'m your trading assistant. You can ask me about your wallet balance, assets, orders, or trading information.'
    }
  ]);
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const coin = useSelector((state) => state.coin);
  const dispatch = useDispatch();

  const handleBotRelease = () => setIsBotRelease(!isBotRelease);
  const handleCategory = (value) => {
    setCategory(value);
    setCurrentPage(1);
  };
  const handleChange = (e) => setInputValue(e.target.value);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMessage = inputValue.trim();
    setInputValue("");
    
    setMessages(prev => [...prev, { type: 'user', content: userMessage }]);
    setIsLoading(true);

    try {
      const jwt = localStorage.getItem("jwt");
      if (!jwt) {
        setMessages(prev => [...prev, { 
          type: 'bot', 
          content: 'Error: Please log in to use the chat feature.' 
        }]);
        setIsLoading(false);
        return;
      }

      const response = await api.post('/api/chat', 
        { message: userMessage },
        {
          headers: {
            'Authorization': `Bearer ${jwt}`
          }
        }
      );

      setMessages(prev => [...prev, { 
        type: 'bot', 
        content: response.data.response || 'Sorry, I couldn\'t process that request.' 
      }]);
    } catch (error) {
      console.error('Chat error:', error);
      setMessages(prev => [...prev, { 
        type: 'bot', 
        content: error.response?.data?.message || 'Sorry, an error occurred. Please try again.' 
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  };

  useEffect(() => {
    if (category === "top50" || category === "topGainers" || category === "topLosers") {
      dispatch(getTop50CoinList());
    }
  }, [category, dispatch]);

  useEffect(() => {
    if (category === "all") {
      dispatch(getCoinList(currentPage));
    }
  }, [category, currentPage, dispatch]);

  const getDisplayedCoins = () => {
    let coins = [];

    if (category === "all") {
      coins = coin?.coinList || [];
    } else if (category === "top50") {
      coins = coin?.top50 || [];
    } else if (category === "topGainers" || category === "topLosers") {
      const top50Data = coin?.top50 || [];
      const sorted = [...top50Data].sort((a, b) => {
        const aChange = a.price_change_percentage_24h || 0;
        const bChange = b.price_change_percentage_24h || 0;
        
        if (category === "topGainers") {
          return bChange - aChange;
        } else {
          return aChange - bChange;
        }
      });
      coins = sorted;
    }

    return coins;
  };

  const displayedCoins = getDisplayedCoins();
  
  const firstCoin = displayedCoins?.[0] || {};

  // === Stat card computations (using existing coin data, no new API calls) ===
  const stats = useMemo(() => {
    const coins = coin?.top50 || coin?.coinList || [];
    const totalMarketCap = coins.reduce((sum, c) => sum + (c.market_cap || 0), 0);
    const avgChange = coins.length > 0 
      ? coins.reduce((sum, c) => sum + (c.price_change_percentage_24h || 0), 0) / coins.length 
      : 0;
    const bestPerformer = coins.length > 0 
      ? [...coins].sort((a, b) => (b.price_change_percentage_24h || 0) - (a.price_change_percentage_24h || 0))[0]
      : null;
    const totalVolume = coins.reduce((sum, c) => sum + (c.total_volume || 0), 0);

    return { totalMarketCap, avgChange, bestPerformer, totalVolume };
  }, [coin?.top50, coin?.coinList]);

  const itemsPerPage = 10;
  let totalPages = 1;
  let paginatedCoins = displayedCoins;
  let showPagination = false;

  if (category === "all") {
    const hasFullPage = displayedCoins.length === itemsPerPage;
    totalPages = hasFullPage ? currentPage + 1 : currentPage;
    paginatedCoins = displayedCoins;
    showPagination = true;
  } else if (category === "top50") {
    totalPages = Math.ceil(displayedCoins.length / itemsPerPage);
    paginatedCoins = displayedCoins.slice(
      (currentPage - 1) * itemsPerPage,
      currentPage * itemsPerPage
    );
    showPagination = totalPages > 1;
  } else {
    totalPages = 1;
    paginatedCoins = displayedCoins;
    showPagination = false;
  }

  const handlePageChange = (newPage) => {
    if (newPage >= 1) {
      if (category === "all") {
        setCurrentPage(newPage);
      } else if (newPage <= totalPages) {
        setCurrentPage(newPage);
      }
    }
  };

  // Show skeleton if no data yet
  if (!coin?.coinList && !coin?.top50) {
    return <SkeletonPage />;
  }

  return (
    <div className='relative space-y-6'>
      {/* ===== Dashboard Header ===== */}
      <div>
        <h1 className='text-2xl font-bold'>Dashboard</h1>
        <p className='text-sm text-muted-foreground mt-1'>Real-time cryptocurrency market overview</p>
      </div>

      {/* ===== Market Overview Cards ===== */}
      <div>
        <h2 className='text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3'>Market Overview</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Market Cap"
          value={`$${(stats.totalMarketCap / 1e12).toFixed(2)}T`}
          icon={<Wallet />}
          trend={stats.avgChange}
          trendLabel="avg 24h"
          gradient="primary"
        />
        <StatCard
          title="Market Trend"
          value={stats.avgChange >= 0 ? 'Bullish' : 'Bearish'}
          icon={stats.avgChange >= 0 ? <TrendingUp /> : <TrendingDown />}
          trend={stats.avgChange}
          trendLabel="24h avg"
          gradient={stats.avgChange >= 0 ? 'success' : 'danger'}
        />
        <StatCard
          title="Top Performer"
          value={stats.bestPerformer?.symbol?.toUpperCase() || '—'}
          icon={<TrendingUp />}
          trend={stats.bestPerformer?.price_change_percentage_24h}
          trendLabel="24h"
          gradient="warning"
        />
        <StatCard
          title="24h Volume"
          value={`$${(stats.totalVolume / 1e9).toFixed(1)}B`}
          icon={<ShieldCheck />}
          gradient="primary"
        />
      </div>

      </div>

      {/* ===== Live Markets Section ===== */}
      <div>
        <h2 className='text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3'>Live Markets</h2>
      </div>
      <div className='grid grid-cols-1 xl:grid-cols-3 gap-6'>
        {/* Left: Coin Table */}
        <div className='xl:col-span-2 glass-card rounded-2xl overflow-hidden'>
          {/* Category tabs */}
          <div className='p-4 border-b border-border/30 flex items-center gap-2 flex-wrap'>
            {['all', 'top50', 'topGainers', 'topLosers'].map((cat) => (
              <button
                key={cat}
                onClick={() => handleCategory(cat)}
                className={`px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200
                  ${category === cat
                    ? 'gradient-primary text-white shadow-lg shadow-blue-500/20'
                    : 'bg-secondary/50 text-muted-foreground hover:bg-secondary hover:text-foreground'
                  }`}
              >
                {cat === 'all' ? 'All' : cat === 'top50' ? 'Top 50' : cat === 'topGainers' ? 'Top Gainers' : 'Top Losers'}
              </button>
            ))}
          </div>

          <AssetTable coin={paginatedCoins} category={category} />

          {showPagination && (
            <div className='p-3 border-t border-border/30'>
              <Pagination>
                <PaginationContent>
                  <PaginationItem>
                    <PaginationPrevious 
                      href="#" 
                      onClick={(e) => {
                        e.preventDefault();
                        handlePageChange(currentPage - 1);
                      }}
                      className={currentPage === 1 ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
                    />
                  </PaginationItem>
                  
                  {(() => {
                    const maxVisiblePages = 5;
                    const pages = [];
                    
                    if (category === "all") {
                      const startPage = Math.max(1, currentPage - 2);
                      const endPage = Math.min(startPage + maxVisiblePages - 1, totalPages);
                      for (let i = startPage; i <= endPage; i++) pages.push(i);
                    } else {
                      if (totalPages <= maxVisiblePages) {
                        for (let i = 1; i <= totalPages; i++) pages.push(i);
                      } else {
                        if (currentPage <= 3) {
                          for (let i = 1; i <= maxVisiblePages; i++) pages.push(i);
                        } else if (currentPage >= totalPages - 2) {
                          for (let i = totalPages - maxVisiblePages + 1; i <= totalPages; i++) pages.push(i);
                        } else {
                          for (let i = currentPage - 2; i <= currentPage + 2; i++) pages.push(i);
                        }
                      }
                    }
                    
                    return pages.map((pageNum) => (
                      <PaginationItem key={pageNum}>
                        <PaginationLink
                          href="#"
                          onClick={(e) => {
                            e.preventDefault();
                            handlePageChange(pageNum);
                          }}
                          isActive={currentPage === pageNum}
                          className="cursor-pointer"
                        >
                          {pageNum}
                        </PaginationLink>
                      </PaginationItem>
                    ));
                  })()}
                  
                  {category === "top50" && totalPages > 5 && currentPage < totalPages - 2 && (
                    <PaginationItem>
                      <PaginationEllipsis />
                    </PaginationItem>
                  )}
                  
                  <PaginationItem>
                    <PaginationNext 
                      href="#" 
                      onClick={(e) => {
                        e.preventDefault();
                        handlePageChange(currentPage + 1);
                      }}
                      className={currentPage === totalPages ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
                    />
                  </PaginationItem>
                </PaginationContent>
              </Pagination>
            </div>
          )}
        </div>

        {/* Right: Chart & Coin Info */}
        <div className="glass-card rounded-2xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-muted-foreground">Price Chart</h3>
            {firstCoin?.symbol && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold gradient-primary text-white">
                {firstCoin.symbol.toUpperCase()}
              </span>
            )}
          </div>
          
          <StockChart coinId={firstCoin?.id || "bitcoin"} key={firstCoin?.id || "bitcoin"} />

          {firstCoin?.name && (
            <div className='flex gap-4 items-center pt-2 border-t border-border/30'>
              <Avatar className="h-10 w-10 ring-2 ring-primary/20">
                <AvatarImage src={firstCoin?.image} />
              </Avatar>
              <div>
                <div className='flex items-center gap-2'>
                  <p className='text-sm font-semibold'>{firstCoin?.symbol?.toUpperCase()}</p>
                  <DotIcon className='text-muted-foreground w-3 h-3' />
                  <p className='text-xs text-muted-foreground'>{firstCoin?.name}</p>
                </div>
                <div className='flex items-end gap-2'>
                  <p className='text-xl font-bold'>
                    ${ (firstCoin?.current_price || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
                  </p>
                  <p className={`text-xs font-semibold ${(firstCoin?.price_change_24h || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                    {(firstCoin?.price_change_24h || 0) >= 0 ? '+' : ''}
                    ${ (firstCoin?.price_change_24h || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
                    {' ('}{ (firstCoin?.price_change_percentage_24h || 0).toFixed(2) }%)
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ===== Chat Bot Panel ===== */}
      {isBotRelease && (
        <div className='fixed bottom-5 right-5 z-50 flex flex-col mb-14'>
          <div className='rounded-2xl w-[20rem] md:w-[25rem] h-[70vh] glass-card flex flex-col shadow-2xl shadow-black/40 overflow-hidden'>
            {/* Header */}
            <div className='flex justify-between items-center border-b border-border/30 px-5 py-3'>
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg gradient-primary flex items-center justify-center">
                  <MessageCircleIcon className="w-4 h-4 text-white" />
                </div>
                <p className='font-semibold text-sm'>AI Assistant</p>
              </div>
              <Button onClick={handleBotRelease} variant='ghost' size='icon' className='h-8 w-8 rounded-lg'>
                <Cross1Icon className='h-4 w-4' />
              </Button>
            </div>

            {/* Messages */}
            <div className='flex-1 flex flex-col overflow-y-auto gap-3 px-4 py-4'>
              {messages.map((msg, i) => (
                <div
                  key={i}
                  className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`px-3.5 py-2 rounded-2xl max-w-[85%] text-sm ${
                      msg.type === 'user'
                        ? 'gradient-primary text-white rounded-br-md'
                        : 'bg-secondary/80 text-foreground rounded-bl-md'
                    }`}
                  >
                    <p className='whitespace-pre-wrap break-words'>{msg.content}</p>
                  </div>
                </div>
              ))}
              {isLoading && (
                <div className='flex justify-start'>
                  <div className='px-4 py-2 rounded-2xl rounded-bl-md bg-secondary/80'>
                    <div className="flex gap-1">
                      <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{animationDelay: '0ms'}} />
                      <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{animationDelay: '150ms'}} />
                      <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{animationDelay: '300ms'}} />
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className='border-t border-border/30 p-3 flex gap-2'>
              <Input
                className='flex-1 rounded-xl bg-secondary/50 border-0 focus-visible:ring-1'
                placeholder='Ask about trading...'
                value={inputValue}
                onChange={handleChange}
                onKeyPress={handleKeyPress}
                disabled={isLoading}
              />
              <Button
                onClick={handleSendMessage}
                disabled={isLoading || !inputValue.trim()}
                size='icon'
                className='h-10 w-10 rounded-xl gradient-primary border-0'
              >
                <Send size={16} />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Chatbot Toggle */}
      {!isBotRelease && (
        <div className='fixed bottom-5 right-5 z-50'>
          <button
            onClick={handleBotRelease}
            className='flex items-center gap-2 px-5 py-3 rounded-2xl gradient-primary text-white font-medium shadow-lg shadow-blue-500/30 hover:shadow-blue-500/40 hover:scale-105 transition-all duration-300'
          >
            <MessageCircleIcon size={20} />
            <span className='text-sm'>AI Chat</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default Home;
