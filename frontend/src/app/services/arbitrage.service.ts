import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ArbitrageOpportunity {
  companyCode: string;
  currentDateTime: string;
  selectedExpiry: string;
  featurePriceL: number;
  featurePriceC: number;
  spotPrice: number;
  lotSize: number;
  holdingDays: number;
  priceDifference: number;
  pctPriceDifference: number;
  futuresInvestment: number;
  spotInvestment: number;
  totalInvestment: number;
  totalProfit: number;
  perDayReturn: number;
  perAnnumReturn: number;
}

export interface ArbitrageRefreshResponse {
  opportunities: ArbitrageOpportunity[];
  count: number;
  minAnnualReturn: number;
  durationMs: number;
  timestamp: number;
  totalCompanies: number;
  successfulCompanies: number;
  failedCompanies: number;
  totalFuturesProcessed: number;
  futuresSkippedNoMatch: number;
  futuresSkippedApiError: number;
}

@Injectable({
  providedIn: 'root'
})
export class ArbitrageService {
  private apiUrl = 'http://localhost:8080/api/arbitrage';

  constructor(private http: HttpClient) {}

  getOpportunities(minAnnualReturn: number = 5.0): Observable<ArbitrageOpportunity[]> {
    return this.http.get<ArbitrageOpportunity[]>(
      `${this.apiUrl}/opportunities?minAnnualReturn=${minAnnualReturn}`
    );
  }

  refreshOpportunities(minAnnualReturn: number = 5.0): Observable<ArbitrageRefreshResponse> {
    return this.http.get<ArbitrageRefreshResponse>(
      `${this.apiUrl}/refresh?minAnnualReturn=${minAnnualReturn}`
    );
  }

  getOpportunitiesBySymbol(symbol: string, minAnnualReturn: number = 5.0): Observable<ArbitrageOpportunity[]> {
    return this.http.get<ArbitrageOpportunity[]>(
      `${this.apiUrl}/opportunities/${symbol}?minAnnualReturn=${minAnnualReturn}`
    );
  }

  checkHealth(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }
}
