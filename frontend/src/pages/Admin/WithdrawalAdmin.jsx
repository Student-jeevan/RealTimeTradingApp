import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  getAllWithdrawalRequest,
  approveWithdrawal,
  rejectWithdrawal,
} from '@/State/Withdrawal/Action';
import {
  Table, TableBody, TableCell,
  TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  CheckCircle2, XCircle, Clock, Loader2,
  RefreshCw, ShieldCheck, AlertCircle,
} from 'lucide-react';

// ─── Status Badge ────────────────────────────────────────────────────────────
const StatusBadge = ({ status }) => {
  const map = {
    PENDING:  { icon: Clock,         cls: 'text-amber-400  bg-amber-500/10',  label: 'Pending'  },
    SUCCESS:  { icon: CheckCircle2,  cls: 'text-emerald-400 bg-emerald-500/10', label: 'Approved' },
    APPROVED: { icon: CheckCircle2,  cls: 'text-emerald-400 bg-emerald-500/10', label: 'Approved' },
    DECLINE:  { icon: XCircle,       cls: 'text-red-400    bg-red-500/10',    label: 'Rejected' },
    REJECTED: { icon: XCircle,       cls: 'text-red-400    bg-red-500/10',    label: 'Rejected' },
  };
  const cfg = map[status] ?? map.PENDING;
  const Icon = cfg.icon;
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold ${cfg.cls}`}>
      <Icon className="w-3.5 h-3.5" />
      {cfg.label}
    </span>
  );
};

// ─── Main Component ───────────────────────────────────────────────────────────
const WithdrawalAdmin = () => {
  const dispatch = useDispatch();
  const { withdrawalRequests, loading, error } = useSelector(s => s.withdrawal);
  const jwt = localStorage.getItem('jwt');

  const [actingId, setActingId] = useState(null); // which row is being processed

  useEffect(() => {
    if (jwt) dispatch(getAllWithdrawalRequest(jwt));
  }, [dispatch, jwt]);

  const handleApprove = async (id) => {
    setActingId(id);
    await dispatch(approveWithdrawal({ id, jwt }));
    setActingId(null);
  };

  const handleReject = async (id) => {
    setActingId(id);
    await dispatch(rejectWithdrawal({ id, jwt }));
    setActingId(null);
  };

  const pending   = (withdrawalRequests || []).filter(w => w.status === 'PENDING');
  const processed = (withdrawalRequests || []).filter(w => w.status !== 'PENDING');

  return (
    <div className="space-y-6 animate-fade-in-up">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-primary" />
            <h1 className="text-2xl font-bold">Withdrawal Requests</h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Review and act on user withdrawal requests
          </p>
        </div>
        <button
          onClick={() => dispatch(getAllWithdrawalRequest(jwt))}
          className="p-2 rounded-xl hover:bg-secondary transition-colors"
          title="Refresh"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* ── Error Banner ── */}
      {error && (
        <div className="flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {/* ── Stats ── */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Pending',  value: pending.length,   cls: 'text-amber-400'   },
          { label: 'Approved', value: processed.filter(w => ['SUCCESS','APPROVED'].includes(w.status)).length, cls: 'text-emerald-400' },
          { label: 'Rejected', value: processed.filter(w => ['DECLINE','REJECTED'].includes(w.status)).length, cls: 'text-red-400'     },
        ].map(({ label, value, cls }) => (
          <div key={label} className="glass-card rounded-2xl p-4 text-center">
            <p className={`text-2xl font-bold ${cls}`}>{value}</p>
            <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
          </div>
        ))}
      </div>

      {/* ── PENDING TABLE ── */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="px-5 pt-5 pb-3 flex items-center gap-2">
          <Clock className="w-4 h-4 text-amber-400" />
          <h3 className="text-sm font-semibold text-muted-foreground">Awaiting Approval</h3>
          {pending.length > 0 && (
            <span className="ml-auto text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400">
              {pending.length} pending
            </span>
          )}
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-12">
            <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
          </div>
        ) : pending.length === 0 ? (
          <p className="text-center text-muted-foreground text-sm py-10">
            No pending withdrawal requests 🎉
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="border-b border-border/20 hover:bg-transparent">
                <TableHead className="text-[11px]">User</TableHead>
                <TableHead className="text-[11px]">Amount</TableHead>
                <TableHead className="text-[11px]">Requested</TableHead>
                <TableHead className="text-[11px]">Status</TableHead>
                <TableHead className="text-right text-[11px]">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {pending.map(w => {
                const isActing = actingId === w.Id;
                const date = w.date ? new Date(w.date).toLocaleString() : '—';
                const initials = w.user?.fullName?.charAt(0)?.toUpperCase() ?? '?';
                return (
                  <TableRow key={w.Id} className="border-b border-border/10">
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Avatar className="h-7 w-7">
                          <AvatarFallback className="text-[10px] bg-primary/10 text-primary">
                            {initials}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <p className="text-xs font-semibold">{w.user?.fullName ?? 'Unknown'}</p>
                          <p className="text-[10px] text-muted-foreground">{w.user?.email}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="text-sm font-bold">${w.amount?.toLocaleString()}</span>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">{date}</TableCell>
                    <TableCell><StatusBadge status={w.status} /></TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleApprove(w.Id)}
                          disabled={isActing}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold
                            bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20
                            disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          {isActing ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                          Approve
                        </button>
                        <button
                          onClick={() => handleReject(w.Id)}
                          disabled={isActing}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold
                            bg-red-500/10 text-red-400 hover:bg-red-500/20
                            disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          {isActing ? <Loader2 className="w-3 h-3 animate-spin" /> : <XCircle className="w-3.5 h-3.5" />}
                          Reject
                        </button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </div>

      {/* ── PROCESSED TABLE ── */}
      {processed.length > 0 && (
        <div className="glass-card rounded-2xl overflow-hidden">
          <div className="px-5 pt-5 pb-3">
            <h3 className="text-sm font-semibold text-muted-foreground">Processed Requests</h3>
          </div>
          <Table>
            <TableHeader>
              <TableRow className="border-b border-border/20 hover:bg-transparent">
                <TableHead className="text-[11px]">User</TableHead>
                <TableHead className="text-[11px]">Amount</TableHead>
                <TableHead className="text-[11px]">Date</TableHead>
                <TableHead className="text-[11px]">Status</TableHead>
                <TableHead className="text-[11px]">Approved By</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {processed.map(w => (
                <TableRow key={w.Id} className="border-b border-border/10 opacity-70">
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Avatar className="h-6 w-6">
                        <AvatarFallback className="text-[9px]">
                          {w.user?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
                        </AvatarFallback>
                      </Avatar>
                      <span className="text-xs">{w.user?.fullName ?? 'Unknown'}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-xs font-semibold">${w.amount?.toLocaleString()}</TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {w.approvedAt ? new Date(w.approvedAt).toLocaleString() : '—'}
                  </TableCell>
                  <TableCell><StatusBadge status={w.status} /></TableCell>
                  <TableCell className="text-xs text-muted-foreground">{w.approvedBy ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
};

export default WithdrawalAdmin;
