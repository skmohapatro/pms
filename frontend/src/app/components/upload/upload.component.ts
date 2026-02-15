import { Component } from '@angular/core';
import { ApiService, UploadResult } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  selectedFile: File | null = null;
  uploading = false;
  result: UploadResult | null = null;

  constructor(private api: ApiService, private snackBar: MatSnackBar) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.result = null;
    }
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.api.uploadExcel(this.selectedFile).subscribe({
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
