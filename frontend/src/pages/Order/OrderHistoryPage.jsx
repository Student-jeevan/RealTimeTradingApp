import React, { useEffect, useState } from "react";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "../../components/ui/table";
import { Badge } from "../../components/ui/badge";
import { format } from "date-fns";
import api from "../../config/api";
import { Button } from "../../components/ui/button";
import { toast } from "sonner";

const OrderHistoryPage = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchOrders();
    }, []);

    const fetchOrders = async () => {
        try {
            setLoading(true);
            const jwt = localStorage.getItem("jwt");
            const res = await api.get("/api/orders", {
                headers: jwt ? { Authorization: `Bearer ${jwt}` } : undefined,
            });
            // Sort newest first
            const sorted = res.data.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            setOrders(sorted);
        } catch (error) {
            console.error("Error fetching orders:", error);
            toast.error("Failed to load order history.");
        } finally {
            setLoading(false);
        }
    };

    const cancelOrder = async (orderId) => {
        try {
            const jwt = localStorage.getItem("jwt");
            await api.post(
                `/api/orders/${orderId}/cancel`,
                {},
                { headers: jwt ? { Authorization: `Bearer ${jwt}` } : undefined }
            );
            toast.success("Order cancelled successfully");
            fetchOrders(); // Refresh table
        } catch (err) {
            toast.error(err.response?.data?.message || "Failed to cancel order");
        }
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case "FILLED":
                return <Badge className="bg-green-500 hover:bg-green-600">FILLED</Badge>;
            case "OPEN":
            case "CREATED":
            case "VALIDATED":
                return <Badge className="bg-blue-500 hover:bg-blue-600">{status}</Badge>;
            case "CANCELLED":
                return <Badge className="bg-gray-500 hover:bg-gray-600">CANCELLED</Badge>;
            case "REJECTED":
                return <Badge className="bg-red-500 hover:bg-red-600">REJECTED</Badge>;
            case "PARTIALLY_FILLED":
                return <Badge className="bg-yellow-500 hover:bg-yellow-600">PARTIAL</Badge>;
            default:
                return <Badge className="bg-gray-500">{status}</Badge>;
        }
    };

    if (loading) return <div className="p-8 text-center text-gray-400 animate-pulse">Loading orders...</div>;

    return (
        <div className="p-4 lg:p-8 flex flex-col gap-6">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold tracking-tight text-white">Order History</h1>
            </div>

            <div className="bg-[#1a1a1a] rounded-xl border border-gray-800 overflow-hidden">
                <Table>
                    <TableHeader className="bg-[#0f0f0f]">
                        <TableRow className="border-gray-800 hover:bg-transparent">
                            <TableHead className="text-gray-400">Date</TableHead>
                            <TableHead className="text-gray-400">Pair</TableHead>
                            <TableHead className="text-gray-400">Type</TableHead>
                            <TableHead className="text-gray-400">Price</TableHead>
                            <TableHead className="text-gray-400">Amount</TableHead>
                            <TableHead className="text-gray-400">Status</TableHead>
                            <TableHead className="text-gray-400 text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {orders.map((order) => (
                            <TableRow key={order.id} className="border-gray-800 hover:bg-[#252525] transition-colors">
                                <TableCell className="text-gray-300">
                                    {format(new Date(order.timestamp), "dd MMM yyyy, HH:mm")}
                                </TableCell>
                                <TableCell className="font-semibold text-white">
                                    {(order.coin?.symbol || order.orderItem?.coin?.symbol || "—").toUpperCase()} / USD
                                </TableCell>
                                <TableCell>
                                    <span className={`font-medium ${order.orderType === 'BUY' ? 'text-green-400' : 'text-red-400'}`}>
                                        {order.orderType}
                                    </span>
                                </TableCell>
                                <TableCell className="text-gray-300">
                                    ${order.price.toFixed(2)}
                                </TableCell>
                                <TableCell className="text-gray-300">
                                    {order.orderItem?.quantity ?? order.quantity ?? 0}
                                </TableCell>
                                <TableCell>
                                    {getStatusBadge(order.status)}
                                </TableCell>
                                <TableCell className="text-right">
                                    {order.status === "OPEN" || order.status === "CREATED" ? (
                                        <Button
                                            variant="destructive"
                                            size="sm"
                                            onClick={() => cancelOrder(order.id)}
                                            className="bg-red-500/20 text-red-500 hover:bg-red-500/30 font-medium"
                                        >
                                            Cancel
                                        </Button>
                                    ) : (
                                        <span className="text-gray-600 text-sm">-</span>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                        {orders.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={7} className="text-center text-gray-500 py-12">
                                    No orders found.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
        </div>
    );
};

export default OrderHistoryPage;
