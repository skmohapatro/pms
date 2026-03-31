import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ArbitrageService, ArbitrageOpportunity } from '../../services/arbitrage.service';

@Component({
  selector: 'app-arbitrage-opportunity',
  templateUrl: './arbitrage-opportunity.component.html',
  styleUrls: ['./arbitrage-opportunity.component.scss']
})
export class ArbitrageOpportunityComponent implements OnInit {
  displayedColumns: string[] = [
    'companyCode',
    'currentDateTime',
    'selectedExpiry',
    'spotPrice',
    'featurePriceL',
    'lotSize',
    'holdingDays',
    'priceDifference',
    'pctPriceDifference',
    'totalInvestment',
    'totalProfit',
    'perDayReturn',
    'perAnnumReturn'
  ];

  dataSource: MatTableDataSource<ArbitrageOpportunity>;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  loading = false;
  error: string | null = null;
  minAnnualReturn = 5.0;
  symbolFilter = '';
  lastRefreshTime: Date | null = null;
  totalOpportunities = 0;
  calculationDuration = 0;
  totalCompanies = 0;
  successfulCompanies = 0;
  failedCompanies = 0;
  totalFuturesProcessed = 0;

  constructor(
    private arbitrageService: ArbitrageService,
    private snackBar: MatSnackBar
  ) {
    this.dataSource = new MatTableDataSource<ArbitrageOpportunity>([]);
  }

  ngOnInit(): void {
    this.loadOpportunities();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
    
    // Custom filter predicate for symbol search
    this.dataSource.filterPredicate = (data: ArbitrageOpportunity, filter: string) => {
      return data.companyCode.toLowerCase().includes(filter.toLowerCase());
    };
  }

  loadOpportunities(): void {
    this.loading = true;
    this.error = null;

    this.arbitrageService.refreshOpportunities(this.minAnnualReturn).subscribe({
      next: (response) => {
        this.dataSource.data = response.opportunities;
        this.totalOpportunities = response.count;
        this.calculationDuration = response.durationMs;
        this.lastRefreshTime = new Date(response.timestamp);
        this.totalCompanies = response.totalCompanies || 0;
        this.successfulCompanies = response.successfulCompanies || 0;
        this.failedCompanies = response.failedCompanies || 0;
        this.totalFuturesProcessed = response.totalFuturesProcessed || 0;
        this.loading = false;
        
        this.snackBar.open(
          `Found ${response.count} opportunities in ${(response.durationMs / 1000).toFixed(2)}s`,
          'Close',
          { duration: 3000 }
        );
      },
      error: (err) => {
        this.error = 'Failed to load arbitrage opportunities. Please ensure backend is running.';
        this.loading = false;
        this.snackBar.open(this.error, 'Close', { duration: 5000 });
        console.error('Error loading opportunities:', err);
      }
    });
  }

  applySymbolFilter(): void {
    this.dataSource.filter = this.symbolFilter.trim();
  }

  clearSymbolFilter(): void {
    this.symbolFilter = '';
    this.dataSource.filter = '';
  }

  onMinReturnChange(): void {
    if (this.minAnnualReturn >= 0 && this.minAnnualReturn <= 100) {
      this.loadOpportunities();
    }
  }

  refreshData(): void {
    this.loadOpportunities();
  }

  getReturnColorClass(returnValue: number): string {
    if (returnValue >= 15) return 'high-return';
    if (returnValue >= 10) return 'medium-return';
    return 'low-return';
  }

  getProfitColorClass(profit: number): string {
    return profit > 0 ? 'positive' : 'negative';
  }

  exportToCSV(): void {
    const data = this.dataSource.filteredData;
    if (data.length === 0) {
      this.snackBar.open('No data to export', 'Close', { duration: 3000 });
      return;
    }

    const headers = [
      'Company Code', 'Found At', 'Expiry Date', 'Spot Price', 'Futures Price (Last)',
      'Futures Price (Close)', 'Lot Size', 'Holding Days', 'Price Difference',
      '% Price Difference', 'Futures Investment', 'Spot Investment',
      'Total Investment', 'Total Profit', 'Per Day Return', 'Per Annum Return %'
    ];

    const csvRows = [headers.join(',')];

    data.forEach(opp => {
      const row = [
        opp.companyCode,
        opp.currentDateTime,
        opp.selectedExpiry,
        opp.spotPrice,
        opp.featurePriceL,
        opp.featurePriceC,
        opp.lotSize,
        opp.holdingDays,
        opp.priceDifference,
        opp.pctPriceDifference,
        opp.futuresInvestment,
        opp.spotInvestment,
        opp.totalInvestment,
        opp.totalProfit,
        opp.perDayReturn,
        opp.perAnnumReturn
      ];
      csvRows.push(row.join(','));
    });

    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `arbitrage-opportunities-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.snackBar.open('CSV exported successfully', 'Close', { duration: 3000 });
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }

  formatNumber(value: number, decimals: number = 2): string {
    return value.toFixed(decimals);
  }
}
