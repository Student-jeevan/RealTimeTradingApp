import React, { useEffect, useState } from "react";
import axios from "axios";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "../../components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { format } from "date-fns";
import api from "../../config/api";

const WalletLedgerPage = () => {
    const [ledgerEntries, setLedgerEntries] = useState([]);
    const [wallet, setWallet] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchWalletData();
    }, []);

    const fetchWalletData = async () => {
        try {
            setLoading(true);
            // Assuming a new endpoint or piggybacking on existing /api/wallet
            const walletRes = await api.get("/api/wallet");
            setWallet(walletRes.data);

            // We need a ledger endpoint. Assuming /api/wallet/ledger exists. 
            // I will create this backend endpoint next to fulfill the UI requirement.
            const ledgerRes = await api.get("/api/wallet/ledger");
            setLedgerEntries(ledgerRes.data);
        } catch (error) {
            console.error("Error fetching ledger data:", error);
        } finally {
            setLoading(false);
        }
    };

    const getTransactionColor = (type) => {
        switch (type) {
            case "CREDIT":
            case "TRADE_RELEASE":
                return "bg-green-500/20 text-green-500";
            case "DEBIT":
            case "FEE":
            case "TRADE_LOCK":
                return "bg-red-500/20 text-red-500";
            default:
                return "bg-gray-500/20 text-gray-500";
        }
    };

    if (loading) return <div className="p-8 text-center text-gray-400">Loading ledger data...</div>;

    return (
        <div className="p-4 lg:p-8 flex flex-col gap-6">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold tracking-tight text-white">Wallet Ledger</h1>
            </div>

            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                <Card className="bg-[#1a1a1a] text-white border-gray-800">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-gray-400">Total Balance</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">${wallet?.balance?.toFixed(2) || "0.00"}</div>
                    </CardContent>
                </Card>
                <Card className="bg-[#1a1a1a] text-white border-gray-800">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-gray-400">Locked Balance</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">${wallet?.lockedBalance?.toFixed(2) || "0.00"}</div>
                    </CardContent>
                </Card>
                <Card className="bg-[#1a1a1a] text-white border-gray-800">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium text-gray-400">Available Balance</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold text-green-400">
                            ${(wallet?.balance - (wallet?.lockedBalance || 0))?.toFixed(2) || "0.00"}
                        </div>
                    </CardContent>
                </Card>
            </div>

            <Card className="bg-[#1a1a1a] border-gray-800">
                <CardHeader>
                    <CardTitle className="text-white">Transaction History</CardTitle>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow className="border-gray-800 hover:bg-transparent">
                                <TableHead className="text-gray-400">Date</TableHead>
                                <TableHead className="text-gray-400">Type</TableHead>
                                <TableHead className="text-gray-400">Amount</TableHead>
                                <TableHead className="text-gray-400">Description</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {ledgerEntries.map((entry) => (
                                <TableRow key={entry.id} className="border-gray-800 hover:bg-[#252525]">
                                    <TableCell className="text-gray-300">
                                        {format(new Date(entry.createdAt), "dd MMM yyyy, HH:mm")}
                                    </TableCell>
                                    <TableCell>
                                        <Badge variant="outline" className={`border-0 ${getTransactionColor(entry.transactionType)}`}>
                                            {entry.transactionType}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="text-gray-300 font-medium">
                                        ${entry.amount.toFixed(4)}
                                    </TableCell>
                                    <TableCell className="text-gray-400">{entry.description}</TableCell>
                                </TableRow>
                            ))}
                            {ledgerEntries.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={4} className="text-center text-gray-500 py-8">
                                        No ledger entries found.
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>
        </div>
    );
};

export default WalletLedgerPage;
