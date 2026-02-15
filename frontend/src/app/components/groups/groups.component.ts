import { Component, OnInit } from '@angular/core';
import { ApiService, InvestmentGroup, CompanyWiseAggregated, GroupDetail } from '../../services/api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { GroupAssignDialogComponent } from './group-assign-dialog.component';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss']
})
export class GroupsComponent implements OnInit {
  groups: InvestmentGroup[] = [];
  instruments: CompanyWiseAggregated[] = [];
  selectedGroupDetail: GroupDetail | null = null;
  newGroupName = '';

  constructor(
    private api: ApiService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadGroups();
    this.loadInstruments();
  }

  loadGroups(): void {
    this.api.getGroups().subscribe({
      next: (data) => this.groups = data,
      error: () => this.snackBar.open('Failed to load groups', 'Close', { duration: 3000 })
    });
  }

  loadInstruments(): void {
    this.api.getCompanyWiseData().subscribe({
      next: (data) => this.instruments = data
    });
  }

  createGroup(): void {
    if (!this.newGroupName.trim()) return;
    this.api.createGroup(this.newGroupName.trim()).subscribe({
      next: () => {
        this.snackBar.open('Group created', 'Close', { duration: 3000 });
        this.newGroupName = '';
        this.loadGroups();
      },
      error: () => this.snackBar.open('Failed to create group (may already exist)', 'Close', { duration: 3000 })
    });
  }

  deleteGroup(id: number): void {
    if (confirm('Delete this group?')) {
      this.api.deleteGroup(id).subscribe({
        next: () => {
          this.snackBar.open('Group deleted', 'Close', { duration: 3000 });
          this.loadGroups();
          if (this.selectedGroupDetail?.groupId === id) {
            this.selectedGroupDetail = null;
          }
        },
        error: () => this.snackBar.open('Failed to delete group', 'Close', { duration: 3000 })
      });
    }
  }

  selectGroup(group: InvestmentGroup): void {
    this.api.getGroupDetail(group.id).subscribe({
      next: (detail) => this.selectedGroupDetail = detail,
      error: () => this.snackBar.open('Failed to load group detail', 'Close', { duration: 3000 })
    });
  }

  openAssignDialog(group: InvestmentGroup): void {
    const dialogRef = this.dialog.open(GroupAssignDialogComponent, {
      width: '600px',
      data: { group, instruments: this.instruments }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.api.assignInstruments(group.id, result).subscribe({
          next: () => {
            this.snackBar.open('Instruments assigned', 'Close', { duration: 3000 });
            this.loadGroups();
            this.selectGroup(group);
          },
          error: () => this.snackBar.open('Failed to assign instruments', 'Close', { duration: 3000 })
        });
      }
    });
  }

  removeInstrument(instrumentId: number): void {
    if (!this.selectedGroupDetail) return;
    this.api.removeInstrumentFromGroup(this.selectedGroupDetail.groupId, instrumentId).subscribe({
      next: () => {
        this.snackBar.open('Instrument removed', 'Close', { duration: 3000 });
        this.selectGroup({ id: this.selectedGroupDetail!.groupId, groupName: this.selectedGroupDetail!.groupName, instruments: [] });
      },
      error: () => this.snackBar.open('Failed to remove instrument', 'Close', { duration: 3000 })
    });
  }
}
