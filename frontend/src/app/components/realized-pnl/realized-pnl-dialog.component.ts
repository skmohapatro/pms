import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RealizedPnL } from '../../services/api.service';

@Component({
  selector: 'app-realized-pnl-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'add' ? 'Add Realized P&L Record' : 'Edit Realized P&L Record' }}</h2>
    <mat-dialog-content>
      <div class="form-grid">
        <mat-form-field appearance="outline">
          <mat-label>Symbol *</mat-label>
          <input matInput [(ngModel)]="data.record.symbol" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>ISIN</mat-label>
          <input matInput [(ngModel)]="data.record.isin">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Quantity</mat-label>
          <input matInput type="number" [(ngModel)]="data.record.quantity">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Buy Value (₹)</mat-label>
          <input matInput type="number" [(ngModel)]="data.record.buyValue" (ngModelChange)="calcPnl()">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Sell Value (₹)</mat-label>
          <input matInput type="number" [(ngModel)]="data.record.sellValue" (ngModelChange)="calcPnl()">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Realized P&L (₹)</mat-label>
          <input matInput type="number" [(ngModel)]="data.record.realizedPnl">
          <mat-hint>Auto-calculated from Sell - Buy</mat-hint>
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="!data.record.symbol">
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
    mat-form-field { width: 100%; }
  `]
})
export class RealizedPnLDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<RealizedPnLDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'add' | 'edit'; record: RealizedPnL }
  ) {}

  calcPnl(): void {
    if (this.data.record.buyValue != null && this.data.record.sellValue != null) {
      this.data.record.realizedPnl = this.data.record.sellValue - this.data.record.buyValue;
    }
  }

  save(): void {
    if (!this.data.record.symbol?.trim()) return;
    this.dialogRef.close(this.data.record);
  }
}
