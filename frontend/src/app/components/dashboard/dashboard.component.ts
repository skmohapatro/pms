import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { interval, Subscription, forkJoin } from 'rxjs';

export interface DashboardSummary {
  totalInvested: number;
  totalCompanies: number;
  totalPnl: number;
  portfolioValue: number;
  watchlistCount: number;
  topGainers: StockMover[];
  topLosers: StockMover[];
}

export interface StockMover {
  symbol: string;
  changePercent: number;
  lastPrice: number;
  source: string; // "portfolio" | "watchlist"
}

export interface NewsArticle {
  title: string;
  source: string;
  url: string;
  publishedDate: string;
  snippet: string;
  relevantHoldings: string[];
  relevantWatchlist: string[];
  category: string; // "portfolio" | "watchlist" | "general"
}

export interface DashboardInsights {
  marketSummary: string;
  portfolioInsights: string[];
  watchlistAlerts: string[];
  riskAlerts: string[];
  headlines: string[];
}

export interface WatchlistAlert {
  symbol: string;
  watchlistName: string;
  priceChange: number;
  priceChangePercent: number;
  lastPrice: number;
  newsCount: number;
  alertType: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  summary: DashboardSummary | null = null;
  news: NewsArticle[] = [];
  filteredNews: NewsArticle[] = [];
  insights: DashboardInsights | null = null;
  watchlistAlerts: WatchlistAlert[] = [];

  summaryLoading = true;
  newsLoading = true;
  insightsLoading = true;

  newsFilter: 'all' | 'portfolio' | 'watchlist' = 'all';

  topGainers: StockMover[] = [];
  topLosers: StockMover[] = [];

  private refreshSub: Subscription | null = null;
  private readonly CHAT_BACKEND_URL = 'http://localhost:5000';

  constructor(
    private api: ApiService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadDashboard();

    // Auto-refresh every 15 minutes
    this.refreshSub = interval(15 * 60 * 1000).subscribe(() => {
      this.loadNews();
      this.loadInsights();
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSub) {
      this.refreshSub.unsubscribe();
    }
  }

  loadDashboard(): void {
    this.loadSummary();
    this.loadNews();
    this.loadInsights();
    this.loadWatchlistAlerts();
  }

  loadSummary(): void {
    this.summaryLoading = true;
    this.api.getDashboardSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.summaryLoading = false;
        this.loadLivePrices();
      },
      error: () => {
        this.summaryLoading = false;
      }
    });
  }

  loadNews(): void {
    this.newsLoading = true;
    this.api.getDashboardNews().subscribe({
      next: (data) => {
        this.news = data;
        this.applyNewsFilter();
        this.newsLoading = false;
      },
      error: () => {
        this.newsLoading = false;
      }
    });
  }

  loadInsights(): void {
    this.insightsLoading = true;
    this.api.getDashboardInsights().subscribe({
      next: (data) => {
        this.insights = data;
        this.insightsLoading = false;
      },
      error: () => {
        this.insightsLoading = false;
      }
    });
  }

  loadWatchlistAlerts(): void {
    this.api.getDashboardWatchlist().subscribe({
      next: (data) => {
        this.watchlistAlerts = data;
        this.enrichWatchlistAlerts();
      },
      error: () => {}
    });
  }

  loadLivePrices(): void {
    if (!this.summary) return;

    // Fetch company-wise data and get live prices for top movers
    this.api.getCompanyWiseData().subscribe({
      next: (companies) => {
        const symbols = companies.map(c => c.instrument);
        this.fetchLtpBatch(symbols, 'portfolio');
      }
    });

    // Also fetch watchlist symbols
    this.api.getWatchLists().subscribe({
      next: (watchlists) => {
        const symbols = new Set<string>();
        watchlists.forEach(wl => {
          wl.instruments.forEach(inst => symbols.add(inst.tradingSymbol));
        });
        if (symbols.size > 0) {
          this.fetchLtpBatch(Array.from(symbols), 'watchlist');
        }
      }
    });
  }

  private async fetchLtpBatch(symbols: string[], source: string): Promise<void> {
    const movers: StockMover[] = [];

    // Fetch in batches of 5
    for (let i = 0; i < Math.min(symbols.length, 20); i++) {
      try {
        const result = await this.http.get<any>(
          `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(symbols[i])}`
        ).toPromise();

        if (result && result.last_price) {
          movers.push({
            symbol: symbols[i],
            lastPrice: result.last_price,
            changePercent: result.day_change_perc || 0,
            source: source
          });
        }
      } catch (e) {
        // Skip failed symbols
      }
    }

    // Merge with existing movers
    const allMovers = [...this.topGainers, ...this.topLosers, ...movers];
    const uniqueMovers = new Map<string, StockMover>();
    allMovers.forEach(m => uniqueMovers.set(m.symbol, m));

    const sorted = Array.from(uniqueMovers.values())
      .sort((a, b) => b.changePercent - a.changePercent);

    this.topGainers = sorted.filter(m => m.changePercent > 0).slice(0, 5);
    this.topLosers = sorted.filter(m => m.changePercent < 0)
      .sort((a, b) => a.changePercent - b.changePercent).slice(0, 5);

    if (this.summary) {
      this.summary.topGainers = this.topGainers;
      this.summary.topLosers = this.topLosers;
    }
  }

  private async enrichWatchlistAlerts(): Promise<void> {
    for (const alert of this.watchlistAlerts.slice(0, 10)) {
      try {
        const result = await this.http.get<any>(
          `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(alert.symbol)}`
        ).toPromise();
        if (result) {
          alert.lastPrice = result.last_price || 0;
          alert.priceChange = result.day_change || 0;
          alert.priceChangePercent = result.day_change_perc || 0;
          alert.alertType = alert.priceChangePercent > 2 ? 'opportunity'
            : alert.priceChangePercent < -2 ? 'risk' : 'info';
        }
      } catch (e) { /* skip */ }
    }
  }

  applyNewsFilter(): void {
    if (this.newsFilter === 'all') {
      this.filteredNews = this.news;
    } else {
      this.filteredNews = this.news.filter(n => n.category === this.newsFilter);
    }
  }

  setNewsFilter(filter: 'all' | 'portfolio' | 'watchlist'): void {
    this.newsFilter = filter;
    this.applyNewsFilter();
  }

  refreshDashboard(): void {
    this.api.refreshDashboard().subscribe({
      next: () => this.loadDashboard(),
      error: () => this.loadDashboard()
    });
  }

  getPnlClass(value: number): string {
    return value >= 0 ? 'positive' : 'negative';
  }

  getSourceBadge(source: string): string {
    return source === 'portfolio' ? '📦' : '👀';
  }

  getCategoryLabel(category: string): string {
    switch (category) {
      case 'portfolio': return 'Portfolio';
      case 'watchlist': return 'Watchlist';
      default: return 'Market';
    }
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'portfolio': return 'badge-portfolio';
      case 'watchlist': return 'badge-watchlist';
      default: return 'badge-general';
    }
  }

  getAlertIcon(alertType: string): string {
    switch (alertType) {
      case 'opportunity': return 'trending_up';
      case 'risk': return 'warning';
      default: return 'info';
    }
  }

  getAlertClass(alertType: string): string {
    switch (alertType) {
      case 'opportunity': return 'alert-opportunity';
      case 'risk': return 'alert-risk';
      default: return 'alert-info';
    }
  }
}
