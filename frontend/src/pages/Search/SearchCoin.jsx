import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { searchCoin } from '@/State/Coin/Action';
import { Input } from '@/components/ui/input';
import { Search } from 'lucide-react';
import AssetTable from '../Home/AssetTable';

function SearchCoin() {
    const [keyword, setKeyword] = useState("");
    const dispatch = useDispatch();
    const { coin } = useSelector(store => store);
    
    useEffect(() => {
        if (!keyword.trim()) return;
        const timeoutId = setTimeout(() => {
            dispatch(searchCoin(keyword));
        }, 300);
        return () => clearTimeout(timeoutId);
    }, [keyword, dispatch]);

    return (
        <div className="p-6 space-y-6">
            <h1 className="text-2xl font-bold">Search Coins</h1>
            <div className="relative">
                <Search className="absolute left-3 top-3.5 h-6 w-6 text-muted-foreground" />
                <Input 
                    placeholder="Search by name or symbol (e.g. BTC, Bitcoin)..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="pl-12 py-7 text-lg rounded-xl glass-card focus-visible:ring-1 focus-visible:ring-primary/50"
                />
            </div>
            
            {coin.loading ? (
                <div className="flex justify-center items-center mt-20">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                    <span className="ml-3 text-muted-foreground">Searching database...</span>
                </div>
            ) : coin.searchCoinList?.length > 0 ? (
                <div className="glass-card rounded-2xl overflow-hidden mt-6">
                    <AssetTable coin={coin.searchCoinList} category="all" />
                </div>
            ) : keyword && (
                <div className="glass-card rounded-2xl p-10 text-center mt-6">
                    <Search className="w-10 h-10 text-muted-foreground/40 mx-auto mb-3" />
                    <p className="text-lg font-medium text-foreground">No coins found</p>
                    <p className="text-sm text-muted-foreground mt-1">We couldn't find any match for "{keyword}"</p>
                </div>
            )}
        </div>
    );
}

export default SearchCoin;
