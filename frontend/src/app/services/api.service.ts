import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PurchaseDateWise {
  id?: number;
  date: string;
  company: string;
  quantity: number;
  price: number;
  investment: number;
}

export interface CompanyWiseAggregated {
  id: number;
  instrument: string;
  qty: number;
  avgCost: number;
  invested: number;
}

export interface InvestmentGroup {
  id: number;
  groupName: string;
  instruments: CompanyWiseAggregated[];
}

export interface GroupDetail {
  groupId: number;
  groupName: string;
  instruments: InstrumentSummary[];
  totalQty: number;
  totalInvested: number;
}

export interface GroupSummary {
  groupId: number;
  groupName: string;
  instrumentCount: number;
  totalQty: number;
  totalInvested: number;
}

export interface InstrumentSummary {
  id: number;
  instrument: string;
  qty: number;
  avgCost: number;
  invested: number;
}

export interface UploadResult {
  totalRows: number;
  successRows: number;
  failedRows: number;
  message: string;
}

export interface MonthlyInvestment {
  year: number;
  month: number;
  monthName: string;
  totalInvestment: number;
}

export interface YearlyInvestment {
  year: number;
  totalInvestment: number;
}

export interface MonthlyStockDetail {
  stockName: string;
  quantityPurchased: number;
  investedAmount: number;
}

export interface Dividend {
  id?: number;
  symbol: string;
  isin?: string;
  exDate?: string;
  quantity?: number;
  dividendPerShare?: number;
  netDividendAmount?: number;
  fy?: string;
}

export interface DividendSummary {
  symbol: string;
  totalDividendReceived: number;
  dividendCount: number;
}

export interface RealizedPnL {
  id?: number;
  symbol: string;
  isin?: string;
  quantity?: number;
  buyValue?: number;
  sellValue?: number;
  realizedPnl?: number;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequest {
  message: string;
  model?: string;
  history?: ChatMessage[];
}

export interface ChatResponse {
  message: string;
  model: string;
  success: boolean;
  error?: string;
}

export interface Instrument {
  id: number;
  exchange: string;
  exchangeToken: string;
  tradingSymbol: string;
  growwSymbol: string;
  name: string;
  instrumentType: string;
  segment: string;
  series: string;
  isin: string;
  lotSize: number;
  tickSize: number;
}

export interface WatchList {
  id: number;
  name: string;
  description: string;
  instruments: Instrument[];
}

export interface InstrumentStatus {
  totalInstruments: number;
  nseCashCount: number;
  lastRefreshTime: string;
  isLoaded: boolean;
}

export interface RefreshResult {
  success: boolean;
  totalInstruments?: number;
  nseCashCount?: number;
  durationMs?: number;
  refreshTime?: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Upload
  uploadExcel(file: File): Observable<UploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadResult>(`${this.baseUrl}/upload`, formData);
  }

  // Purchase Date Wise CRUD
  getPurchases(): Observable<PurchaseDateWise[]> {
    return this.http.get<PurchaseDateWise[]>(`${this.baseUrl}/purchases`);
  }

  createPurchase(purchase: PurchaseDateWise): Observable<PurchaseDateWise> {
    return this.http.post<PurchaseDateWise>(`${this.baseUrl}/purchases`, purchase);
  }

  updatePurchase(id: number, purchase: PurchaseDateWise): Observable<PurchaseDateWise> {
    return this.http.put<PurchaseDateWise>(`${this.baseUrl}/purchases/${id}`, purchase);
  }

