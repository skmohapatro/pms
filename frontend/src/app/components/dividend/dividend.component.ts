import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService, Dividend } from '../../services/api.service';
import { DividendDialogComponent } from './dividend-dialog.component';

@Component({
  selector: 'app-dividend',
  templateUrl: './dividend.component.html',
  styleUrls: ['./dividend.component.scss']
})
export class DividendComponent implements OnInit {
  displayedColumns = ['symbol', 'isin', 'exDate', 'quantity', 'dividendPerShare', 'netDividendAmount', 'fy', 'actions'];
  dataSource = new MatTableDataSource<Dividend>();
  isLoading = false;
  totalDividend = 0;
  fyList: string[] = [];
  selectedFy = '';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
    this.loadFyList();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadData(): void {
    this.isLoading = true;
    this.apiService.getDividends().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.totalDividend = data.reduce((sum, d) => sum + (d.netDividendAmount || 0), 0);
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load dividends', 'Close', { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  loadFyList(): void {
    this.apiService.getDividendFyList().subscribe({
      next: (list) => this.fyList = list,
      error: () => {}
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  filterByFy(): void {
    this.dataSource.filterPredicate = (data: Dividend, filter: string) => {
      if (!filter) return true;
      return data.fy === filter;
    };
    this.dataSource.filter = this.selectedFy;
  }

  clearFyFilter(): void {
    this.selectedFy = '';
    this.dataSource.filterPredicate = (data: Dividend, filter: string) =>
      JSON.stringify(data).toLowerCase().includes(filter);
    this.dataSource.filter = '';
  }

  openAddDialog(): void {
    const dialogRef = this.dialog.open(DividendDialogComponent, {
      width: '500px',
      data: { mode: 'add', dividend: {} }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.createDividend(result).subscribe({
          next: () => {
            this.snackBar.open('Dividend added successfully', 'Close', { duration: 3000 });
            this.loadData();
            this.loadFyList();
          },
          error: () => this.snackBar.open('Failed to add dividend', 'Close', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(dividend: Dividend): void {
    const dialogRef = this.dialog.open(DividendDialogComponent, {
      width: '500px',
      data: { mode: 'edit', dividend: { ...dividend } }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result && dividend.id) {
        this.apiService.updateDividend(dividend.id, result).subscribe({
          next: () => {
            this.snackBar.open('Dividend updated successfully', 'Close', { duration: 3000 });
            this.loadData();
            this.loadFyList();
          },
          error: () => this.snackBar.open('Failed to update dividend', 'Close', { duration: 3000 })
        });
      }
    });
  }

  deleteDividend(dividend: Dividend): void {
    if (!confirm(`Delete dividend record for ${dividend.symbol}?`)) return;
    this.apiService.deleteDividend(dividend.id!).subscribe({
      next: () => {
        this.snackBar.open('Dividend deleted', 'Close', { duration: 3000 });
        this.loadData();
        this.loadFyList();
      },
      error: () => this.snackBar.open('Failed to delete dividend', 'Close', { duration: 3000 })
    });
  }
}
