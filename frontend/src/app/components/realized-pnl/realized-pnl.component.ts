import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService, RealizedPnL } from '../../services/api.service';
import { RealizedPnLDialogComponent } from './realized-pnl-dialog.component';

@Component({
  selector: 'app-realized-pnl',
  templateUrl: './realized-pnl.component.html',
  styleUrls: ['./realized-pnl.component.scss']
})
export class RealizedPnLComponent implements OnInit, AfterViewInit {
  displayedColumns = ['symbol', 'isin', 'quantity', 'buyValue', 'sellValue', 'realizedPnl', 'actions'];
  dataSource = new MatTableDataSource<RealizedPnL>();
  isLoading = false;

  totalBuyValue = 0;
  totalSellValue = 0;
  totalRealizedPnl = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadData(): void {
    this.isLoading = true;
    this.apiService.getRealizedPnL().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.totalBuyValue = data.reduce((s, r) => s + (r.buyValue || 0), 0);
        this.totalSellValue = data.reduce((s, r) => s + (r.sellValue || 0), 0);
        this.totalRealizedPnl = data.reduce((s, r) => s + (r.realizedPnl || 0), 0);
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load Realized P&L data', 'Close', { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  openAddDialog(): void {
    const dialogRef = this.dialog.open(RealizedPnLDialogComponent, {
      width: '500px',
      data: { mode: 'add', record: {} }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.apiService.createRealizedPnL(result).subscribe({
          next: () => {
            this.snackBar.open('Record added successfully', 'Close', { duration: 3000 });
            this.loadData();
          },
          error: () => this.snackBar.open('Failed to add record', 'Close', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(record: RealizedPnL): void {
    const dialogRef = this.dialog.open(RealizedPnLDialogComponent, {
      width: '500px',
      data: { mode: 'edit', record: { ...record } }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result && record.id) {
        this.apiService.updateRealizedPnL(record.id, result).subscribe({
          next: () => {
            this.snackBar.open('Record updated successfully', 'Close', { duration: 3000 });
            this.loadData();
          },
          error: () => this.snackBar.open('Failed to update record', 'Close', { duration: 3000 })
        });
      }
    });
  }

  deleteRecord(record: RealizedPnL): void {
    if (!confirm(`Delete Realized P&L record for ${record.symbol}?`)) return;
    this.apiService.deleteRealizedPnL(record.id!).subscribe({
      next: () => {
        this.snackBar.open('Record deleted', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: () => this.snackBar.open('Failed to delete record', 'Close', { duration: 3000 })
    });
  }
}
