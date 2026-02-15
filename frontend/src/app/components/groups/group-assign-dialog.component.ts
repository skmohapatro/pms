import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSelectionListChange } from '@angular/material/list';
import { CompanyWiseAggregated, InvestmentGroup } from '../../services/api.service';

@Component({
  selector: 'app-group-assign-dialog',
  template: `
    <h2 mat-dialog-title>Assign Instruments to "{{ data.group.groupName }}"</h2>
    <mat-dialog-content>
      <p>Select instruments to include in this group:</p>
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>Search instruments</mat-label>
        <input matInput [(ngModel)]="searchText" (ngModelChange)="filterInstruments()" placeholder="Type to filter...">
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
      <div class="instrument-list">
        <mat-selection-list (selectionChange)="onSelectionChange($event)">
          <mat-list-option *ngFor="let inst of filteredInstruments"
                           [value]="inst.id"
                           [selected]="selectedIds.has(inst.id)">
            {{ inst.instrument }} (Qty: {{ inst.qty }}, Invested: {{ inst.invested | number:'1.2-2' }})
          </mat-list-option>
        </mat-selection-list>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .search-field {
      width: 100%;
      margin-bottom: 4px;
    }
    .instrument-list {
      max-height: 350px;
      overflow-y: auto;
    }
  `]
})
export class GroupAssignDialogComponent implements OnInit {
  selectedIds: Set<number> = new Set();
  searchText = '';
  filteredInstruments: CompanyWiseAggregated[] = [];

  constructor(
    public dialogRef: MatDialogRef<GroupAssignDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { group: InvestmentGroup; instruments: CompanyWiseAggregated[] }
  ) {}

  ngOnInit(): void {
    if (this.data.group.instruments) {
      this.data.group.instruments.forEach(i => this.selectedIds.add(i.id));
    }
    this.filteredInstruments = [...this.data.instruments];
  }

  filterInstruments(): void {
    const term = this.searchText.toLowerCase().trim();
    if (!term) {
      this.filteredInstruments = [...this.data.instruments];
    } else {
      this.filteredInstruments = this.data.instruments.filter(
        inst => inst.instrument.toLowerCase().includes(term)
      );
    }
  }

  onSelectionChange(event: MatSelectionListChange): void {
    for (const option of event.options) {
      if (option.selected) {
        this.selectedIds.add(option.value);
      } else {
        this.selectedIds.delete(option.value);
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(Array.from(this.selectedIds));
  }
}
