import React, { useEffect, useState, useRef } from 'react';
import { Button } from '@/components/ui/button';
import AssetTable from './AssetTable';
import StockChart from './StockChart';
import { Avatar, AvatarImage } from '@/components/ui/avatar';
import { Cross1Icon, DotIcon } from '@radix-ui/react-icons';
import { MessageCircleIcon, Send } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { getCoinList, getTop50CoinList } from '@/State/Coin/Action';
import { useDispatch, useSelector } from 'react-redux';
import api from '@/config/api';
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
    setCurrentPage(1); // Reset to first page when category changes
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
    
    // Add user message to chat
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

  // Fetch data based on category
  useEffect(() => {
    if (category === "top50" || category === "topGainers" || category === "topLosers") {
      dispatch(getTop50CoinList());
    }
  }, [category, dispatch]);

  // Fetch coin list for "all" category with pagination
  useEffect(() => {
    if (category === "all") {
      dispatch(getCoinList(currentPage));
    }
  }, [category, currentPage, dispatch]);

  // Calculate displayed coins based on category, sorting, and pagination
  const getDisplayedCoins = () => {
    let coins = [];

    if (category === "all") {
      // API already returns paginated results (10 coins per page)
      coins = coin?.coinList || [];
    } else if (category === "top50") {
      coins = coin?.top50 || [];
    } else if (category === "topGainers" || category === "topLosers") {
      // Use top50 data for sorting
      const top50Data = coin?.top50 || [];
      // Sort by price_change_percentage_24h
      const sorted = [...top50Data].sort((a, b) => {
        const aChange = a.price_change_percentage_24h || 0;
        const bChange = b.price_change_percentage_24h || 0;
        
        if (category === "topGainers") {
          // Descending order (highest gains first)
          return bChange - aChange;
        } else {
          // Ascending order (lowest losses first)
          return aChange - bChange;
        }
      });
      coins = sorted;
    }

    return coins;
  };

  const displayedCoins = getDisplayedCoins();
  
  // Get the first coin for the sidebar chart
  const firstCoin = displayedCoins?.[0] || {};

  // Pagination logic
  const itemsPerPage = 10;
  let totalPages = 1;
  let paginatedCoins = displayedCoins;
  let showPagination = false;

  if (category === "all") {
    // For "all", API handles pagination server-side (10 coins per page)
    // Allow navigation: if we have 10 coins, there might be more pages
    // If we have less than 10, we're on the last page
    const hasFullPage = displayedCoins.length === itemsPerPage;
    totalPages = hasFullPage ? currentPage + 1 : currentPage;
    paginatedCoins = displayedCoins; // Use API result directly
    showPagination = true;
  } else if (category === "top50") {
    // For "top50", paginate client-side (50 coins / 10 per page = 5 pages)
    totalPages = Math.ceil(displayedCoins.length / itemsPerPage);
    paginatedCoins = displayedCoins.slice(
      (currentPage - 1) * itemsPerPage,
      currentPage * itemsPerPage
    );
    showPagination = totalPages > 1;
  } else {
    // For "topGainers" and "topLosers", show all coins (no pagination)
    totalPages = 1;
    paginatedCoins = displayedCoins;
    showPagination = false;
  }

  // Handle pagination
  const handlePageChange = (newPage) => {
    if (newPage >= 1) {
      // For "all" category, allow forward navigation even if we don't know the exact total
      if (category === "all") {
        setCurrentPage(newPage);
      } else if (newPage <= totalPages) {
        setCurrentPage(newPage);
      }
    }
  };

  return (
    <div className='relative'>
      <div className='lg:flex'>
        {/* Left Side - Coin Table */}
        <div className='lg:w-[50%] lg:border-r'>
          <div className='p-3 flex items-center gap-4'>
            <Button onClick={() => handleCategory("all")} variant={category === "all" ? "default" : "outline"} className='rounded-full'>All</Button>
            <Button onClick={() => handleCategory("top50")} variant={category === "top50" ? "default" : "outline"} className='rounded-full'>Top 50</Button>
            <Button onClick={() => handleCategory("topGainers")} variant={category === "topGainers" ? "default" : "outline"} className='rounded-full'>Top Gainers</Button>
            <Button onClick={() => handleCategory("topLosers")} variant={category === "topLosers" ? "default" : "outline"} className='rounded-full'>Top Losers</Button>
          </div>

          <AssetTable coin={paginatedCoins} category={category} />

          {/* Show pagination only when needed */}
          {showPagination && (
            <div>
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
                  
                  {/* Page numbers */}
                  {(() => {
                    const maxVisiblePages = 5;
                    const pages = [];
                    
                    if (category === "all") {
                      // For "all", show current page and a few around it
                      const startPage = Math.max(1, currentPage - 2);
                      const endPage = Math.min(startPage + maxVisiblePages - 1, totalPages);
                      
                      for (let i = startPage; i <= endPage; i++) {
                        pages.push(i);
                      }
                    } else {
                      // For "top50", show pages with ellipsis if needed
                      if (totalPages <= maxVisiblePages) {
                        for (let i = 1; i <= totalPages; i++) {
                          pages.push(i);
                        }
                      } else {
                        if (currentPage <= 3) {
                          for (let i = 1; i <= maxVisiblePages; i++) {
                            pages.push(i);
                          }
                        } else if (currentPage >= totalPages - 2) {
                          for (let i = totalPages - maxVisiblePages + 1; i <= totalPages; i++) {
                            pages.push(i);
                          }
                        } else {
                          for (let i = currentPage - 2; i <= currentPage + 2; i++) {
                            pages.push(i);
                          }
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

        {/* Right Side - Chart & Selected Coin */}
        <div className="hidden lg:block lg:w-[50%] p-5">
          <StockChart coinId={firstCoin?.id || "bitcoin"} key={firstCoin?.id || "bitcoin"} />

          {firstCoin && (
            <div className='flex gap-5 items-center mt-5'>
              <div>
                <Avatar>
                  <AvatarImage src={firstCoin?.image} />
                </Avatar>
              </div>
              <div>
                <div className='flex items-center gap-2'>
                  <p>{firstCoin?.symbol?.toUpperCase()}</p>
                  <DotIcon className='text-gray-400' />
                  <p className='text-gray-400'>{firstCoin?.name}</p>
                </div>
                <div className='flex items-end gap-2'>
                  <p className='text-xl font-bold'>
                    ${ (firstCoin?.current_price || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
                  </p>
                  <p className={(firstCoin?.price_change_24h || 0) >= 0 ? 'text-green-600' : 'text-red-600'}>
                    <span>
                      {(firstCoin?.price_change_24h || 0) >= 0 ? '+' : ''}${ (firstCoin?.price_change_24h || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
                    </span>
                    <span>
                      {' ('}{(firstCoin?.price_change_percentage_24h || 0).toFixed(2)}%)
                    </span>
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Chat Bot Panel */}
      {isBotRelease && (
        <div className='absolute bottom-5 right-5 z-40 flex flex-col mb-20'>
          <div className='rounded-md w-[20rem] md:w-[25rem] lg:w-[25rem] h-[70vh] bg-slate-900 flex flex-col shadow-2xl'>
            {/* Header */}
            <div className='flex justify-between items-center border-b px-6 py-3'>
              <p className='font-semibold'>Chat Bot</p>
              <Button onClick={handleBotRelease} variant='ghost' size='icon' className='h-8 w-8'>
                <Cross1Icon className='h-4 w-4' />
              </Button>
            </div>

            {/* Messages */}
            {/* Messages */}
<div className='flex-1 flex flex-col overflow-y-auto gap-4 px-5 py-4 scroll-container'>
  {messages.map((msg, i) => (
    <div
      key={i}
      className={`flex ${msg.type === 'user' ? 'justify-start' : 'justify-end'}`}
    >
      <div
        className={`px-4 py-2 rounded-lg max-w-[85%] ${
          msg.type === 'user' ? 'bg-slate-800 text-gray-100' : 'bg-blue-600 text-white'
        }`}
      >
        <p className='text-sm whitespace-pre-wrap break-words'>{msg.content}</p>
      </div>
    </div>
  ))}
  {isLoading && (
    <div className='flex justify-end'>
      <div className='px-4 py-2 rounded-lg bg-blue-600 text-white'>
        <p className='text-sm text-gray-100'>Thinking...</p>
      </div>
    </div>
  )}
  <div ref={messagesEndRef} />
</div>

            {/* Input */}
            <div className='border-t p-3 flex gap-2'>
              <Input
                className='flex-1'
                placeholder='Ask about wallet, assets, orders, or trading...'
                value={inputValue}
                onChange={handleChange}
                onKeyPress={handleKeyPress}
                disabled={isLoading}
              />
              <Button
                onClick={handleSendMessage}
                disabled={isLoading || !inputValue.trim()}
                size='icon'
                className='h-10 w-10'
              >
                <Send size={18} />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Chatbot Toggle Button */}
      {!isBotRelease && (
        <div className='absolute bottom-5 right-5 z-50'>
          <Button 
            onClick={handleBotRelease} 
            className='w-[10rem] h-[3rem] gap-2 items-center shadow-lg hover:shadow-xl transition-shadow'
          >
            <MessageCircleIcon size={24} className='w-6 h-6' />
            <span className='text-lg font-medium'>Chat Bot</span>
          </Button>
        </div>
      )}
    </div>
  );
};

export default Home;
