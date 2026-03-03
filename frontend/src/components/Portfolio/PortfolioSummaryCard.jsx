import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { ArrowDownIcon, ArrowUpIcon } from "@radix-ui/react-icons";
import api from "../../config/api";

const PortfolioSummaryCard = () => {
    const [summary, setSummary] = useState({
        totalInvested: 0,
        currentValue: 0,
        totalPnL: 0,
        change24h: 0,
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchPortfolioData();
    }, []);

    const fetchPortfolioData = async () => {
        try {
            setLoading(true);
            const res = await api.get("/api/asset");
            const assets = res.data;

            let invested = 0;
            let currentVal = 0;
            let change = 0;

            assets.forEach((asset) => {
                invested += asset.buyPrice * asset.quantity;
                currentVal += asset.coin.currentPrice * asset.quantity;
                change += (asset.coin.priceChangePercentage24h || 0) * (asset.coin.currentPrice * asset.quantity);
            });

            const pnl = currentVal - invested;
            const averageChange = currentVal > 0 ? change / currentVal : 0;

            setSummary({
                totalInvested: invested,
                currentValue: currentVal,
                totalPnL: pnl,
                change24h: averageChange,
            });
        } catch (error) {
            console.error("Failed to fetch portfolio data", error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="text-gray-500 text-sm animate-pulse">Loading portfolio...</div>;

    const isPositive = summary.totalPnL >= 0;

    return (
        <Card className="bg-gradient-to-br from-[#1E293B] to-[#0F172A] border-gray-800 shadow-xl overflow-hidden relative">
            <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/10 rounded-full blur-3xl -mr-10 -mt-10"></div>
            <CardHeader className="pb-2">
                <CardTitle className="text-gray-400 text-sm font-medium uppercase tracking-wider">
                    Portfolio Value
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                    <div>
                        <div className="text-4xl font-bold text-white mb-1">
                            ${summary.currentValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </div>
                        <div className={`flex items-center text-sm font-medium ${isPositive ? 'text-green-500' : 'text-red-500'}`}>
                            {isPositive ? <ArrowUpIcon className="mr-1" /> : <ArrowDownIcon className="mr-1" />}
                            ${Math.abs(summary.totalPnL).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (Net P&L)
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
                        <div>
                            <p className="text-gray-500 mb-0.5">Total Invested</p>
                            <p className="font-semibold text-gray-200">
                                ${summary.totalInvested.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </p>
                        </div>
                        <div>
                            <p className="text-gray-500 mb-0.5">24h Change</p>
                            <p className={`font-semibold ${summary.change24h >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                                {summary.change24h > 0 ? '+' : ''}{summary.change24h.toFixed(2)}%
                            </p>
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
};

export default PortfolioSummaryCard;
