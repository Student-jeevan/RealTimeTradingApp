import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '@/config/api';

// Convert http(s):// → ws(s):// for the native WebSocket URL
const WS_URL = API_BASE_URL.replace(/^http/, 'ws') + '/ws';

let stompClient = null;
let subscriptions = {};
let isConnecting = false;
let connectionQueue = [];

/**
 * Connect to the STOMP WebSocket broker using native WebSocket (no SockJS).
 *
 * @param {string}   token       - JWT (sent as Authorization header on CONNECT frame)
 * @param {function} onConnected - called once the STOMP session is ready
 * @param {function} [onError]   - optional error callback
 */
export function connect(token, onConnected, onError) {
    if (stompClient && stompClient.connected) {
        onConnected && onConnected(stompClient);
        return;
    }

    // Add to pending queue so all callers get notified when connection is established
    connectionQueue.push({ onConnected, onError });

    if (isConnecting) return;
    isConnecting = true;

    stompClient = new Client({
        brokerURL: WS_URL,             // native WebSocket — no SockJS needed
        connectHeaders: {
            Authorization: token ? `Bearer ${token}` : '',
        },
        reconnectDelay: 5000,
        onConnect: () => {
            console.log('[WS] Connected to', WS_URL);
            isConnecting = false;
            const queue = [...connectionQueue];
            connectionQueue = [];
            queue.forEach(q => q.onConnected && q.onConnected(stompClient));
        },
        onStompError: (frame) => {
            console.error('[WS] STOMP error', frame);
            isConnecting = false;
            const queue = [...connectionQueue];
            connectionQueue = [];
            queue.forEach(q => q.onError && q.onError(frame));
        },
        onWebSocketError: (event) => {
            console.error('[WS] WebSocket error', event);
            isConnecting = false;
            const queue = [...connectionQueue];
            connectionQueue = [];
            queue.forEach(q => q.onError && q.onError(event));
        },
        onDisconnect: () => {
            console.log('[WS] Disconnected');
            isConnecting = false;
        },
    });

    stompClient.activate();
}

/**
 * Subscribe to a STOMP destination.
 * Returns a subscription ID you can pass to unsubscribe().
 */
export function subscribe(destination, callback) {
    if (!stompClient || !stompClient.connected) {
        console.warn('[WS] Cannot subscribe — not connected yet');
        return null;
    }

    const sub = stompClient.subscribe(destination, (message) => {
        try {
            callback(JSON.parse(message.body));
        } catch {
            callback(message.body);
        }
    });

    subscriptions[sub.id] = sub;
    return sub.id;
}

/**
 * Unsubscribe a previously subscribed destination by its subscription ID.
 */
export function unsubscribe(subscriptionId) {
    if (subscriptionId && subscriptions[subscriptionId]) {
        subscriptions[subscriptionId].unsubscribe();
        delete subscriptions[subscriptionId];
    }
}

/**
 * Disconnect the STOMP client and clean up all subscriptions.
 */
export function disconnect() {
    if (stompClient) {
        Object.values(subscriptions).forEach((s) => {
            try { s.unsubscribe(); } catch (_) { }
        });
        subscriptions = {};
        stompClient.deactivate();
        stompClient = null;
    }
}

export function getClient() {
    return stompClient;
}
