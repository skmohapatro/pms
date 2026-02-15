import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UploadComponent } from './components/upload/upload.component';
import { PurchasesComponent } from './components/purchases/purchases.component';
import { CompanyWiseComponent } from './components/company-wise/company-wise.component';
import { GroupsComponent } from './components/groups/groups.component';
import { AnalyticsComponent } from './components/analytics/analytics.component';

const routes: Routes = [
  { path: '', redirectTo: '/upload', pathMatch: 'full' },
  { path: 'upload', component: UploadComponent },
  { path: 'purchases', component: PurchasesComponent },
  { path: 'company-wise', component: CompanyWiseComponent },
  { path: 'groups', component: GroupsComponent },
  { path: 'analytics', component: AnalyticsComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
