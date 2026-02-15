import { Component, OnInit, ViewChild } from '@angular/core';
import { ApiService, CompanyWiseAggregated } from '../../services/api.service';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-company-wise',
  templateUrl: './company-wise.component.html',
  styleUrls: ['./company-wise.component.scss']
})
export class CompanyWiseComponent implements OnInit {
  displayedColumns: string[] = ['instrument', 'qty', 'avgCost', 'invested'];
  dataSource = new MatTableDataSource<CompanyWiseAggregated>([]);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  totalInvested = 0;
  totalQty = 0;

  constructor(private api: ApiService, private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.api.getCompanyWiseData().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.totalInvested = data.reduce((sum, d) => sum + d.invested, 0);
        this.totalQty = data.reduce((sum, d) => sum + d.qty, 0);
        setTimeout(() => {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
        });
      },
      error: () => this.snackBar.open('Failed to load data', 'Close', { duration: 3000 })
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  getScreenerUrl(instrument: string): string {
    const symbol = (instrument || '').trim();
    return `https://www.screener.in/company/${encodeURIComponent(symbol)}/consolidated/`;
  }
}
