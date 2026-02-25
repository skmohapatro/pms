import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UploadComponent } from './components/upload/upload.component';
import { PurchasesComponent } from './components/purchases/purchases.component';
import { CompanyWiseComponent } from './components/company-wise/company-wise.component';
import { GroupsComponent } from './components/groups/groups.component';
import { AnalyticsComponent } from './components/analytics/analytics.component';
import { ChatComponent } from './components/chat/chat.component';
import { DividendComponent } from './components/dividend/dividend.component';
import { RealizedPnLComponent } from './components/realized-pnl/realized-pnl.component';
import { LiveStockComponent } from './components/live-stock/live-stock.component';
import { WatchlistComponent } from './components/watchlist/watchlist.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'upload', component: UploadComponent },
  { path: 'purchases', component: PurchasesComponent },
  { path: 'company-wise', component: CompanyWiseComponent },
  { path: 'groups', component: GroupsComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'chat', component: ChatComponent },
  { path: 'dividends', component: DividendComponent },
  { path: 'realized-pnl', component: RealizedPnLComponent },
  { path: 'live-stock', component: LiveStockComponent },
  { path: 'watchlist', component: WatchlistComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