  deletePurchase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/purchases/${id}`);
  }

  getCompanies(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/purchases/companies`);
  }

  // Company Wise Aggregated
  getCompanyWiseData(): Observable<CompanyWiseAggregated[]> {
    return this.http.get<CompanyWiseAggregated[]>(`${this.baseUrl}/company-wise`);
  }

  // Groups
  getGroups(): Observable<InvestmentGroup[]> {
    return this.http.get<InvestmentGroup[]>(`${this.baseUrl}/groups`);
  }

  createGroup(groupName: string): Observable<InvestmentGroup> {
    return this.http.post<InvestmentGroup>(`${this.baseUrl}/groups`, { groupName });
  }

  deleteGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/groups/${id}`);
  }

  getGroupDetail(id: number): Observable<GroupDetail> {
    return this.http.get<GroupDetail>(`${this.baseUrl}/groups/${id}/detail`);
  }

  getGroupSummaries(): Observable<GroupSummary[]> {
    return this.http.get<GroupSummary[]>(`${this.baseUrl}/groups/summary`);
  }

  assignInstruments(groupId: number, instrumentIds: number[]): Observable<InvestmentGroup> {
    return this.http.post<InvestmentGroup>(`${this.baseUrl}/groups/${groupId}/instruments`, instrumentIds);
  }

  addInstrumentToGroup(groupId: number, instrumentId: number): Observable<InvestmentGroup> {
    return this.http.post<InvestmentGroup>(`${this.baseUrl}/groups/${groupId}/instruments/${instrumentId}`, {});
  }

  removeInstrumentFromGroup(groupId: number, instrumentId: number): Observable<InvestmentGroup> {
    return this.http.delete<InvestmentGroup>(`${this.baseUrl}/groups/${groupId}/instruments/${instrumentId}`);
  }

  // Analytics
  getMonthlyInvestment(startDate?: string, endDate?: string, company?: string): Observable<MonthlyInvestment[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (company) params = params.set('company', company);
    return this.http.get<MonthlyInvestment[]>(`${this.baseUrl}/analytics/monthly`, { params });
  }

  getYearlyInvestment(startDate?: string, endDate?: string, company?: string): Observable<YearlyInvestment[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (company) params = params.set('company', company);
    return this.http.get<YearlyInvestment[]>(`${this.baseUrl}/analytics/yearly`, { params });
  }

  getMonthlyStockDetails(year: number, month: number, company?: string): Observable<MonthlyStockDetail[]> {
    let params = new HttpParams();
    params = params.set('year', year.toString());
    params = params.set('month', month.toString());
    if (company) params = params.set('company', company);
    return this.http.get<MonthlyStockDetail[]>(`${this.baseUrl}/analytics/monthly/details`, { params });
  }

  // Dividends
  getDividends(): Observable<Dividend[]> {
    return this.http.get<Dividend[]>(`${this.baseUrl}/dividends`);
  }

  createDividend(dividend: Dividend): Observable<Dividend> {
    return this.http.post<Dividend>(`${this.baseUrl}/dividends`, dividend);
  }

  updateDividend(id: number, dividend: Dividend): Observable<Dividend> {
    return this.http.put<Dividend>(`${this.baseUrl}/dividends/${id}`, dividend);
  }

  deleteDividend(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/dividends/${id}`);
  }

  getDividendSymbols(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/dividends/symbols`);
  }

  getDividendFyList(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/dividends/fy`);
  }

  getDividendBySymbol(symbol: string): Observable<DividendSummary> {
    return this.http.get<DividendSummary>(`${this.baseUrl}/dividends/by-symbol/${encodeURIComponent(symbol)}`);
  }

  // Realized P&L
  getRealizedPnL(): Observable<RealizedPnL[]> {
    return this.http.get<RealizedPnL[]>(`${this.baseUrl}/realized-pnl`);
  }

  createRealizedPnL(record: RealizedPnL): Observable<RealizedPnL> {
    return this.http.post<RealizedPnL>(`${this.baseUrl}/realized-pnl`, record);
  }

  updateRealizedPnL(id: number, record: RealizedPnL): Observable<RealizedPnL> {
    return this.http.put<RealizedPnL>(`${this.baseUrl}/realized-pnl/${id}`, record);
  }

  deleteRealizedPnL(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/realized-pnl/${id}`);
  }

  getRealizedPnLSymbols(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/realized-pnl/symbols`);
  }

  // Chat
  sendChatMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  getChatModels(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/chat/models`);
  }

  getChatContext(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/chat/context`);
  }

  // Instruments
  getInstruments(): Observable<Instrument[]> {
    return this.http.get<Instrument[]>(`${this.baseUrl}/instruments`);
  }

  getNseCashInstruments(): Observable<Instrument[]> {
    return this.http.get<Instrument[]>(`${this.baseUrl}/instruments/nse-cash`);
  }

  searchInstruments(query: string): Observable<Instrument[]> {
    return this.http.get<Instrument[]>(`${this.baseUrl}/instruments/search`, {
      params: { q: query }
    });
  }

  getInstrumentById(id: number): Observable<Instrument> {
    return this.http.get<Instrument>(`${this.baseUrl}/instruments/${id}`);
  }

  getInstrumentBySymbol(symbol: string): Observable<Instrument> {
    return this.http.get<Instrument>(`${this.baseUrl}/instruments/symbol/${symbol}`);
  }

  refreshInstruments(): Observable<RefreshResult> {
    return this.http.post<RefreshResult>(`${this.baseUrl}/instruments/refresh`, {});
  }

  getInstrumentStatus(): Observable<InstrumentStatus> {
    return this.http.get<InstrumentStatus>(`${this.baseUrl}/instruments/status`);
  }

  // Watch Lists
  getWatchLists(): Observable<WatchList[]> {
    return this.http.get<WatchList[]>(`${this.baseUrl}/watchlists`);
  }

  getWatchListById(id: number): Observable<WatchList> {
    return this.http.get<WatchList>(`${this.baseUrl}/watchlists/${id}`);
  }

  createWatchList(name: string, description?: string): Observable<WatchList> {
    return this.http.post<WatchList>(`${this.baseUrl}/watchlists`, { name, description });
  }

  updateWatchList(id: number, name: string, description?: string): Observable<WatchList> {
    return this.http.put<WatchList>(`${this.baseUrl}/watchlists/${id}`, { name, description });
  }

  deleteWatchList(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/watchlists/${id}`);
  }

  addInstrumentToWatchList(watchListId: number, instrumentId: number): Observable<WatchList> {
    return this.http.post<WatchList>(`${this.baseUrl}/watchlists/${watchListId}/instruments/${instrumentId}`, {});
  }

  addInstrumentBySymbol(watchListId: number, symbol: string): Observable<WatchList> {
    return this.http.post<WatchList>(`${this.baseUrl}/watchlists/${watchListId}/instruments/symbol/${symbol}`, {});
  }

  removeInstrumentFromWatchList(watchListId: number, instrumentId: number): Observable<WatchList> {
    return this.http.delete<WatchList>(`${this.baseUrl}/watchlists/${watchListId}/instruments/${instrumentId}`);
  }

  addMultipleInstrumentsToWatchList(watchListId: number, instrumentIds: number[]): Observable<WatchList> {
    return this.http.post<WatchList>(`${this.baseUrl}/watchlists/${watchListId}/instruments`, instrumentIds);
  }

  // Dashboard
  getDashboardSummary(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/dashboard/summary`);
  }

  getDashboardNews(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/dashboard/news`);
  }

  getDashboardInsights(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/dashboard/insights`);
  }

  getDashboardWatchlist(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/dashboard/watchlist`);
  }

  refreshDashboard(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/dashboard/refresh`, {});
  }
}
