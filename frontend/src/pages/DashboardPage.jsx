import React, { useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ErrorState } from '../components/EmptyState';
import { Skeleton } from '../components/LoadingSpinner';
import { downloadBlob } from '../lib/download';
import {
  useDashboardStats,
  useExportDashboard,
} from '../features/dashboard/api';
import { useTransactionsByMonth } from '../features/transactions/api';

const transformTransactionData = (transactions, month, year) => {
  const dailyData = {};
  const daysInMonth = new Date(year, month, 0).getDate();

  for (let day = 1; day <= daysInMonth; day++) {
    dailyData[day] = { day, amount: 0 };
  }

  if (!Array.isArray(transactions)) {
    return Object.values(dailyData);
  }

  transactions.forEach((transaction) => {
    const type = transaction.transactionType || transaction.type;
    if (type === 'RETURN_TO_SUPPLIER') return;

    const timestamp = transaction.createdAt || transaction.transactionDate;
    if (!timestamp) return;

    const date = new Date(timestamp);
    if (date.getMonth() + 1 !== month || date.getFullYear() !== year) return;

    const day = date.getDate();
    const amount = Number(transaction.totalPrice ?? 0);
    dailyData[day].amount += type === 'SALE' ? amount : -amount;
  });

  return Object.values(dailyData);
};

function useMediaQuery(query) {
  const [matches, setMatches] = useState(
    () => window.matchMedia(query).matches
  );

  React.useEffect(() => {
    const media = window.matchMedia(query);
    const handleChange = () => setMatches(media.matches);

    media.addEventListener('change', handleChange);
    return () => media.removeEventListener('change', handleChange);
  }, [query]);

  return matches;
}

function formatCurrency(value) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatNumber(value) {
  return Number(value ?? 0).toLocaleString();
}

function getUtilizationTone(percent) {
  if (percent >= 100) return 'bg-error';
  if (percent >= 85) return 'bg-warning';
  return 'bg-success';
}

function getWarehousePercent(warehouse) {
  if (!warehouse.capacity) return 0;
  return Math.min(100, Math.round((warehouse.used / warehouse.capacity) * 100));
}

function WarehouseTooltip({ active, payload }) {
  if (!active || !payload?.length) return null;

  const warehouse = payload[0]?.payload;
  if (!warehouse) return null;

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-3 text-sm shadow-lg">
      <p className="mb-2 font-semibold text-slate-900">{warehouse.name}</p>
      <div className="space-y-1">
        <p className="flex items-center justify-between gap-6 text-slate-900">
          <span className="font-medium">Used</span>
          <span>{formatNumber(warehouse.used)} units</span>
        </p>
        <p className="flex items-center justify-between gap-6 text-slate-900">
          <span className="font-medium">Remaining</span>
          <span>{formatNumber(warehouse.remaining)} units</span>
        </p>
        <p className="flex items-center justify-between gap-6 text-slate-600">
          <span>Capacity</span>
          <span>{formatNumber(warehouse.capacity)} units</span>
        </p>
      </div>
    </div>
  );
}

function StatCard({ label, value, tone = 'primary' }) {
  const toneClasses = {
    primary: 'border-primary/20 bg-primary-subtle text-primary',
    warning: 'border-warning/20 bg-warning-light text-warning',
    error: 'border-error/20 bg-error-light text-error',
    success: 'border-success/20 bg-success-light text-success',
  };

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-soft">
      <div
        className={`mb-4 h-2 w-12 rounded-full ${toneClasses[tone] || toneClasses.primary}`}
      />
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-display font-bold text-slate-900">
        {value}
      </p>
    </div>
  );
}

