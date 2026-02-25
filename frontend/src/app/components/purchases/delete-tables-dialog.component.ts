import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';

export interface TableInfo {
  key: string;
  displayName: string;
  recordCount: number;
  selected: boolean;
}

@Component({
  selector: 'app-delete-tables-dialog',
  template: `
    <h2 mat-dialog-title>Delete Table Data</h2>
    <mat-dialog-content>
      <p class="warning-text">
        <mat-icon color="warn">warning</mat-icon>
        Select the tables you want to clear. This action cannot be undone!
      </p>
      
      <div class="loading" *ngIf="loading">
        <mat-spinner diameter="30"></mat-spinner>
        <span>Loading table info...</span>
      </div>

      <div class="table-list" *ngIf="!loading">
        <div class="select-all">
          <mat-checkbox 
            [checked]="allSelected" 
            [indeterminate]="someSelected && !allSelected"
            (change)="toggleAll($event.checked)">
            Select All
          </mat-checkbox>
        </div>
        
        <mat-divider></mat-divider>
        
        <div class="table-item" *ngFor="let table of tables">
          <mat-checkbox [(ngModel)]="table.selected">
            {{ table.displayName }}
            <span class="record-count">({{ table.recordCount }} records)</span>
          </mat-checkbox>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="warn" 
              [disabled]="!hasSelection || deleting"
              (click)="confirmDelete()">
        <mat-icon>{{ deleting ? 'hourglass_empty' : 'delete_forever' }}</mat-icon>
        {{ deleting ? 'Deleting...' : 'Delete Selected' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .warning-text {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #c62828;
      background: #ffebee;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
    }
    .loading {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px;
      justify-content: center;
    }
    .table-list {
      min-width: 350px;
    }
    .select-all {
      padding: 8px 0;
      font-weight: 500;
    }
    .table-item {
      padding: 8px 0 8px 16px;
    }
    .record-count {
      color: #666;
      font-size: 0.85em;
      margin-left: 4px;
    }
    mat-divider {
      margin: 8px 0;
    }
  `]
})
export class DeleteTablesDialogComponent implements OnInit {
  tables: TableInfo[] = [];
  loading = true;
  deleting = false;

  private readonly API_URL = 'http://localhost:8080/api/data';

  constructor(
    private dialogRef: MatDialogRef<DeleteTablesDialogComponent>,
    private http: HttpClient,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    this.loadTableInfo();
  }

  loadTableInfo(): void {
    this.http.get<any[]>(`${this.API_URL}/tables`).subscribe({
      next: (tables) => {
        this.tables = tables.map(t => ({ ...t, selected: false }));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  get allSelected(): boolean {
    return this.tables.length > 0 && this.tables.every(t => t.selected);
  }

  get someSelected(): boolean {
    return this.tables.some(t => t.selected);
  }

  get hasSelection(): boolean {
    return this.tables.some(t => t.selected);
  }

  toggleAll(checked: boolean): void {
    this.tables.forEach(t => t.selected = checked);
  }

  confirmDelete(): void {
    const selectedKeys = this.tables.filter(t => t.selected).map(t => t.key);
    if (selectedKeys.length === 0) return;

    this.deleting = true;
    this.http.delete<any>(`${this.API_URL}/delete`, { body: selectedKeys }).subscribe({
      next: (result) => {
        this.dialogRef.close({ success: true, result });
      },
      error: (err) => {
        this.deleting = false;
        this.dialogRef.close({ success: false, error: err.error?.message || 'Delete failed' });
      }
    });
  }
}
