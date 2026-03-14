import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface StockQuote {
  symbol: string;
  exchange: string;
  segment?: string;
  last_price?: number;
  day_change?: number;
  day_change_perc?: number;
  ohlc?: {
    open?: number;
    high?: number;
    low?: number;
    close?: number;
  };
  volume?: number;
  bid_price?: number;
  offer_price?: number;
  week_52_high?: number;
  week_52_low?: number;
  upper_circuit_limit?: number;
  lower_circuit_limit?: number;
  total_buy_quantity?: number;
  total_sell_quantity?: number;
  market_cap?: number;
  // FNO specific fields
  open_interest?: number;
  oi_day_change?: number;
  oi_day_change_percentage?: number;
  implied_volatility?: number;
  // Instrument metadata
  lot_size?: number;
}

@Component({
  selector: 'app-live-stock',
  templateUrl: './live-stock.component.html',
  styleUrls: ['./live-stock.component.scss']
})
export class LiveStockComponent {
  searchQuery = '';
  loading = false;
  stockData: StockQuote | null = null;
  error: string | null = null;
  apiAvailable = false;
  recentSearches: string[] = [];

  private readonly CHAT_BACKEND_URL = 'http://localhost:5000';

  constructor(private http: HttpClient, private snackBar: MatSnackBar) {
    this.checkApiHealth();
    this.loadRecentSearches();
  }

  checkApiHealth(): void {
    this.http.get<{ available: boolean }>(`${this.CHAT_BACKEND_URL}/api/stock/health`).subscribe({
      next: (res) => {
        this.apiAvailable = res.available;
        if (!res.available) {
          this.error = 'Groww API is not configured on the backend';
        }
      },
      error: () => {
        this.apiAvailable = false;
        this.error = 'Chat backend is not running. Start it on port 5000.';
      }
    });
  }

  searchStock(): void {
    if (!this.searchQuery.trim()) return;

    const symbol = this.searchQuery.trim().toUpperCase();
    this.loading = true;
    this.error = null;
    this.stockData = null;

    this.http.get<StockQuote>(`${this.CHAT_BACKEND_URL}/api/stock/search?q=${symbol}`).subscribe({
      next: (data) => {
        this.stockData = data;
        this.loading = false;
        this.addToRecentSearches(symbol);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || `Could not find stock: ${symbol}`;
        this.snackBar.open(this.error!, 'Close', { duration: 4000 });
      }
    });
  }

  quickSearch(symbol: string): void {
    this.searchQuery = symbol;
    this.searchStock();
  }

  refresh(): void {
    if (this.stockData) {
      this.searchQuery = this.stockData.symbol;
      this.searchStock();
    }
  }

  private addToRecentSearches(symbol: string): void {
    this.recentSearches = [symbol, ...this.recentSearches.filter(s => s !== symbol)].slice(0, 5);
    localStorage.setItem('recentStockSearches', JSON.stringify(this.recentSearches));
  }

  private loadRecentSearches(): void {
    const saved = localStorage.getItem('recentStockSearches');
    if (saved) {
      this.recentSearches = JSON.parse(saved);
    }
  }

  formatNumber(value: number | null | undefined): string {
    if (value == null) return '-';
    return value.toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  formatCurrency(value: number | null | undefined): string {
    if (value == null) return '-';
    return '₹' + value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatMarketCap(value: number | null | undefined): string {
    if (value == null) return '-';
    if (value >= 1e12) return '₹' + (value / 1e12).toFixed(2) + ' T';
    if (value >= 1e9) return '₹' + (value / 1e9).toFixed(2) + ' B';
    if (value >= 1e7) return '₹' + (value / 1e7).toFixed(2) + ' Cr';
    if (value >= 1e5) return '₹' + (value / 1e5).toFixed(2) + ' L';
    return '₹' + value.toLocaleString('en-IN');
  }
}
