import { Component, OnInit, ViewChild } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { HttpClient } from '@angular/common/http';
import { ApiService, CompanyWiseAggregated } from '../../services/api.service';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface StockDetail {
  ohlc?: { open?: number; high?: number; low?: number; close?: number };
  bid_price?: number;
  offer_price?: number;
  total_buy_quantity?: number;
  total_sell_quantity?: number;
  week_52_high?: number;
  week_52_low?: number;
  upper_circuit_limit?: number;
  lower_circuit_limit?: number;
  volume?: number;
  market_cap?: number;
  dividend_yield?: number;
  totalDividendReceived?: number;
  dividendCount?: number;
}

export interface CompanyWiseWithLive extends CompanyWiseAggregated {
  ltp?: number;
  dayChange?: number;
  dayChangePerc?: number;
  unrealizedPnl?: number;
  currentValue?: number;
  changeInValue?: number;
  loading?: boolean;
  detail?: StockDetail;
  detailLoading?: boolean;
}

@Component({
  selector: 'app-company-wise',
  templateUrl: './company-wise.component.html',
  styleUrls: ['./company-wise.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class CompanyWiseComponent implements OnInit {
  displayedColumns: string[] = ['instrument', 'qty', 'avgCost', 'invested', 'ltp', 'currentValue', 'unrealizedPnl', 'dayChange', 'changeInValue'];
  columnsToDisplayWithExpand = [...this.displayedColumns];
  dataSource = new MatTableDataSource<CompanyWiseWithLive>([]);
  expandedElement: CompanyWiseWithLive | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  totalInvested = 0;
  totalQty = 0;
  totalCurrentValue = 0;
  totalUnrealizedPnl = 0;
  totalDayChange = 0;
  totalChangeInValue = 0;
  loadingPrices = false;
  loadingProgress = 0;

  private readonly CHAT_BACKEND_URL = 'http://localhost:5000';
  private readonly BATCH_SIZE = 10;
  private readonly MAX_RETRIES = 2;
  private readonly RETRY_DELAY_MS = 300;

  constructor(private api: ApiService, private http: HttpClient, private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.api.getCompanyWiseData().subscribe({
      next: (data) => {
        const enrichedData: CompanyWiseWithLive[] = data.map(d => ({ ...d, loading: true }));
        this.dataSource.data = enrichedData;
        this.totalInvested = data.reduce((sum, d) => sum + d.invested, 0);
        this.totalQty = data.reduce((sum, d) => sum + d.qty, 0);
        setTimeout(() => {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
        });
        this.fetchLivePrices(enrichedData);
      },
      error: () => this.snackBar.open('Failed to load data', 'Close', { duration: 3000 })
    });
  }

  async fetchLivePrices(data: CompanyWiseWithLive[]): Promise<void> {
    this.loadingPrices = true;
    this.loadingProgress = 0;
    const total = data.length;
    let completed = 0;
    let failCount = 0;

    // Process in parallel batches
    for (let i = 0; i < data.length; i += this.BATCH_SIZE) {
      const batch = data.slice(i, i + this.BATCH_SIZE);
      const batchPromises = batch.map((item, idx) => 
        this.fetchWithRetry(item.instrument).then(result => ({ index: i + idx, result }))
      );

      const results = await Promise.all(batchPromises);

      for (const { index, result } of results) {
        const item = data[index];
        if (result && result.last_price != null) {
          const ltp = result.last_price;
          const currentValue = ltp * item.qty;
          const unrealizedPnl = currentValue - item.invested;
          const changeInValue = (result.day_change || 0) * item.qty;
          data[index] = {
            ...item,
            ltp,
            dayChange: result.day_change,
            dayChangePerc: result.day_change_perc,
            currentValue,
            unrealizedPnl,
            changeInValue,
            loading: false
          };
        } else {
          data[index] = { ...item, loading: false };
          failCount++;
        }
        completed++;
      }

      this.loadingProgress = Math.round((completed / total) * 100);
      this.dataSource.data = [...data];
      this.calculateTotals(data);
    }

    this.loadingPrices = false;
    if (failCount > 0) {
      this.snackBar.open(`Fetched ${total - failCount}/${total} prices (${failCount} failed)`, 'Close', { duration: 4000 });
    }
  }

  private async fetchWithRetry(instrument: string, attempt = 0): Promise<any> {
    try {
      const response = await this.http.get<any>(
        `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(instrument)}`
      ).toPromise();
      return response;
    } catch (error) {
      if (attempt < this.MAX_RETRIES) {
        await this.delay(this.RETRY_DELAY_MS * (attempt + 1));
        return this.fetchWithRetry(instrument, attempt + 1);
      }
      return null;
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  calculateTotals(data: CompanyWiseWithLive[]): void {
    this.totalCurrentValue = data.reduce((sum, d) => sum + (d.currentValue || 0), 0);
    this.totalUnrealizedPnl = data.reduce((sum, d) => sum + (d.unrealizedPnl || 0), 0);
    this.totalDayChange = data.reduce((sum, d) => sum + ((d.dayChange || 0) * d.qty), 0);
    this.totalChangeInValue = data.reduce((sum, d) => sum + (d.changeInValue || 0), 0);
  }

  refreshPrices(): void {
    this.fetchLivePrices(this.dataSource.data);
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  getScreenerUrl(instrument: string): string {
    const symbol = (instrument || '').trim();
    return `https://www.screener.in/company/${encodeURIComponent(symbol)}`;
  }

  getPnlClass(value: number | undefined): string {
    if (value == null) return '';
    return value >= 0 ? 'positive' : 'negative';
  }

  toggleRow(row: CompanyWiseWithLive): void {
    if (this.expandedElement === row) {
      this.expandedElement = null;
    } else {
      this.expandedElement = row;
      if (!row.detail && !row.detailLoading) {
        this.fetchStockDetail(row);
      }
    }
  }

  async fetchStockDetail(row: CompanyWiseWithLive): Promise<void> {
    row.detailLoading = true;
    try {
      const [stockResult, dividendResult] = await Promise.all([
        this.http.get<any>(
          `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(row.instrument)}`
        ).toPromise(),
        this.api.getDividendBySymbol(row.instrument).toPromise()
      ]);
      
      row.detail = {
        ohlc: stockResult?.ohlc,
        bid_price: stockResult?.bid_price,
        offer_price: stockResult?.offer_price,
        total_buy_quantity: stockResult?.total_buy_quantity,
        total_sell_quantity: stockResult?.total_sell_quantity,
        week_52_high: stockResult?.week_52_high,
        week_52_low: stockResult?.week_52_low,
        upper_circuit_limit: stockResult?.upper_circuit_limit,
        lower_circuit_limit: stockResult?.lower_circuit_limit,
        volume: stockResult?.volume,
        market_cap: stockResult?.market_cap,
        dividend_yield: stockResult?.dividend_yield,
        totalDividendReceived: dividendResult?.totalDividendReceived,
        dividendCount: dividendResult?.dividendCount
      };
    } catch (e) {
      this.snackBar.open('Failed to load stock details', 'Close', { duration: 2000 });
    }
    row.detailLoading = false;
  }

  getNseUrl(instrument: string): string {
    const symbol = (instrument || '').trim();
    return `https://www.nseindia.com/get-quotes/equity?symbol=${encodeURIComponent(symbol)}`;
  }

  getTickertapeUrl(instrument: string): string {
    const symbol = (instrument || '').trim();
    return `https://www.tickertape.in/stocks/${encodeURIComponent(symbol)}`;
  }

  refreshDetail(row: CompanyWiseWithLive, event: Event): void {
    event.stopPropagation();
    row.detail = undefined;
    this.fetchStockDetail(row);
  }
}
