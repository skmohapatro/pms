import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { NgChartsModule } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { UploadComponent } from './components/upload/upload.component';
import { PurchasesComponent } from './components/purchases/purchases.component';
import { PurchaseDialogComponent } from './components/purchases/purchase-dialog.component';
import { DeleteTablesDialogComponent } from './components/purchases/delete-tables-dialog.component';
import { CompanyWiseComponent } from './components/company-wise/company-wise.component';
import { GroupsComponent } from './components/groups/groups.component';
import { GroupAssignDialogComponent } from './components/groups/group-assign-dialog.component';
import { AnalyticsComponent } from './components/analytics/analytics.component';
import { DividendComponent } from './components/dividend/dividend.component';
import { DividendDialogComponent } from './components/dividend/dividend-dialog.component';
import { RealizedPnLComponent } from './components/realized-pnl/realized-pnl.component';
import { RealizedPnLDialogComponent } from './components/realized-pnl/realized-pnl-dialog.component';
import { LiveStockComponent } from './components/live-stock/live-stock.component';
import { WatchlistComponent } from './components/watchlist/watchlist.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

@NgModule({
  declarations: [
    AppComponent,
    UploadComponent,
    PurchasesComponent,
    PurchaseDialogComponent,
    DeleteTablesDialogComponent,
    CompanyWiseComponent,
    GroupsComponent,
    GroupAssignDialogComponent,
    AnalyticsComponent,
    DividendComponent,
    DividendDialogComponent,
    RealizedPnLComponent,
    RealizedPnLDialogComponent,
    LiveStockComponent,
    WatchlistComponent,
    DashboardComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatListModule,
    MatTooltipModule,
    MatTabsModule,
    MatCheckboxModule,
    MatDividerModule,
    NgChartsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
