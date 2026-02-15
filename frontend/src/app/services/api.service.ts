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
}
