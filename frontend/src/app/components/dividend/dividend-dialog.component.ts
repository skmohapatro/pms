import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Dividend } from '../../services/api.service';

@Component({
  selector: 'app-dividend-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'add' ? 'Add Dividend' : 'Edit Dividend' }}</h2>
    <mat-dialog-content>
      <div class="form-grid">
        <mat-form-field appearance="outline">
          <mat-label>Symbol *</mat-label>
          <input matInput [(ngModel)]="data.dividend.symbol" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>ISIN</mat-label>
          <input matInput [(ngModel)]="data.dividend.isin">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Ex-Date</mat-label>
          <input matInput [(ngModel)]="data.dividend.exDate" placeholder="YYYY-MM-DD">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Quantity</mat-label>
          <input matInput type="number" [(ngModel)]="data.dividend.quantity">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Dividend Per Share (₹)</mat-label>
          <input matInput type="number" [(ngModel)]="data.dividend.dividendPerShare" (ngModelChange)="calcNet()">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Net Dividend Amount (₹)</mat-label>
          <input matInput type="number" [(ngModel)]="data.dividend.netDividendAmount">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Financial Year (e.g. 2024-2025)</mat-label>
          <input matInput [(ngModel)]="data.dividend.fy">
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="!data.dividend.symbol">
        {{ data.mode === 'add' ? 'Add' : 'Save' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      padding: 8px 0;
      min-width: 420px;
    }
    .full-width { grid-column: 1 / -1; }
    mat-form-field { width: 100%; }
    ::ng-deep .mat-mdc-form-field-subscript-wrapper { display: none; }
  `]
})
export class DividendDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<DividendDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'add' | 'edit'; dividend: Dividend }
  ) {}

  calcNet(): void {
    if (this.data.dividend.quantity && this.data.dividend.dividendPerShare) {
      this.data.dividend.netDividendAmount =
        this.data.dividend.quantity * this.data.dividend.dividendPerShare;
    }
  }

  save(): void {
    if (!this.data.dividend.symbol?.trim()) return;
    this.dialogRef.close(this.data.dividend);
  }
}
