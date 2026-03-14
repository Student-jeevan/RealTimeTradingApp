import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { toast } from 'sonner';
import {
    createAlert,
    getActiveAlerts,
    getTriggeredAlerts,
    deleteAlert,
} from '@/State/Alert/Action';
import { Trash2, Bell, History } from 'lucide-react';

function PriceAlertsPage() {
    const dispatch = useDispatch();
    const { alert } = useSelector((state) => state);
    const jwt = localStorage.getItem('jwt');

    const [coinId, setCoinId] = useState('');
    const [targetPrice, setTargetPrice] = useState('');
    const [condition, setCondition] = useState('ABOVE');

    useEffect(() => {
        if (jwt) {
            dispatch(getActiveAlerts(jwt));
            dispatch(getTriggeredAlerts(jwt));
        }
    }, [dispatch, jwt]);

    const handleCreateAlert = async (e) => {
        e.preventDefault();
        if (!coinId || !targetPrice) {
            toast.error('Please fill in all fields');
            return;
        }

        try {
            await dispatch(
                createAlert({
                    jwt,
                    coinId: coinId.toLowerCase(),
                    targetPrice: Number(targetPrice),
                    condition,
                })
            );
            toast.success('Price alert created');
            setCoinId('');
            setTargetPrice('');
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to create alert');
        }
    };

    const handleDeleteAlert = async (id) => {
        try {
            await dispatch(deleteAlert({ jwt, id }));
            toast.success('Alert deleted');
        } catch (error) {
            toast.error('Failed to delete alert');
        }
    };

    return (
        <div className="p-10 space-y-10">
            <div className="max-w-xl mx-auto bg-[#111827] p-8 rounded-xl border border-gray-800 shadow-2xl">
                <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
                    <Bell className="text-yellow-500" /> Create Price Alert
                </h2>
                <form onSubmit={handleCreateAlert} className="space-y-4">
                    <div className="space-y-2">
                        <label className="text-sm text-gray-400">Coin ID (e.g. bitcoin)</label>
                        <Input
                            placeholder="bitcoin"
                            value={coinId}
                            onChange={(e) => setCoinId(e.target.value)}
                            className="bg-gray-900 border-gray-700"
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-sm text-gray-400">Target Price (USD)</label>
                            <Input
                                type="number"
                                placeholder="60000"
                                value={targetPrice}
                                onChange={(e) => setTargetPrice(e.target.value)}
                                className="bg-gray-900 border-gray-700"
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-sm text-gray-400">Condition</label>
                            <select
                                value={condition}
                                onChange={(e) => setCondition(e.target.value)}
                                className="w-full h-10 px-3 py-2 bg-gray-900 border border-gray-700 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-gray-400"
                            >
                                <option value="ABOVE">Above</option>
                                <option value="BELOW">Below</option>
                            </select>
                        </div>
                    </div>
                    <Button type="submit" className="w-full py-6 mt-4 bg-yellow-600 hover:bg-yellow-700">
                        Set Alert
                    </Button>
                </form>
            </div>

            <div className="space-y-6">
                <h2 className="text-xl font-semibold flex items-center gap-2">
                    <Bell className="h-5 w-5" /> Active Alerts
                </h2>
                <div className="border border-gray-800 rounded-lg overflow-hidden">
                    <Table>
                        <TableHeader className="bg-gray-950">
                            <TableRow>
                                <TableHead>Coin</TableHead>
                                <TableHead>Condition</TableHead>
                                <TableHead>Target Price</TableHead>
                                <TableHead>Created At</TableHead>
                                <TableHead className="text-right">Action</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {alert.activeAlerts.map((item) => (
                                <TableRow key={item.id} className="hover:bg-gray-900">
                                    <TableCell className="font-medium uppercase">{item.coin}</TableCell>
                                    <TableCell>
                                        <span
                                            className={`px-2 py-1 rounded text-xs ${item.condition === 'ABOVE'
                                                ? 'bg-green-900 text-green-300'
                                                : 'bg-red-900 text-red-300'
                                                }`}
                                        >
                                            {item.condition}
                                        </span>
                                    </TableCell>
                                    <TableCell>${item.targetPrice.toLocaleString()}</TableCell>
                                    <TableCell>{new Date(item.createdAt).toLocaleString()}</TableCell>
                                    <TableCell className="text-right">
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => handleDeleteAlert(item.id)}
                                            className="text-gray-400 hover:text-red-500"
                                        >
                                            <Trash2 className="h-4 w-4" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                            {alert.activeAlerts.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={5} className="text-center py-10 text-gray-500">
                                        No active alerts
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
            </div>

            <div className="space-y-6">
                <h2 className="text-xl font-semibold flex items-center gap-2">
                    <History className="h-5 w-5" /> Triggered History
                </h2>
                <div className="border border-gray-800 rounded-lg overflow-hidden">
                    <Table>
                        <TableHeader className="bg-gray-950">
                            <TableRow>
                                <TableHead>Coin</TableHead>
                                <TableHead>Target</TableHead>
                                <TableHead>Triggered Price</TableHead>
                                <TableHead>Triggered At</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {alert.triggeredAlerts.map((item) => (
                                <TableRow key={item.id} className="hover:bg-gray-900 opacity-70">
                                    <TableCell className="font-medium uppercase">{item.coin}</TableCell>
                                    <TableCell>${item.targetPrice.toLocaleString()}</TableCell>
                                    <TableCell className="text-green-400">
                                        ${item.triggeredPrice?.toLocaleString()}
                                    </TableCell>
                                    <TableCell>{new Date(item.triggeredAt).toLocaleString()}</TableCell>
                                </TableRow>
                            ))}
                            {alert.triggeredAlerts.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={4} className="text-center py-10 text-gray-500">
                                        No history found
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </div>
            </div>
        </div>
    );
}

export default PriceAlertsPage;