function DashboardPage() {
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [exportError, setExportError] = useState('');
  const isPhone = useMediaQuery('(max-width: 767px)');

  const exportMutation = useExportDashboard();
  const userRole = localStorage.getItem('userRole');
  const canExportDashboard = userRole === 'ADMIN' || userRole === 'MANAGER';

  const handleExport = () => {
    setExportError('');
    exportMutation.mutate(undefined, {
      onSuccess: (blob) => {
        downloadBlob(blob, 'dashboard-stats.csv');
      },
      onError: (err) => setExportError(err.message || 'Failed to export CSV.'),
    });
  };

  const {
    data: statsResponse,
    isLoading: isStatsLoading,
    isError: isStatsError,
    error: statsError,
  } = useDashboardStats();

  const {
    data: transactionResponse,
    isLoading: isChartLoading,
    isError: isChartError,
    error: chartError,
  } = useTransactionsByMonth(selectedMonth, selectedYear);

  const dashboardStats = statsResponse?.dashboardStats;

  const chartData = useMemo(() => {
    const transactions = transactionResponse?.transactions ?? [];
    return transformTransactionData(transactions, selectedMonth, selectedYear);
  }, [transactionResponse, selectedMonth, selectedYear]);

  const chartSummary = useMemo(() => {
    const amounts = chartData.map((entry) => Number(entry.amount ?? 0));
    const total = amounts.reduce((sum, amount) => sum + amount, 0);
    const best = chartData.reduce(
      (currentBest, entry) =>
        entry.amount > currentBest.amount ? entry : currentBest,
      { day: '-', amount: Number.NEGATIVE_INFINITY }
    );
    const worst = chartData.reduce(
      (currentWorst, entry) =>
        entry.amount < currentWorst.amount ? entry : currentWorst,
      { day: '-', amount: Number.POSITIVE_INFINITY }
    );

    return {
      total,
      best: best.amount === Number.NEGATIVE_INFINITY ? null : best,
      worst: worst.amount === Number.POSITIVE_INFINITY ? null : worst,
    };
  }, [chartData]);

  const warehouseData = useMemo(() => {
    return (dashboardStats?.warehouseStats ?? []).map((warehouse) => {
      const capacity = Number(warehouse.capacity ?? 0);
      const used = Number(warehouse.totalQuantity ?? 0);
      return {
        id: warehouse.id,
        name: warehouse.name,
        capacity,
        used,
        remaining: Math.max(0, capacity - used),
      };
    });
  }, [dashboardStats]);

  const handleMonthChange = (e) =>
    setSelectedMonth(parseInt(e.target.value, 10));
  const handleYearChange = (e) => setSelectedYear(parseInt(e.target.value, 10));

  // Keep error handling - only return early on error
  if (isStatsError) {
    return (
      <ErrorState
        title="Failed to load dashboard"
        message={statsError?.message || 'Unable to load dashboard metrics.'}
      />
    );
  }

  return (
    <div>
      <header className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900">
            Dashboard
          </h1>
          <p className="mt-2 text-sm text-slate-500">
            Inventory health, stock value, and warehouse capacity at a glance.
          </p>
        </div>
        {canExportDashboard && (
          <button
            type="button"
            onClick={handleExport}
            disabled={exportMutation.isPending}
            className="flex items-center gap-1.5 sm:gap-2 text-slate-700 bg-white border-2 border-slate-200 hover:border-primary hover:text-primary px-3 py-2 sm:px-4 sm:py-2.5 rounded-lg sm:rounded-xl text-xs sm:text-sm font-semibold transition-all duration-200 disabled:opacity-60"
          >
            {exportMutation.isPending ? 'Exporting…' : 'Export CSV'}
          </button>
        )}
      </header>
      {exportError && (
        <div className="mb-4 rounded-xl border-2 border-error bg-error-light px-4 py-3 text-sm text-error font-medium">
          {exportError}
        </div>
      )}

      <section className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {isStatsLoading ? (
          <>
            {[...Array(6)].map((_, i) => (
              <div
                key={i}
                className="rounded-xl border border-slate-200 bg-white p-5 shadow-soft"
              >
                <Skeleton className="mb-4 w-12 h-2" variant="text" />
                <Skeleton className="w-24 h-4" variant="text" />
                <Skeleton className="mt-2 w-32 h-8" variant="text" />
              </div>
            ))}
          </>
        ) : (
          <>
            <StatCard
              label="Total Products"
              value={formatNumber(dashboardStats?.totalActiveProducts)}
            />
            <StatCard
              label="Low Stock"
              value={formatNumber(dashboardStats?.lowStockCount)}
              tone="warning"
            />
            <StatCard
              label="Out of Stock"
              value={formatNumber(dashboardStats?.outOfStockCount)}
              tone="error"
            />
            <StatCard
              label="Stock Value (Retail)"
              value={formatCurrency(dashboardStats?.totalStockValue)}
              tone="success"
            />
            <StatCard
              label="Inventory Cost"
              value={formatCurrency(dashboardStats?.inventoryCost)}
              tone="primary"
            />
            <StatCard
              label="Estimated Margin"
              value={formatCurrency(dashboardStats?.estimatedMargin)}
              tone={
                (dashboardStats?.estimatedMargin ?? 0) >= 0
                  ? 'success'
                  : 'error'
              }
            />
          </>
        )}
      </section>

      <section className="mb-8">
        <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">
              Net Daily Sales
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Daily sales minus purchase spend for the selected month.
            </p>
          </div>
          <div className="flex gap-3">
            <select
              value={selectedMonth}
              onChange={handleMonthChange}
              aria-label="Select month"
              className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-medium text-slate-700 transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
            >
              {Array.from({ length: 12 }, (_, i) => (
                <option key={i + 1} value={i + 1}>
                  {new Date(0, i).toLocaleString('default', {
                    month: 'long',
                  })}
                </option>
              ))}
            </select>

            <select
              value={selectedYear}
              onChange={handleYearChange}
              aria-label="Select year"
              className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-medium text-slate-700 transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
            >
              {Array.from({ length: 5 }, (_, i) => {
                const year = new Date().getFullYear() - i;
                return (
                  <option key={year} value={year}>
                    {year}
                  </option>
                );
              })}
            </select>
          </div>
        </div>

        <div className="min-w-0 rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
          {/* Show skeleton while chart data loads - independent of stats loading */}
          {isChartLoading ? (
            <div className="space-y-4">
              <div className="flex gap-4">
                <Skeleton className="flex-1 h-16 rounded-lg" variant="card" />
                <Skeleton className="flex-1 h-16 rounded-lg" variant="card" />
              </div>
              <Skeleton className="w-full h-64 rounded-lg" variant="card" />
            </div>
          ) : isChartError ? (
            <div className="flex min-h-[280px] items-center justify-center text-sm text-error md:min-h-[360px]">
              {chartError?.message || 'Unable to load transaction chart.'}
            </div>
          ) : isPhone ? (
            <div className="space-y-5">
              <div>
                <p className="text-sm font-medium text-slate-500">
                  Net this month
                </p>
                <p className="mt-1 text-3xl font-display font-bold text-slate-900">
                  {formatCurrency(chartSummary.total)}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-slate-50 p-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Best day
                  </p>
                  <p className="mt-1 text-sm font-semibold text-slate-900">
                    Day {chartSummary.best?.day ?? '-'}
                  </p>
                  <p className="text-sm text-success">
                    {formatCurrency(chartSummary.best?.amount)}
                  </p>
                </div>
                <div className="rounded-lg bg-slate-50 p-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Weakest day
                  </p>
                  <p className="mt-1 text-sm font-semibold text-slate-900">
                    Day {chartSummary.worst?.day ?? '-'}
                  </p>
                  <p className="text-sm text-error">
                    {formatCurrency(chartSummary.worst?.amount)}
                  </p>
                </div>
              </div>

              <div className="min-w-0">
                <ResponsiveContainer width="100%" height={160}>
                  <LineChart
                    data={chartData}
                    margin={{ top: 8, right: 4, left: 4, bottom: 8 }}
                  >
                    <CartesianGrid
                      strokeDasharray="3 3"
                      vertical={false}
                      stroke="#e2e8f0"
                    />
                    <XAxis
                      dataKey="day"
                      axisLine={{ stroke: '#94a3b8' }}
                      tickLine={false}
                      tick={{ fill: '#64748b', fontSize: 10 }}
                      interval="preserveStartEnd"
                      minTickGap={30}
                    />
                    <YAxis
                      axisLine={{ stroke: '#94a3b8' }}
                      tickLine={false}
                      tickFormatter={(value) =>
                        `$${(value / 1000).toFixed(0)}k`
                      }
                      tick={{ fill: '#64748b', fontSize: 10 }}
                      width={36}
                    />
                    <Tooltip
                      contentStyle={{
                        borderRadius: '8px',
                        border: '1px solid #e2e8f0',
                        boxShadow: '0 4px 12px -2px rgb(0 0 0 / 0.12)',
                      }}
                      formatter={(value) => [
                        formatCurrency(value),
                        'Net Sales',
                      ]}
                      labelFormatter={(day) => `Day ${day}`}
                    />
                    <Line
                      type="monotone"
                      dataKey="amount"
                      name="Net Sales"
                      stroke="#3E3998"
                      strokeWidth={3}
                      dot={false}
                      activeDot={{ r: 5 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          ) : (
            <div className="min-w-0">
              <ResponsiveContainer width="100%" height={360}>
                <LineChart data={chartData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    vertical={false}
                    stroke="#e2e8f0"
                  />
                  <XAxis
                    dataKey="day"
                    axisLine={{ stroke: '#94a3b8' }}
                    tickLine={false}
                    tick={{ fill: '#64748b', fontSize: 12 }}
                  />
                  <YAxis
                    axisLine={{ stroke: '#94a3b8' }}
                    tickLine={false}
                    tickFormatter={(value) =>
                      `$${Math.round(value).toLocaleString()}`
                    }
                    tick={{ fill: '#64748b', fontSize: 12 }}
                  />
                  <Tooltip
                    contentStyle={{
                      borderRadius: '8px',
                      border: '1px solid #e2e8f0',
                      boxShadow: '0 4px 12px -2px rgb(0 0 0 / 0.12)',
                    }}
                    formatter={(value) => [formatCurrency(value), 'Net Sales']}
                  />
                  <Line
                    type="monotone"
                    dataKey="amount"
                    name="Net Sales"
                    stroke="#3E3998"
                    strokeWidth={3}
                    dot={{ fill: '#3E3998', r: 3 }}
                    activeDot={{ r: 6 }}
                    animationDuration={900}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </section>

      <section>
        <div className="mb-4">
          <h2 className="text-xl font-semibold text-slate-900">
            Warehouse Utilization
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Stock quantity currently assigned to each active warehouse.
          </p>
        </div>

        <div className="min-w-0 rounded-xl border border-slate-200 bg-white p-4 shadow-soft">
          {/* Show skeleton while stats (including warehouse data) loads */}
          {isStatsLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="space-y-2">
                  <Skeleton className="w-1/3 h-4" variant="text" />
                  <Skeleton className="w-full h-4" variant="text" />
                  <Skeleton className="w-full h-2" variant="text" />
                </div>
              ))}
            </div>
          ) : isPhone ? (
            <div className="space-y-4">
              {warehouseData.map((warehouse) => {
                const percent = getWarehousePercent(warehouse);
                return (
                  <div key={warehouse.id} className="space-y-2">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {warehouse.name}
                        </p>
                        <p className="text-xs text-slate-500">
                          {formatNumber(warehouse.used)} /{' '}
                          {formatNumber(warehouse.capacity)} units
                        </p>
                      </div>
                      <span className="shrink-0 text-sm font-semibold text-slate-900">
                        {percent}%
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-200">
                      <div
                        className={`h-full rounded-full ${getUtilizationTone(percent)}`}
                        style={{ width: `${percent}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="min-w-0">
              <ResponsiveContainer width="100%" height={360}>
                <BarChart
                  data={warehouseData}
                  layout="vertical"
                  margin={{ top: 12, right: 24, left: 24, bottom: 12 }}
                >
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                  <XAxis
                    type="number"
                    tick={{ fill: '#334155', fontSize: 12 }}
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={130}
                    tick={{ fill: '#0f172a', fontSize: 12 }}
                  />
                  <Tooltip content={<WarehouseTooltip />} />
                  <Bar
                    dataKey="used"
                    stackId="warehouse"
                    name="Used"
                    fill="#3E3998"
                    radius={[4, 0, 0, 4]}
                  />
                  <Bar
                    dataKey="remaining"
                    stackId="warehouse"
                    name="Remaining"
                    fill="#cbd5e1"
                    radius={[0, 4, 4, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

export default DashboardPage;
