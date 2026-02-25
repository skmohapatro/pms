import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiService, UploadResult } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface SheetOption {
  name: string;
  selected: boolean;
  description: string;
}

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  selectedFile: File | null = null;
  uploading = false;
  detectingSheets = false;
  result: UploadResult | null = null;
  sheetOptions: SheetOption[] = [];

  private readonly sheetDescriptions: Record<string, string> = {
    'Purchase Date Wise': 'Stock purchase history by date',
    'Dividend': 'Dividend income records',
    'RealizedP&L': 'Realized profit & loss records'
  };

  constructor(private api: ApiService, private http: HttpClient, private snackBar: MatSnackBar) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.result = null;
      this.sheetOptions = [];
      this.detectSheets();
    }
  }

  detectSheets(): void {
    if (!this.selectedFile) return;
    this.detectingSheets = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    this.http.post<string[]>('http://localhost:8080/api/upload/sheets', formData).subscribe({
      next: (sheets) => {
        this.sheetOptions = sheets.map(name => ({
          name,
          selected: true,
          description: this.sheetDescriptions[name] || 'Additional sheet'
        }));
        this.detectingSheets = false;
      },
      error: () => {
        this.detectingSheets = false;
        this.snackBar.open('Could not read sheet names from file', 'Close', { duration: 3000 });
      }
    });
  }

  get selectedSheets(): string[] {
    return this.sheetOptions.filter(s => s.selected).map(s => s.name);
  }

  get canUpload(): boolean {
    return !!this.selectedFile && this.selectedSheets.length > 0 && !this.uploading;
  }

  toggleAll(checked: boolean): void {
    this.sheetOptions.forEach(s => s.selected = checked);
  }

  get allSelected(): boolean {
    return this.sheetOptions.every(s => s.selected);
  }

  get someSelected(): boolean {
    return this.sheetOptions.some(s => s.selected) && !this.allSelected;
  }

  upload(): void {
    if (!this.selectedFile || this.selectedSheets.length === 0) return;
    this.uploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    this.selectedSheets.forEach(s => formData.append('sheets', s));
    this.http.post<UploadResult>('http://localhost:8080/api/upload', formData).subscribe({
      next: (res) => {
        this.result = res;
        this.uploading = false;
        this.snackBar.open(res.message, 'Close', { duration: 5000 });
      },
      error: (err) => {
        this.uploading = false;
        const msg = err.error?.message || 'Upload failed';
        this.snackBar.open(msg, 'Close', { duration: 5000 });
      }
    });
  }
}
