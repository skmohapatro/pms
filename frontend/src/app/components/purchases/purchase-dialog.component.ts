import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, startWith, map } from 'rxjs/operators';
import { PurchaseDateWise, Instrument, ApiService } from '../../services/api.service';

@Component({
  selector: 'app-purchase-dialog',
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon class="title-icon">{{ data.mode === 'create' ? 'add_circle' : 'edit' }}</mat-icon>
        {{ data.mode === 'create' ? 'Add Transaction' : 'Edit Transaction' }}
      </h2>
      <mat-dialog-content class="dialog-content">
        <form class="transaction-form">
          <!-- Date Field -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Date</mat-label>
              <input matInput [matDatepicker]="picker" [(ngModel)]="data.purchase.date" name="date" required>
              <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
              <mat-datepicker #picker [startAt]="currentDate"></mat-datepicker>
            </mat-form-field>
          </div>

          <!-- Company Field with Autocomplete -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Company</mat-label>
              <input matInput 
                     [formControl]="companyControl" 
                     [matAutocomplete]="auto"
                     placeholder="Search by symbol or name..."
                     name="company" 
                     required>
              <mat-autocomplete #auto="matAutocomplete" [displayWith]="displayCompany" (optionSelected)="onOptionSelected($event)">
                <mat-option *ngFor="let company of filteredCompanies | async" [value]="company">
                  <div class="company-option">
                    <span class="company-symbol">{{ company.tradingSymbol }}</span>
                    <span class="company-name">{{ company.name }}</span>
                  </div>
                </mat-option>
              </mat-autocomplete>
              <mat-spinner matSuffix *ngIf="isSearching" diameter="18"></mat-spinner>
            </mat-form-field>
          </div>

          <!-- Quantity and Price Row -->
          <div class="form-row-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Quantity</mat-label>
              <input matInput type="number" [(ngModel)]="data.purchase.quantity" name="quantity" 
                     (ngModelChange)="calculateInvestment()" required>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Price</mat-label>
              <input matInput type="number" step="0.01" [(ngModel)]="data.purchase.price" name="price" 
                     (ngModelChange)="calculateInvestment()" required>
            </mat-form-field>
          </div>

          <!-- Investment Field -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field readonly-field">
              <mat-label>Investment (Auto-calculated)</mat-label>
              <input matInput type="number" step="0.01" [(ngModel)]="data.purchase.investment" 
                     name="investment" readonly>
            </mat-form-field>
          </div>
        </form>
      </mat-dialog-content>
      <mat-dialog-actions class="dialog-actions" align="end">
        <button mat-button class="cancel-btn" (click)="onCancel()">
          <mat-icon>close</mat-icon>
          Cancel
        </button>
        <button mat-raised-button color="primary" class="save-btn" (click)="onSave()"
                [disabled]="!data.purchase.date || !data.purchase.company">
          <mat-icon>{{ data.mode === 'create' ? 'add' : 'save' }}</mat-icon>
          {{ data.mode === 'create' ? 'Create' : 'Save' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      min-width: 500px;
      background: #fafafa;
    }

    .dialog-title {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 24px 16px;
      margin: 0;
      color: #1976d2;
      font-weight: 500;
    }

    .title-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .dialog-content {
      padding: 0 24px 20px;
    }

    .transaction-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .form-row-row {
      display: flex;
      gap: 16px;
    }

    .form-row-row .form-field {
      flex: 1;
    }

    .form-field {
      width: 100%;
    }

    .field-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      margin-right: 8px;
      color: #666;
    }

    .readonly-field .mat-mdc-text-field-wrapper {
      background-color: #f5f5f5;
    }

    .company-option {
      display: flex;
      flex-direction: column;
      padding: 4px 0;
    }

    .company-symbol {
      font-weight: 500;
      color: #1976d2;
    }

    .company-name {
      font-size: 12px;
      color: #666;
    }

    .searching-icon {
      display: flex;
      align-items: center;
    }

    .dialog-actions {
      padding: 16px 24px 20px;
      gap: 12px;
    }

    .cancel-btn, .save-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 0 16px;
      height: 40px;
    }

    .save-btn {
      background: linear-gradient(135deg, #1976d2, #1565c0);
    }
  `]
})
export class PurchaseDialogComponent {
  currentDate = new Date();
  companyControl = new FormControl();
  filteredCompanies!: Observable<Instrument[]>;
  isSearching = false;
  searchResults: Instrument[] = [];

  constructor(
    public dialogRef: MatDialogRef<PurchaseDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: string; purchase: PurchaseDateWise },
    private api: ApiService
  ) {
    this.setupCompanySearch();
    this.calculateInvestment();
    
    // Initialize companyControl with existing value if in edit mode
    if (this.data.purchase.company) {
      this.companyControl.setValue(this.data.purchase.company);
    }
  }

  setupCompanySearch(): void {
    this.filteredCompanies = this.companyControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query: string | Instrument | null) => {
        const searchQuery = typeof query === 'string' ? query : '';
        if (!searchQuery || searchQuery.trim().length < 2) {
          return of([]);
        }
        this.isSearching = true;
        return this.api.searchInstruments(searchQuery).pipe(
          catchError(() => of([]))
        );
      }),
      map(results => {
        this.isSearching = false;
        this.searchResults = results;
        return results;
      })
    );
  }

  displayCompany(company: Instrument | string): string {
    if (typeof company === 'string') {
      return company;
    }
    return company ? company.tradingSymbol : '';
  }

  onOptionSelected(event: any): void {
    const selected = event.option.value as Instrument;
    this.data.purchase.company = selected.tradingSymbol;
  }

  calculateInvestment(): void {
    const quantity = this.data.purchase.quantity || 0;
    const price = this.data.purchase.price || 0;
    this.data.purchase.investment = Math.round((quantity * price) * 100) / 100;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.data.purchase);
  }
}
