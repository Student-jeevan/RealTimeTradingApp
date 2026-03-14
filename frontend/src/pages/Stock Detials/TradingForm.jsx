import React, { useState, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { useSelector, useDispatch } from "react-redux";
import { getUserWallet } from "@/State/Wallet/Action";
import { getAssetDetails } from "@/State/Asset/Action";
import { payOrder } from "@/State/Order/Action";
import { toast } from "sonner";
import OrderBookPanel from "@/pages/Order/OrderBookPanel";
import useUserOrderUpdates from "@/hooks/useUserOrderUpdates";
import { ArrowDownIcon, ArrowUpIcon, WalletIcon } from "lucide-react";

function TradingForm() {
  const [amount, setAmount] = useState("");
  const [orderType, setOrderType] = useState("BUY");
  const [quantity, setQuantity] = useState(0);
  const [limitPrice, setLimitPrice] = useState("");

  const { coin, wallet, asset } = useSelector((store) => store);
  const dispatch = useDispatch();
  const jwt = localStorage.getItem("jwt");

  // Subscribe to real-time user-specific order updates via WebSocket
  useUserOrderUpdates();

  const coinPrice =
    coin?.coinDetails?.market_data?.current_price?.usd ?? 0;

  const availableCash =
    wallet?.userWallet?.availableBalance ??
    wallet?.userWallet?.balance ??
    0;

  const availableAsset = asset?.assetDetails?.quantity ?? 0;

  /* -------------------- LOAD WALLET -------------------- */
  useEffect(() => {
    if (!jwt) return;
    if (!wallet?.userWallet || Object.keys(wallet.userWallet).length === 0) {
      dispatch(getUserWallet(jwt));
    }
  }, [dispatch, jwt, wallet?.userWallet]);

  /* -------------------- LOAD ASSET -------------------- */
  useEffect(() => {
    if (!jwt || !coin?.coinDetails?.id) return;
    dispatch(getAssetDetails({ coinId: coin.coinDetails.id, jwt }));
  }, [dispatch, jwt, coin?.coinDetails?.id]);

  /* -------------------- REFRESH ASSET ON SELL -------------------- */
  useEffect(() => {
    if (orderType === "SELL" && jwt && coin?.coinDetails?.id) {
      dispatch(getAssetDetails({ coinId: coin.coinDetails.id, jwt }));
    }
  }, [orderType, dispatch, jwt, coin?.coinDetails?.id]);

  /* -------------------- HANDLERS -------------------- */
  const handleChange = (e) => {
    const value = e.target.value;
    setAmount(value);

    // If input is empty, reset quantity
    if (value === "") {
      setQuantity(0);
      return;
    }

    const numValue = Number(value);
    const priceToUse = limitPrice ? Number(limitPrice) : coinPrice;

    if (!priceToUse || isNaN(numValue)) {
      setQuantity(0);
      return;
    }

    const volume = calculateQuantity(numValue, priceToUse);
    setQuantity(volume);
  };

  // Also recalculate when limit price changes
  useEffect(() => {
    if (amount !== "") {
      const numValue = Number(amount);
      const priceToUse = limitPrice ? Number(limitPrice) : coinPrice;
      if (priceToUse && !isNaN(numValue)) {
        setQuantity(calculateQuantity(numValue, priceToUse));
      }
    }
  }, [limitPrice, coinPrice, amount]);

  const calculateQuantity = (amt, price) => {
    const volume = amt / price;
    return volume.toFixed(6);
  };

  const handleBuyCrypto = async () => {
    if (!jwt || !coin?.coinDetails?.id) return;

    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      toast.error("Please enter a valid amount");
      return;
    }

    try {
      await dispatch(
        payOrder({
          jwt,
          amount: numAmount,
          orderData: {
            coinId: coin.coinDetails.id,
            quantity: Number(quantity),
            orderType,
            price: Number(limitPrice || coinPrice), // send explicit limit price
          },
        })
      );

      toast.success(`${orderType} Order Placed Successfully`);
      setAmount("");
      setQuantity(0);

      // Refresh Data
      dispatch(getUserWallet(jwt));
      dispatch(getAssetDetails({ coinId: coin.coinDetails.id, jwt }));

    } catch (error) {
      toast.error(error.response?.data?.message || "Order Failed");
    }
  };

  // UI Helpers
  const isBuy = orderType === "BUY";
  const symbol = coin?.coinDetails?.symbol?.toUpperCase() || "";

  /* -------------------- UI -------------------- */
  return (
    <div className="flex flex-col h-full bg-slate-950 rounded-xl overflow-hidden border border-slate-800 shadow-xl">

      {/* Tab Header */}
      <div className="flex w-full bg-slate-900 border-b border-slate-800 p-1">
        <button
          onClick={() => setOrderType("BUY")}
          className={`flex-1 py-3 text-sm font-semibold rounded-lg transition-all ${isBuy
            ? "bg-slate-800 text-green-400 shadow-sm"
            : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50"
            }`}
        >
          Buy {symbol}
        </button>
        <button
          onClick={() => setOrderType("SELL")}
          className={`flex-1 py-3 text-sm font-semibold rounded-lg transition-all ${!isBuy
            ? "bg-slate-800 text-red-400 shadow-sm"
            : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50"
            }`}
        >
          Sell {symbol}
        </button>
      </div>

      <div className="p-5 space-y-6">

        {/* Coin Info Header */}
        <div className="flex items-center justify-between p-4 bg-slate-900/50 rounded-xl border border-slate-800/50">
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10 border-2 border-slate-800">
              <AvatarImage
                src={coin?.coinDetails?.image?.small || ""}
                alt={coin?.coinDetails?.name}
              />
            </Avatar>
            <div>
              <p className="font-bold text-slate-100">{coin?.coinDetails?.name}</p>
              <p className="text-xs text-slate-400">{symbol}</p>
            </div>
          </div>
          <div className="text-right">
            <p className="text-lg font-bold text-slate-100">${coinPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })}</p>
            <p
              className={`text-xs font-medium flex items-center justify-end gap-1 ${coin?.coinDetails?.market_data?.price_change_percentage_24h >= 0
                ? "text-green-500"
                : "text-red-500"
                }`}
            >
              {coin?.coinDetails?.market_data?.price_change_percentage_24h >= 0 ? (
                <ArrowUpIcon className="w-3 h-3" />
              ) : (
                <ArrowDownIcon className="w-3 h-3" />
              )}
              {Math.abs(coin?.coinDetails?.market_data?.price_change_percentage_24h || 0).toFixed(2)}%
            </p>
          </div>
        </div>

        {/* Form Inputs */}
        <div className="space-y-4">

          {/* Order Mode / Limit Price */}
          <div className="flex items-center justify-between bg-slate-900 border border-slate-800 rounded-xl p-3">
            <span className="text-sm font-medium text-slate-400 pl-2">Limit Price</span>
            <div className="flex items-center gap-2">
              <span className="text-sm text-slate-500">$</span>
              <Input
                className="w-28 h-8 text-right bg-transparent border-none focus-visible:ring-0 text-slate-100 font-medium p-0"
                placeholder={coinPrice ? coinPrice.toString() : "0"}
                type="number"
                value={limitPrice}
                onChange={(e) => setLimitPrice(e.target.value)}
              />
            </div>
          </div>

          {/* Amount Output / Input */}
          <div className="relative group rounded-xl border border-slate-700 bg-slate-900/80 p-4 transition-all focus-within:border-primary/50 focus-within:ring-1 focus-within:ring-primary/50">
            <div className="flex justify-between mb-2">
              <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                Total (USD)
              </label>
              <span className="text-xs text-slate-500 font-medium">
                ≈ {quantity || "0.00"} {symbol}
              </span>
            </div>
            <div className="flex items-center">
              <span className="text-2xl text-slate-400 mr-2">$</span>
              <input
                className="w-full bg-transparent text-3xl font-bold text-slate-100 outline-none placeholder:text-slate-700"
                placeholder="0.00"
                type="number"
                value={amount}
                onChange={handleChange}
              />
            </div>
          </div>

        </div>

        {/* Action Area */}
        <div className="pt-2">
          <div className="flex items-center justify-between mb-4 px-1 text-sm">
            <span className="text-slate-400 flex items-center gap-2">
              <WalletIcon className="w-4 h-4" />
              {isBuy ? "Available USD" : `Available ${symbol}`}
            </span>
            <span className="font-semibold text-slate-200">
              {isBuy ? `$${availableCash?.toFixed(2)}` : availableAsset}
            </span>
          </div>

          <Button
            onClick={handleBuyCrypto}
            className={`w-full py-6 text-lg font-bold rounded-xl shadow-lg transition-transform hover:scale-[1.02] active:scale-[0.98] ${isBuy
              ? "bg-green-600 hover:bg-green-500 text-white shadow-green-900/20"
              : "bg-red-600 hover:bg-red-500 text-white shadow-red-900/20"
              }`}
          >
            {isBuy ? `Buy ${symbol}` : `Sell ${symbol}`}
          </Button>
        </div>

      </div>

      {/* Order Book Panel */}
      {coin?.coinDetails?.id && (
        <div className="mt-auto bg-slate-900 border-t border-slate-800 p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-slate-300">
              Order Book
            </h3>
            <span className="text-[10px] uppercase tracking-wider text-slate-500 bg-slate-800 px-2 py-1 rounded">Live</span>
          </div>
          <div className="max-h-64 overflow-y-auto pr-1 custom-scrollbar">
            <OrderBookPanel coinId={coin.coinDetails.id} />
          </div>
        </div>
      )}
    </div>
  );
}

export default TradingForm;

