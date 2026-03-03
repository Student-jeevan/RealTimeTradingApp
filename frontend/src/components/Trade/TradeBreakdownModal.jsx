import React, { useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Loader2 } from "lucide-react";
import api from "../../config/api";
import { toast } from "sonner";

const TradeBreakdownModal = ({ isOpen, onClose, coin, quantity, orderType, onConfirm }) => {
    const [breakdown, setBreakdown] = useState(null);
    const [loading, setLoading] = useState(false);
    const [executing, setExecuting] = useState(false);
    const [error, setError] = useState("");

    React.useEffect(() => {
        if (isOpen && coin && quantity) {
            fetchBreakdown();
        }
    }, [isOpen, coin, quantity]);

    const fetchBreakdown = async () => {
        setLoading(true);
        setError("");
        try {
            const res = await api.post("/api/trade/breakdown", {
                coinId: coin.id,
                quantity: quantity,
                orderType: orderType,
            });
            setBreakdown(res.data);
        } catch (err) {
            setError(err.response?.data?.message || "Failed to fetch trade breakdown.");
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = async () => {
        setExecuting(true);
        try {
            await onConfirm();
            toast.success("Order executed successfully!");
            onClose();
        } catch (err) {
            toast.error(err.response?.data?.message || "Order execution failed.");
        } finally {
            setExecuting(false);
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[425px] bg-[#1a1a1a] text-white border-gray-800">
                <DialogHeader>
                    <DialogTitle className="text-xl font-bold">Confirm {orderType} Order</DialogTitle>
                </DialogHeader>

                {loading ? (
                    <div className="flex justify-center items-center py-8">
                        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
                    </div>
                ) : error ? (
                    <div className="text-red-500 text-center py-4 bg-red-500/10 rounded-md">
                        {error}
                    </div>
                ) : breakdown ? (
                    <div className="grid gap-4 py-4">
                        <div className="flex justify-between items-center pb-2 border-b border-gray-700">
                            <span className="text-gray-400">Asset</span>
                            <span className="font-semibold">{coin.name} ({coin.symbol.toUpperCase()})</span>
                        </div>
                        <div className="flex justify-between items-center pb-2 border-b border-gray-700">
                            <span className="text-gray-400">Current Price</span>
                            <span>${breakdown.currentPrice.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between items-center pb-2 border-b border-gray-700">
                            <span className="text-gray-400">Quantity</span>
                            <span>{quantity}</span>
                        </div>
                        <div className="flex justify-between items-center pb-2 border-b border-gray-700">
                            <span className="text-gray-400">Trade Value</span>
                            <span>${breakdown.tradeValue.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between items-center pb-2 border-b border-gray-700">
                            <span className="text-gray-400">Trading Fee</span>
                            <span className="text-red-400">-${breakdown.fee.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between items-center pt-2">
                            <span className="text-gray-300 font-bold">Total {orderType === "BUY" ? "Cost" : "Value"}</span>
                            <span className="text-xl font-bold text-blue-400">${breakdown.totalCost.toFixed(2)}</span>
                        </div>

                        {orderType === "BUY" && (
                            <div className="mt-4 p-3 bg-[#252525] rounded-md text-sm flex justify-between">
                                <span className="text-gray-400">Available After Trade</span>
                                <span className={breakdown.availableBalanceAfterTrade >= 0 ? "text-green-400" : "text-red-500 font-bold"}>
                                    ${breakdown.availableBalanceAfterTrade.toFixed(2)}
                                </span>
                            </div>
                        )}

                        <Button
                            className="w-full mt-4 bg-blue-600 hover:bg-blue-700 text-white"
                            onClick={handleConfirm}
                            disabled={executing || (orderType === 'BUY' && breakdown.availableBalanceAfterTrade < 0)}
                        >
                            {executing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                            {executing ? "Processing..." : `Confirm ${orderType}`}
                        </Button>
                    </div>
                ) : null}
            </DialogContent>
        </Dialog>
    );
};

export default TradeBreakdownModal;
