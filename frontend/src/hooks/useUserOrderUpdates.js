import { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { connect, subscribe, unsubscribe, disconnect } from '@/lib/websocket';
import { WEBSOCKET_ORDER_UPDATE } from '@/State/Order/ActionTypes';
import { toast } from 'sonner';

/**
 * Custom hook that subscribes to /user/queue/orders over STOMP WebSocket.
 * Dispatches WEBSOCKET_ORDER_UPDATE to Redux and shows a toast on fills.
 *
 * Must be called once in a component that is mounted for the lifetime of the
 * user session (e.g. TradingForm, or a top-level authenticated wrapper).
 */
const useUserOrderUpdates = () => {
    const dispatch = useDispatch();
    const subIdRef = useRef(null);

    useEffect(() => {
        const jwt = localStorage.getItem('jwt');
        if (!jwt) return;

        connect(
            jwt,
            () => {
                // Subscribe to /user/queue/orders
                // Spring routes this to the authenticated user via convertAndSendToUser
                subIdRef.current = subscribe('/user/queue/orders', (order) => {
                    dispatch({ type: WEBSOCKET_ORDER_UPDATE, payload: order });

                    // Show a friendly toast based on order status
                    if (order.status === 'FILLED') {
                        toast.success(
                            `✅ Order #${order.id} FILLED — ${order.orderType} ${order.filledQuantity} ${order.coin?.symbol?.toUpperCase() ?? ''}`
                        );
                    } else if (order.status === 'PARTIALLY_FILLED') {
                        toast.info(
                            `⚡ Order #${order.id} Partially Filled — ${order.filledQuantity}/${order.quantity} ${order.coin?.symbol?.toUpperCase() ?? ''}`
                        );
                    }
                });
            },
            (err) => {
                console.error('[WS] useUserOrderUpdates error', err);
            }
        );

        return () => {
            if (subIdRef.current) {
                unsubscribe(subIdRef.current);
                subIdRef.current = null;
            }
        };
    }, [dispatch]);
};

export default useUserOrderUpdates;
