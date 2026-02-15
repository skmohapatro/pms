import { Component, OnInit, ViewChild } from '@angular/core';
import { ApiService, PurchaseDateWise } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { PurchaseDialogComponent } from './purchase-dialog.component';

@Component({
  selector: 'app-purchases',
  templateUrl: './purchases.component.html',
  styleUrls: ['./purchases.component.scss']
})
export class PurchasesComponent implements OnInit {
  displayedColumns: string[] = ['id', 'date', 'company', 'quantity', 'price', 'investment', 'actions'];
  dataSource = new MatTableDataSource<PurchaseDateWise>([]);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private api: ApiService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.api.getPurchases().subscribe({
      next: (data) => {
        this.dataSource.data = data;
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

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(PurchaseDialogComponent, {
      width: '500px',
      data: { mode: 'create', purchase: { date: '', company: '', quantity: 0, price: 0, investment: 0 } }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.api.createPurchase(result).subscribe({
          next: () => {
            this.snackBar.open('Transaction created', 'Close', { duration: 3000 });
            this.loadData();
          },
          error: () => this.snackBar.open('Failed to create', 'Close', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(purchase: PurchaseDateWise): void {
    const dialogRef = this.dialog.open(PurchaseDialogComponent, {
      width: '500px',
      data: { mode: 'edit', purchase: { ...purchase } }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && purchase.id) {
        this.api.updatePurchase(purchase.id, result).subscribe({
          next: () => {
            this.snackBar.open('Transaction updated', 'Close', { duration: 3000 });
            this.loadData();
          },
          error: () => this.snackBar.open('Failed to update', 'Close', { duration: 3000 })
        });
      }
    });
  }

  deletePurchase(id: number): void {
    if (confirm('Are you sure you want to delete this transaction?')) {
      this.api.deletePurchase(id).subscribe({
        next: () => {
          this.snackBar.open('Transaction deleted', 'Close', { duration: 3000 });
          this.loadData();
        },
        error: () => this.snackBar.open('Failed to delete', 'Close', { duration: 3000 })
      });
    }
  }
}
