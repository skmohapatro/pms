import { Component, OnInit, ViewChild } from '@angular/core';
import { ApiService, GroupSummary, InstrumentSummary, MonthlyInvestment, YearlyInvestment, MonthlyStockDetail } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { trigger, state, style, transition, animate } from '@angular/animations';

interface MonthlyInvestmentWithCumulative extends MonthlyInvestment {
  cumulativeInvestment: number;
  stockDetails?: MonthlyStockDetail[];
  expanded?: boolean;
}

interface GroupSummaryWithDetail extends GroupSummary {
  instruments?: InstrumentSummary[];
  expanded?: boolean;
}

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({ height: '0px', minHeight: '0', overflow: 'hidden' })),
      state('expanded', style({ height: '*' })),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)'))
    ])
  ]
})
export class AnalyticsComponent implements OnInit {
  @ViewChild('monthlyChart') monthlyChart?: BaseChartDirective;
  @ViewChild('yearlyChart') yearlyChart?: BaseChartDirective;
  @ViewChild('groupChart') groupChart?: BaseChartDirective;

  companies: string[] = [];
  selectedCompany = '';
  startDate = '';
  endDate = '';

  monthlyData: MonthlyInvestmentWithCumulative[] = [];
  yearlyData: YearlyInvestment[] = [];
  expandedRow: MonthlyInvestmentWithCumulative | null = null;

  groupSummaries: GroupSummaryWithDetail[] = [];
  expandedGroupRow: GroupSummaryWithDetail | null = null;

  // Monthly Chart
  monthlyChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  monthlyChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: true },
      title: { display: true, text: 'Monthly Investment' },
      tooltip: {
        callbacks: {
          afterLabel: (context) => {
            const index = context.dataIndex;
            if (this.monthlyData[index]) {
              return `Cumulative: ${this.monthlyData[index].cumulativeInvestment.toFixed(2)}`;
            }
            return '';
          }
        }
      }
    },
    scales: {
      y: { beginAtZero: true }
    }
  };

  // Group Comparison Chart
  groupChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  groupChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: true },
      title: { display: true, text: 'Group Comparison (Total Invested)' }
    },
    scales: {
      y: { beginAtZero: true }
    }
  };

  // Yearly Chart
  yearlyChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  yearlyChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: true },
      title: { display: true, text: 'Yearly Investment' }
    },
    scales: {
      y: { beginAtZero: true }
    }
  };

  monthlyDisplayedColumns = ['monthName', 'totalInvestment', 'cumulativeInvestment'];
  yearlyDisplayedColumns = ['year', 'totalInvestment'];
  groupDisplayedColumns = ['groupName', 'instrumentCount', 'totalQty', 'totalInvested'];

  constructor(private api: ApiService, private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.loadCompanies();
    this.loadData();
  }

  loadCompanies(): void {
    this.api.getCompanies().subscribe({
      next: (data) => this.companies = data
    });
  }

  loadData(): void {
    const start = this.startDate || undefined;
    const end = this.endDate || undefined;
    const company = this.selectedCompany || undefined;

    this.api.getMonthlyInvestment(start, end, company).subscribe({
      next: (data) => {
        this.monthlyData = this.calculateCumulativeInvestment(data);
        this.updateMonthlyChart(this.monthlyData);
      },
      error: () => this.snackBar.open('Failed to load monthly data', 'Close', { duration: 3000 })
    });

    this.api.getYearlyInvestment(start, end, company).subscribe({
      next: (data) => {
        this.yearlyData = data;
        this.updateYearlyChart(data);
      },
      error: () => this.snackBar.open('Failed to load yearly data', 'Close', { duration: 3000 })
    });

    this.api.getGroupSummaries().subscribe({
      next: (data) => {
        this.groupSummaries = data.map(g => ({ ...g, expanded: false }));
        this.expandedGroupRow = null;
        this.updateGroupChart(data);
      },
      error: () => this.snackBar.open('Failed to load group data', 'Close', { duration: 3000 })
    });
  }

  applyFilters(): void {
    this.loadData();
  }

  clearFilters(): void {
    this.selectedCompany = '';
    this.startDate = '';
    this.endDate = '';
    this.loadData();
  }

  private updateMonthlyChart(data: MonthlyInvestment[]): void {
    this.monthlyChartData = {
      labels: data.map(d => d.monthName),
      datasets: [{
        data: data.map(d => d.totalInvestment),
        label: 'Investment',
        backgroundColor: '#3f51b5',
        borderColor: '#303f9f',
        borderWidth: 1
      }]
    };
    this.monthlyChart?.update();
  }

  private updateYearlyChart(data: YearlyInvestment[]): void {
    this.yearlyChartData = {
      labels: data.map(d => String(d.year)),
      datasets: [{
        data: data.map(d => d.totalInvestment),
        label: 'Investment',
        backgroundColor: '#ff4081',
        borderColor: '#c51162',
        borderWidth: 1
      }]
    };
    this.yearlyChart?.update();
  }

  private updateGroupChart(data: GroupSummary[]): void {
    this.groupChartData = {
      labels: data.map(d => d.groupName),
      datasets: [{
        data: data.map(d => d.totalInvested),
        label: 'Total Invested',
        backgroundColor: '#009688',
        borderColor: '#00796b',
        borderWidth: 1
      }]
    };
    this.groupChart?.update();
  }

  private calculateCumulativeInvestment(data: MonthlyInvestment[]): MonthlyInvestmentWithCumulative[] {
    let cumulative = 0;
    return data.map(item => {
      cumulative += item.totalInvestment;
      return {
        ...item,
        cumulativeInvestment: cumulative,
        expanded: false
      };
    });
  }

  toggleRow(row: MonthlyInvestmentWithCumulative): void {
    if (this.expandedRow === row) {
      this.expandedRow = null;
      row.expanded = false;
    } else {
      if (this.expandedRow) {
        this.expandedRow.expanded = false;
      }
      this.expandedRow = row;
      row.expanded = true;
      
      if (!row.stockDetails) {
        const company = this.selectedCompany || undefined;
        this.api.getMonthlyStockDetails(row.year, row.month, company).subscribe({
          next: (details) => {
            row.stockDetails = details;
          },
          error: () => {
            row.stockDetails = [];
            this.snackBar.open('Failed to load stock details', 'Close', { duration: 3000 });
          }
        });
      }
    }
  }

  isExpanded(row: MonthlyInvestmentWithCumulative): boolean {
    return this.expandedRow === row;
  }

  toggleGroupRow(row: GroupSummaryWithDetail): void {
    if (this.expandedGroupRow === row) {
      this.expandedGroupRow = null;
      row.expanded = false;
    } else {
      if (this.expandedGroupRow) {
        this.expandedGroupRow.expanded = false;
      }
      this.expandedGroupRow = row;
      row.expanded = true;

      if (!row.instruments) {
        this.api.getGroupDetail(row.groupId).subscribe({
          next: (detail) => {
            row.instruments = detail.instruments;
          },
          error: () => {
            row.instruments = [];
            this.snackBar.open('Failed to load group instruments', 'Close', { duration: 3000 });
          }
        });
      }
    }
  }

  isGroupExpanded(row: GroupSummaryWithDetail): boolean {
    return this.expandedGroupRow === row;
  }
}
