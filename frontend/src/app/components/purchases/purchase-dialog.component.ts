import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PurchaseDateWise } from '../../services/api.service';

@Component({
  selector: 'app-purchase-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add Transaction' : 'Edit Transaction' }}</h2>
    <mat-dialog-content>
      <form class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Date</mat-label>
          <input matInput type="date" [(ngModel)]="data.purchase.date" name="date" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Company</mat-label>
          <input matInput [(ngModel)]="data.purchase.company" name="company" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Quantity</mat-label>
          <input matInput type="number" [(ngModel)]="data.purchase.quantity" name="quantity" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Price</mat-label>
          <input matInput type="number" step="0.01" [(ngModel)]="data.purchase.price" name="price" required>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Investment</mat-label>
          <input matInput type="number" step="0.01" [(ngModel)]="data.purchase.investment" name="investment" required>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()"
              [disabled]="!data.purchase.date || !data.purchase.company">
        {{ data.mode === 'create' ? 'Create' : 'Save' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form {
      display: flex;
      flex-direction: column;
      min-width: 350px;
    }
    mat-form-field {
      width: 100%;
    }
  `]
})
export class PurchaseDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<PurchaseDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: string; purchase: PurchaseDateWise }
  ) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.data.purchase);
  }
}
