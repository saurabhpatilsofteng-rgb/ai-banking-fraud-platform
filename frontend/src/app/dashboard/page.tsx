
"use client";

import { useCallback, useEffect, useState } from "react";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type Account = {
  id: number;
  maskedAccountNumber: string;
  balance: number;
  currency: string;
  status: string;
  createdAt: string;
};

type Transaction = {
  id: number;
  fromAccountId: number;
  toAccountId: number | null;
  amount: number;
  type: "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";
  status: string;
  createdAt: string;
};

type FraudAssessment = {
  assessmentId: number;
  transactionId: number;
  riskScore: number;
  riskLevel: string;
  reviewRecommended: boolean;
  modelType: string;
  assessedAt: string;
};

export default function DashboardPage() {
  const [token, setToken] = useState("");
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [assessments, setAssessments] = useState<FraudAssessment[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const [fromAccountId, setFromAccountId] = useState("");
const [toAccountId, setToAccountId] = useState("");
const [transferAmount, setTransferAmount] = useState("");

  const loadDashboard = useCallback(async (accessToken: string) => {
    try {
      setError("");

      const headers = {
        Authorization: `Bearer ${accessToken}`,
      };

      const [accountResponse, transactionResponse, fraudResponse] =
        await Promise.all([
          fetch(`${API_URL}/api/accounts/me`, { headers }),
          fetch(`${API_URL}/api/transactions`, { headers }),
          fetch(`${API_URL}/api/fraud/my-assessments`, { headers }),
        ]);

      const responses = [
        accountResponse,
        transactionResponse,
        fraudResponse,
      ];

      if (responses.some((response) => response.status === 401)) {
        sessionStorage.removeItem("accessToken");
        window.location.href = "/";
        return;
      }

      if (responses.some((response) => !response.ok)) {
        throw new Error(
          "Unable to load dashboard data. Check your backend and API permissions."
        );
      }

      const [accountData, transactionData, fraudData] =
        await Promise.all(
          responses.map((response) => response.json())
        );

      setAccounts(accountData);
      setTransactions(transactionData);
      setAssessments(fraudData);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Something went wrong."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const accessToken = sessionStorage.getItem("accessToken");

    if (!accessToken) {
      window.location.href = "/";
      return;
    }

    setToken(accessToken);
    void loadDashboard(accessToken);
  }, [loadDashboard]);

  async function createAccount() {
    if (!token || busy) return;

    setBusy(true);
    setError("");

    try {
      const response = await fetch(`${API_URL}/api/accounts/me`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error("Could not create the account.");
      }

      await loadDashboard(token);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Account creation failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function assessFraud(transactionId: number) {
    if (!token || busy) return;

    setBusy(true);
    setError("");

    try {
      const response = await fetch(
        `${API_URL}/api/fraud/transactions/${transactionId}/assess`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 401) {
        sessionStorage.removeItem("accessToken");
        window.location.href = "/";
        return;
      }

      if (!response.ok) {
        throw new Error(
          "Fraud assessment failed. Check whether the Python ML service is running."
        );
      }

      await loadDashboard(token);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Fraud assessment failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function transferMoney() {
  if (!token || busy) return;

  const sourceId = Number(fromAccountId);
  const destinationId = Number(toAccountId);
  const amount = Number(transferAmount);

  if (
    !fromAccountId ||
    !toAccountId ||
    sourceId === destinationId ||
    !Number.isFinite(amount) ||
    amount <= 0 ||
    !/^\d+(\.\d{1,2})?$/.test(transferAmount)
  ) {
    setError(
      "Select two different accounts and enter a positive amount with up to two decimal places."
    );
    return;
  }

  setBusy(true);
  setError("");

  try {
    const response = await fetch(`${API_URL}/api/banking/transfer`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        fromAccountId: sourceId,
        toAccountId: destinationId,
        amount,
      }),
    });

    if (response.status === 401) {
      sessionStorage.removeItem("accessToken");
      window.location.href = "/";
      return;
    }

    if (!response.ok) {
      const message = await response.text();
      throw new Error(
        message || `Transfer failed with HTTP ${response.status}.`
      );
    }

    setTransferAmount("");
    setFromAccountId("");
    setToAccountId("");

    await loadDashboard(token);
  } catch (err) {
    setError(
      err instanceof Error ? err.message : "Transfer failed."
    );
  } finally {
    setBusy(false);
  }
}

  function logout() {
    sessionStorage.removeItem("accessToken");
    window.location.href = "/";
  }

  function riskColor(level: string) {
    switch (level.toUpperCase()) {
      case "HIGH":
        return "bg-red-100 text-red-700";
      case "MEDIUM":
        return "bg-amber-100 text-amber-700";
      default:
        return "bg-green-100 text-green-700";
    }
  }

  const totalBalance = accounts.reduce(
    (total, account) => total + Number(account.balance),
    0
  );

  if (loading) {
    return (
      <main className="min-h-screen bg-slate-950 p-8 text-white">
        Loading banking dashboard...
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-950 p-4 text-white sm:p-8">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-cyan-400">
              AI Banking Dashboard
            </h1>
            <p className="mt-2 text-slate-400">
              Accounts, transactions and fraud risk monitoring
            </p>
          </div>

          <button
            onClick={logout}
            className="rounded-lg border border-slate-600 px-4 py-2 hover:bg-slate-800"
          >
            Logout
          </button>
        </header>

        {error && (
          <div
            role="alert"
            className="rounded-lg border border-red-500/40 bg-red-950/50 p-4 text-red-300"
          >
            {error}
          </div>
        )}

        <section className="grid gap-4 sm:grid-cols-2">
          <div className="rounded-xl border border-slate-800 bg-slate-900 p-6">
            <p className="text-sm text-slate-400">Total account balance</p>
            <p className="mt-3 text-3xl font-bold text-cyan-400">
              ₹{totalBalance.toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900 p-6">
            <p className="text-sm text-slate-400">My accounts</p>
            <p className="mt-3 text-3xl font-bold">{accounts.length}</p>
          </div>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-xl font-semibold">My Accounts</h2>

            <button
              onClick={createAccount}
              disabled={busy}
              className="rounded-lg bg-cyan-500 px-4 py-2 font-semibold text-slate-950 disabled:opacity-50"
            >
              {busy ? "Please wait..." : "+ Create Account"}
            </button>
          </div>

          {accounts.length === 0 ? (
            <p className="text-slate-400">
              No accounts yet. Create your first demo account.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-slate-400">
                  <tr>
                    <th className="p-3">Account</th>
                    <th className="p-3">Balance</th>
                    <th className="p-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {accounts.map((account) => (
                    <tr
                      key={account.id}
                      className="border-t border-slate-800"
                    >
                      <td className="p-3">
                        {account.maskedAccountNumber}
                      </td>
                      <td className="p-3">
                        {account.currency}{" "}
                        {Number(account.balance).toLocaleString("en-IN", {
                          minimumFractionDigits: 2,
                        })}
                      </td>
                      <td className="p-3">{account.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
  <h2 className="mb-5 text-xl font-semibold">Transfer Money</h2>

  {accounts.length < 2 ? (
    <p className="text-slate-400">
      You need at least two accounts to make an internal transfer.
      Create another demo account to test this feature.
    </p>
  ) : (
    <form
      className="grid gap-4 md:grid-cols-3"
      onSubmit={(event) => {
        event.preventDefault();
        void transferMoney();
      }}
    >
      <label className="space-y-2">
        <span className="text-sm text-slate-300">From account</span>
        <select
          required
          value={fromAccountId}
          onChange={(event) => setFromAccountId(event.target.value)}
          className="w-full rounded-lg border border-slate-700 bg-slate-950 p-3"
        >
          <option value="">Select source</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.maskedAccountNumber} — ₹
              {Number(account.balance).toFixed(2)}
            </option>
          ))}
        </select>
      </label>

      <label className="space-y-2">
        <span className="text-sm text-slate-300">To account</span>
        <select
          required
          value={toAccountId}
          onChange={(event) => setToAccountId(event.target.value)}
          className="w-full rounded-lg border border-slate-700 bg-slate-950 p-3"
        >
          <option value="">Select destination</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.maskedAccountNumber}
            </option>
          ))}
        </select>
      </label>

      <label className="space-y-2">
        <span className="text-sm text-slate-300">Amount (INR)</span>
        <input
          required
          type="number"
          min="0.01"
          step="0.01"
          value={transferAmount}
          onChange={(event) => setTransferAmount(event.target.value)}
          placeholder="Enter amount"
          className="w-full rounded-lg border border-slate-700 bg-slate-950 p-3"
        />
      </label>

      <div className="md:col-span-3">
        <button
          type="submit"
          disabled={busy || accounts.length < 2}
          className="rounded-lg bg-cyan-500 px-5 py-3 font-semibold text-slate-950 disabled:opacity-50"
        >
          {busy ? "Processing..." : "Transfer Money"}
        </button>
      </div>
    </form>
  )}
</section>

        <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
          <h2 className="mb-5 text-xl font-semibold">Transaction History</h2>

          {transactions.length === 0 ? (
            <p className="text-slate-400">
              No transactions found. Transactions will appear here when
              available.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-slate-400">
                  <tr>
                    <th className="p-3">ID</th>
                    <th className="p-3">Type</th>
                    <th className="p-3">Amount</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Date</th>
                    <th className="p-3">Fraud check</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((transaction) => (
                    <tr
                      key={transaction.id}
                      className="border-t border-slate-800"
                    >
                      <td className="p-3">{transaction.id}</td>
                      <td className="p-3">{transaction.type}</td>
                      <td className="p-3">
                        ₹{Number(transaction.amount).toLocaleString("en-IN", {
                          minimumFractionDigits: 2,
                        })}
                      </td>
                      <td className="p-3">{transaction.status}</td>
                      <td className="p-3">
                        {new Date(transaction.createdAt).toLocaleString()}
                      </td>
                      <td className="p-3">
                        <button
                          onClick={() => assessFraud(transaction.id)}
                          disabled={busy}
                          className="whitespace-nowrap rounded-lg border border-cyan-500 px-3 py-2 text-cyan-300 hover:bg-cyan-950 disabled:opacity-50"
                        >
                          Assess risk
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900 p-6">
          <h2 className="mb-5 text-xl font-semibold">
            Fraud Assessment History
          </h2>

          {assessments.length === 0 ? (
            <p className="text-slate-400">
              No assessments yet. Select Assess risk on a transaction to
              request a model prediction.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-slate-400">
                  <tr>
                    <th className="p-3">Transaction</th>
                    <th className="p-3">Risk score</th>
                    <th className="p-3">Risk level</th>
                    <th className="p-3">Review recommended</th>
                    <th className="p-3">Assessed at</th>
                  </tr>
                </thead>
                <tbody>
                  {assessments.map((assessment) => (
                    <tr
                      key={assessment.assessmentId}
                      className="border-t border-slate-800"
                    >
                      <td className="p-3">{assessment.transactionId}</td>
                      <td className="p-3">
                        {assessment.riskScore.toFixed(2)}%
                      </td>
                      <td className="p-3">
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold ${riskColor(assessment.riskLevel)}`}
                        >
                          {assessment.riskLevel}
                        </span>
                      </td>
                      <td className="p-3">
                        {assessment.reviewRecommended ? "Yes" : "No"}
                      </td>
                      <td className="p-3">
                        {new Date(assessment.assessedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <p className="mt-4 text-xs text-slate-500">
            Risk scores are demo-model predictions, not proof of fraud or
            a final banking decision.
          </p>
        </section>
      </div>
    </main>
  );
}
