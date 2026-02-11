import React, { useState, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { DotIcon } from "@radix-ui/react-icons";
import { Button } from "@/components/ui/button";
import { useSelector, useDispatch } from "react-redux";
import { getUserWallet } from "@/State/Wallet/Action";
import { getAssetDetails } from "@/State/Asset/Action";
import { payOrder } from "@/State/Order/Action";
import { toast } from "sonner";

function TradingForm() {
  const [amount, setAmount] = useState(0);
  const [orderType, setOrderType] = useState("BUY");
  const [quantity, setQuantity] = useState(0);

  const { coin, wallet, asset } = useSelector((store) => store);
  const dispatch = useDispatch();
  const jwt = localStorage.getItem("jwt");

  const coinPrice =
    coin?.coinDetails?.market_data?.current_price?.usd ?? 0;

  /* -------------------- LOAD WALLET -------------------- */
  useEffect(() => {
    if (!jwt) return;
    if (!wallet?.userWallet) {
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
    const value = Number(e.target.value || 0);
    setAmount(value);

    if (!coinPrice) {
      setQuantity(0);
      return;
    }

    const volume = calculateQuantity(value, coinPrice);
    setQuantity(volume);
  };

  const calculateQuantity = (amount, price) => {
    const volume = amount / price;
    return volume.toFixed(6);
  };

  const handleBuyCrypto = async () => {
    if (!jwt || !coin?.coinDetails?.id) return;

    try {
      await dispatch(
        payOrder({
          jwt,
          amount,
          orderData: {
            coinId: coin.coinDetails.id,
            quantity: Number(quantity),
            orderType,
          },
        })
      );

      toast.success(`${orderType} Order Placed Successfully`);

      // Refresh Data
      dispatch(getUserWallet(jwt));
      dispatch(getAssetDetails({ coinId: coin.coinDetails.id, jwt }));

    } catch (error) {
      toast.error(error.response?.data?.message || "Order Failed");
    }
  };

  /* -------------------- UI -------------------- */
  return (
    <div className="space-y-10 p-5">
      {/* Amount Input */}
      <div>
        <div className="flex gap-4 items-center justify-between">
          <Input
            className="py-7"
            placeholder="Enter Amount"
            type="number"
            onChange={handleChange}
          />
          <div>
            <p className="border text-2xl flex justify-center items-center w-36 h-14 rounded-md">
              {quantity}
            </p>
          </div>
        </div>
      </div>

      {/* Coin Info */}
      <div className="flex gap-5 items-center">
        <Avatar>
          <AvatarImage
            src={coin?.coinDetails?.image?.large || ""}
            alt={coin?.coinDetails?.name}
          />
        </Avatar>

        <div>
          <div className="flex items-center gap-2">
            <p>{coin?.coinDetails?.symbol?.toUpperCase()}</p>
            <DotIcon className="text-gray-400" />
            <p className="text-gray-400">
              {coin?.coinDetails?.name}
            </p>
          </div>

          <div className="flex items-end gap-2">
            <p className="text-xl font-bold">
              ${coinPrice}
            </p>
            <p
              className={
                coin?.coinDetails?.market_data
                  ?.price_change_percentage_24h >= 0
                  ? "text-green-600"
                  : "text-red-600"
              }
            >
              (
              {coin?.coinDetails?.market_data
                ?.price_change_percentage_24h?.toFixed(2)}
              %)
            </p>
          </div>
        </div>
      </div>

      {/* Order Info */}
      <div className="flex items-center justify-between">
        <p>Order Type</p>
        <p>Market Order</p>
      </div>

      <div className="flex items-center justify-between">
        <p>
          {orderType === "BUY"
            ? "Available Cash"
            : "Available Quantity"}
        </p>
        <p>
          {orderType === "BUY"
            ? `$${wallet?.userWallet?.balance ?? 0}`
            : asset?.assetDetails?.quantity ?? 0}
        </p>
      </div>

      {/* Actions */}
      <div>
        <Button
          onClick={handleBuyCrypto}
          className={`w-full py-6 ${orderType === "SELL"
            ? "bg-red-600 text-white"
            : ""
            }`}
        >
          {orderType}
        </Button>

        <Button
          variant="link"
          className="w-full mt-5 text-xl"
          onClick={() =>
            setOrderType(orderType === "BUY" ? "SELL" : "BUY")
          }
        >
          {orderType === "BUY" ? "Or Sell" : "Or Buy"}
        </Button>
      </div>
    </div>
  );
}

export default TradingForm;
