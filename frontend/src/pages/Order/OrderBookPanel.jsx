import React, { useEffect, useRef, useState } from "react";
import { connect, subscribe, unsubscribe } from "@/lib/websocket";
import api from "@/config/api";

/**
 * OrderBookPanel — displays live buy/sell order book for a coin.
 *
 * Connects to the STOMP broker on mount, subscribes to:
 *   /topic/orderbook/{coinId}
 *
 * Falls back to a one-time REST fetch while the WebSocket is connecting.
 */
const OrderBookPanel = ({ coinId }) => {
  const [book, setBook] = useState({ buyOrders: [], sellOrders: [] });
  const subIdRef = useRef(null);
  const jwt = localStorage.getItem("jwt");

  useEffect(() => {
    if (!coinId) return;

    // ── 1. One-time REST fetch so the panel shows data immediately ──────────
    const fetchOnce = async () => {
      try {
        const res = await api.get(`/api/orders/orderbook/${coinId}`);
        setBook(res.data || { buyOrders: [], sellOrders: [] });
      } catch (e) {
        console.error("[OrderBook] initial fetch failed", e);
      }
    };
    fetchOnce();

    // ── 2. Subscribe to real-time updates via STOMP ──────────────────────────
    connect(
      jwt,
      () => {
        subIdRef.current = subscribe(
          `/topic/orderbook/${coinId}`,
          (data) => {
            setBook(data || { buyOrders: [], sellOrders: [] });
          }
        );
      },
      (err) => {
        console.error("[OrderBook] WS connect error", err);
      }
    );

    return () => {
      if (subIdRef.current) {
        unsubscribe(subIdRef.current);
        subIdRef.current = null;
      }
    };
  }, [coinId, jwt]);

  const renderSide = (orders, isBuy) => (
    <table className="w-full text-xs">
      <thead>
        <tr className="text-gray-500">
          <th className="text-left">Price</th>
          <th className="text-left">Qty</th>
          <th className="text-left">Total</th>
        </tr>
      </thead>
      <tbody>
        {orders.map((o) => {
          const qty = o.remainingQuantity ?? o.quantity ?? 0;
          const total = Number(o.price) * qty;
          return (
            <tr
              key={o.id}
              className={isBuy ? "text-green-400" : "text-red-400"}
            >
              <td>{Number(o.price).toFixed(4)}</td>
              <td>{Number(qty).toFixed(4)}</td>
              <td>{total.toFixed(4)}</td>
            </tr>
          );
        })}
        {orders.length === 0 && (
          <tr>
            <td colSpan={3} className="text-gray-500 py-2 text-center">
              No orders
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );

  return (
    <div className="grid grid-cols-2 gap-4 bg-[#111827] rounded-lg p-4 border border-gray-800">
      <div>
        <h3 className="text-green-400 mb-2 text-sm font-semibold flex items-center gap-1">
          Buy Orders
          <span className="ml-1 h-1.5 w-1.5 rounded-full bg-green-400 animate-pulse" />
        </h3>
        {renderSide(book.buyOrders || [], true)}
      </div>
      <div>
        <h3 className="text-red-400 mb-2 text-sm font-semibold flex items-center gap-1">
          Sell Orders
          <span className="ml-1 h-1.5 w-1.5 rounded-full bg-red-400 animate-pulse" />
        </h3>
        {renderSide(book.sellOrders || [], false)}
      </div>
    </div>
  );
};

export default OrderBookPanel;
