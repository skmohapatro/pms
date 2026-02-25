import { Component, OnInit } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { HttpClient } from '@angular/common/http';
import { ApiService, InvestmentGroup, CompanyWiseAggregated, GroupDetail, InstrumentSummary } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { GroupAssignDialogComponent } from './group-assign-dialog.component';

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

export interface InstrumentWithLive extends InstrumentSummary {
  ltp?: number;
  dayChange?: number;
  dayChangePerc?: number;
  loading?: boolean;
  detail?: StockDetail;
  detailLoading?: boolean;
}

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0' })),
      state('expanded', style({ height: '*' })),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class GroupsComponent implements OnInit {
  groups: InvestmentGroup[] = [];
  instruments: CompanyWiseAggregated[] = [];
  selectedGroupDetail: GroupDetail | null = null;
  newGroupName = '';
  expandedElement: InstrumentWithLive | null = null;
  enrichedInstruments: InstrumentWithLive[] = [];

  private readonly CHAT_BACKEND_URL = 'http://localhost:5000';

  constructor(
    private api: ApiService,
    private http: HttpClient,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.loadInstruments();
  }

  loadGroups(): void {
    this.api.getGroups().subscribe({
      next: (data) => this.groups = data,
      error: () => this.snackBar.open('Failed to load groups', 'Close', { duration: 3000 })
    });
  }

  loadInstruments(): void {
    this.api.getCompanyWiseData().subscribe({
      next: (data) => this.instruments = data
    });
  }

  createGroup(): void {
    if (!this.newGroupName.trim()) return;
    this.api.createGroup(this.newGroupName.trim()).subscribe({
      next: () => {
        this.snackBar.open('Group created', 'Close', { duration: 3000 });
        this.newGroupName = '';
        this.loadGroups();
      },
      error: () => this.snackBar.open('Failed to create group (may already exist)', 'Close', { duration: 3000 })
    });
  }

  deleteGroup(id: number): void {
    if (confirm('Delete this group?')) {
      this.api.deleteGroup(id).subscribe({
        next: () => {
          this.snackBar.open('Group deleted', 'Close', { duration: 3000 });
          this.loadGroups();
          if (this.selectedGroupDetail?.groupId === id) {
            this.selectedGroupDetail = null;
          }
        },
        error: () => this.snackBar.open('Failed to delete group', 'Close', { duration: 3000 })
      });
    }
  }

  selectGroup(group: InvestmentGroup): void {
    this.api.getGroupDetail(group.id).subscribe({
      next: (detail) => {
        this.selectedGroupDetail = detail;
        this.enrichedInstruments = detail.instruments.map(i => ({ ...i, loading: false }));
        this.expandedElement = null;
      },
      error: () => this.snackBar.open('Failed to load group detail', 'Close', { duration: 3000 })
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
        `${this.CHAT_BACKEND_URL}/api/stock/search?q=${encodeURIComponent(row.instrument)}`
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

  getScreenerUrl(instrument: string): string {
    return `https://www.screener.in/company/${encodeURIComponent(instrument.trim())}`;
  }

  getNseUrl(instrument: string): string {
    return `https://www.nseindia.com/get-quotes/equity?symbol=${encodeURIComponent(instrument.trim())}`;
  }

  getTickertapeUrl(instrument: string): string {
    return `https://www.tickertape.in/stocks/${encodeURIComponent(instrument.trim())}`;
  }

  openAssignDialog(group: InvestmentGroup): void {
    const dialogRef = this.dialog.open(GroupAssignDialogComponent, {
      width: '600px',
      data: { group, instruments: this.instruments }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.api.assignInstruments(group.id, result).subscribe({
          next: () => {
            this.snackBar.open('Instruments assigned', 'Close', { duration: 3000 });
            this.loadGroups();
            this.selectGroup(group);
          },
          error: () => this.snackBar.open('Failed to assign instruments', 'Close', { duration: 3000 })
        });
      }
    });
  }

  removeInstrument(instrumentId: number): void {
    if (!this.selectedGroupDetail) return;
    this.api.removeInstrumentFromGroup(this.selectedGroupDetail.groupId, instrumentId).subscribe({
      next: () => {
        this.snackBar.open('Instrument removed', 'Close', { duration: 3000 });
        this.selectGroup({ id: this.selectedGroupDetail!.groupId, groupName: this.selectedGroupDetail!.groupName, instruments: [] });
      },
      error: () => this.snackBar.open('Failed to remove instrument', 'Close', { duration: 3000 })
    });
  }
}
