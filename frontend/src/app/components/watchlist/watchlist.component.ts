import { Component, OnInit } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { HttpClient } from '@angular/common/http';
import { ApiService, WatchList, Instrument, InstrumentStatus } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormControl } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';

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
}

export interface InstrumentWithLive extends Instrument {
  ltp?: number;
  dayChange?: number;
  dayChangePerc?: number;
  loading?: boolean;
  detail?: StockDetail;
  detailLoading?: boolean;
}

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class WatchlistComponent implements OnInit {
  watchLists: WatchList[] = [];
  selectedWatchList: WatchList | null = null;
  newWatchListName = '';
  newWatchListDescription = '';

  searchControl = new FormControl('');
  searchResults: Instrument[] = [];
  isSearching = false;
  instrumentStatus: InstrumentStatus | null = null;
  isRefreshing = false;

  displayedColumns = ['tradingSymbol', 'name', 'segment', 'actions'];
  expandedElement: InstrumentWithLive | null = null;
  enrichedInstruments: InstrumentWithLive[] = [];

  private readonly CHAT_BACKEND_URL = 'http://localhost:5000';

  constructor(
    private api: ApiService,
    private http: HttpClient,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadWatchLists();
    this.loadInstrumentStatus();
    this.setupSearch();
  }

  loadWatchLists(): void {
    this.api.getWatchLists().subscribe({
      next: (data) => this.watchLists = data,
      error: () => this.snackBar.open('Failed to load watch lists', 'Close', { duration: 3000 })
    });
  }

  loadInstrumentStatus(): void {
    this.api.getInstrumentStatus().subscribe({
      next: (status) => this.instrumentStatus = status,
      error: () => console.error('Failed to load instrument status')
    });
  }

  setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query: string | null) => {
        if (!query || query.trim().length < 2) {
          return of([]);
        }
        this.isSearching = true;
        return this.api.searchInstruments(query).pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe(results => {
      this.searchResults = results;
      this.isSearching = false;
    });
  }

  createWatchList(): void {
    if (!this.newWatchListName.trim()) return;
    this.api.createWatchList(this.newWatchListName.trim(), this.newWatchListDescription.trim()).subscribe({
      next: () => {
        this.snackBar.open('Watch list created', 'Close', { duration: 3000 });
        this.newWatchListName = '';
        this.newWatchListDescription = '';
        this.loadWatchLists();
      },
      error: (err) => this.snackBar.open(err.error?.error || 'Failed to create watch list', 'Close', { duration: 3000 })
    });
  }

  deleteWatchList(id: number): void {
    if (confirm('Delete this watch list?')) {
      this.api.deleteWatchList(id).subscribe({
        next: () => {
          this.snackBar.open('Watch list deleted', 'Close', { duration: 3000 });
          this.loadWatchLists();
          if (this.selectedWatchList?.id === id) {
            this.selectedWatchList = null;
          }
        },
        error: () => this.snackBar.open('Failed to delete watch list', 'Close', { duration: 3000 })
      });
    }
  }

  selectWatchList(watchList: WatchList): void {
    this.api.getWatchListById(watchList.id).subscribe({
      next: (detail) => {
        this.selectedWatchList = detail;
        this.enrichedInstruments = detail.instruments.map(i => ({ ...i, loading: false }));
        this.expandedElement = null;
      },
      error: () => this.snackBar.open('Failed to load watch list detail', 'Close', { duration: 3000 })
    });
  }

  toggleRow(row: InstrumentWithLive): void {
    if (this.expandedElement === row) {
      this.expandedElement = null;
    } else {
      this.expandedElement = row;
      if (!row.detail && !row.detailLoading) {
        this.fetchStockDetail(row);
      }
    }
  }

  async fetchStockDetail(row: InstrumentWithLive): Promise<void> {
    row.detailLoading = true;
    try {
      const result = await this.http.get<any>(
        `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(row.tradingSymbol)}`
      ).toPromise();
      if (result) {
        row.ltp = result.last_price;
        row.dayChange = result.day_change;
        row.dayChangePerc = result.day_change_perc;
        row.detail = {
          ohlc: result.ohlc,
          bid_price: result.bid_price,
          offer_price: result.offer_price,
          total_buy_quantity: result.total_buy_quantity,
          total_sell_quantity: result.total_sell_quantity,
          week_52_high: result.week_52_high,
          week_52_low: result.week_52_low,
          upper_circuit_limit: result.upper_circuit_limit,
          lower_circuit_limit: result.lower_circuit_limit,
          volume: result.volume,
          market_cap: result.market_cap
        };
      }
    } catch (e) {
      this.snackBar.open('Failed to load stock details', 'Close', { duration: 2000 });
    }
    row.detailLoading = false;
  }

  refreshDetail(row: InstrumentWithLive, event: Event): void {
    event.stopPropagation();
    row.detail = undefined;
    this.fetchStockDetail(row);
  }

  getPnlClass(value: number | undefined): string {
    if (value == null) return '';
    return value >= 0 ? 'positive' : 'negative';
  }

  getScreenerUrl(symbol: string): string {
    return `https://www.screener.in/company/${encodeURIComponent(symbol.trim())}`;
  }

  getNseUrl(symbol: string): string {
    return `https://www.nseindia.com/get-quotes/equity?symbol=${encodeURIComponent(symbol.trim())}`;
  }

  getTickertapeUrl(symbol: string): string {
    return `https://www.tickertape.in/stocks/${encodeURIComponent(symbol.trim())}`;
  }

  addInstrumentToWatchList(instrument: Instrument): void {
    if (!this.selectedWatchList) {
      this.snackBar.open('Please select a watch list first', 'Close', { duration: 3000 });
      return;
    }

    const alreadyExists = this.selectedWatchList.instruments.some(i => i.id === instrument.id);
    if (alreadyExists) {
      this.snackBar.open('Instrument already in watch list', 'Close', { duration: 3000 });
      return;
    }

    this.api.addInstrumentToWatchList(this.selectedWatchList.id, instrument.id).subscribe({
      next: (updatedWatchList) => {
        this.selectedWatchList = updatedWatchList;
        this.snackBar.open(`${instrument.tradingSymbol} added to watch list`, 'Close', { duration: 3000 });
        this.searchControl.setValue('');
        this.searchResults = [];
      },
      error: (err) => this.snackBar.open(err.error?.error || 'Failed to add instrument', 'Close', { duration: 3000 })
    });
  }

  removeInstrument(instrumentId: number): void {
    if (!this.selectedWatchList) return;
    this.api.removeInstrumentFromWatchList(this.selectedWatchList.id, instrumentId).subscribe({
      next: (updatedWatchList) => {
        this.selectedWatchList = updatedWatchList;
        this.snackBar.open('Instrument removed', 'Close', { duration: 3000 });
      },
      error: () => this.snackBar.open('Failed to remove instrument', 'Close', { duration: 3000 })
    });
  }

  refreshInstruments(): void {
    this.isRefreshing = true;
    this.snackBar.open('Refreshing instruments from Groww... This may take a moment.', 'Close', { duration: 5000 });

    this.api.refreshInstruments().subscribe({
      next: (result) => {
        this.isRefreshing = false;
        if (result.success) {
          this.snackBar.open(`Loaded ${result.totalInstruments} instruments (${result.nseCashCount} NSE Cash)`, 'Close', { duration: 5000 });
          this.loadInstrumentStatus();
        } else {
          this.snackBar.open(`Failed: ${result.error}`, 'Close', { duration: 5000 });
        }
      },
      error: (err) => {
        this.isRefreshing = false;
        this.snackBar.open(err.error?.error || 'Failed to refresh instruments', 'Close', { duration: 5000 });
      }
    });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.searchResults = [];
  }
}
