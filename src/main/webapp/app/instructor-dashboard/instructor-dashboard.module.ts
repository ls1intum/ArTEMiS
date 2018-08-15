import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArTEMiSSharedModule, UserRouteAccessService } from '../shared';
import { HomeComponent } from '../home';
import { MomentModule } from 'angular2-moment';
import { JhiMainComponent } from '../layouts';
import { InstructorDashboardComponent } from './instructor-dashboard.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ArTEMiSCoursesModule } from '../courses/courses.module';
import { ResultComponent } from '../courses/results/result.component';
import { InstructorDashboardArchiveDialogComponent, InstructorDashboardArchivePopupComponent } from './instructor-dashboard-archive-dialog.component';
import { InstructorDashboardCleanupDialogComponent, InstructorDashboardCleanupPopupComponent } from './instructor-dashboard-cleanup-dialog.component';
import { InstructorDashboardExportReposComponent, InstructorDashboardExportReposPopupComponent} from './instructor-dashboard-repo-export-dialog.component';
import { InstructorDashboardPopupService } from './instructor-dashboard-popup.service';
import { JhiResultDetailComponent } from '../courses';
import { InstructorDashboardResultDialogComponent, InstructorDashboardResultPopupComponent } from './instructor-dashboard-result-dialog.component';
import { SortByModule } from '../components/pipes/sort-by.module';

const ENTITY_STATES = [
    {
        path: 'course/:courseId/exercise/:exerciseId/dashboard',
        component: InstructorDashboardComponent,
        data: {
            authorities: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
            pageTitle: 'instructorDashboard.title'
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: 'participation/:participationId/result/new',
        component: InstructorDashboardResultPopupComponent,
        data: {
            authorities: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
            pageTitle: 'instructorDashboard.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    },
    {
        path: 'exercise/:id/archive',
        component: InstructorDashboardArchivePopupComponent,
        data: {
            authorities: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
            pageTitle: 'instructorDashboard.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    },
    {
        path: 'exercise/:id/cleanup',
        component: InstructorDashboardCleanupPopupComponent,
        data: {
            authorities: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
            pageTitle: 'instructorDashboard.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    },
    {
      path: 'exercise/:id/exportRepos',
      component: InstructorDashboardExportReposPopupComponent,
      data: {
        authorities: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
        pageTitle: 'instructorDashboard.title'
      },
      canActivate: [UserRouteAccessService],
      outlet: 'popup'
    }
];

@NgModule({
    imports: [
        ArTEMiSSharedModule,
        MomentModule,
        RouterModule.forChild(ENTITY_STATES),
        NgbModule,
        ArTEMiSCoursesModule,
        SortByModule,
    ],
    declarations: [
        InstructorDashboardComponent,
        InstructorDashboardArchiveDialogComponent,
        InstructorDashboardArchivePopupComponent,
        InstructorDashboardCleanupDialogComponent,
        InstructorDashboardCleanupPopupComponent,
        InstructorDashboardResultDialogComponent,
        InstructorDashboardResultPopupComponent,
        InstructorDashboardExportReposComponent,
        InstructorDashboardExportReposPopupComponent,
    ],
    entryComponents: [
        HomeComponent,
        InstructorDashboardComponent,
        JhiMainComponent,
        ResultComponent,
        JhiResultDetailComponent,
        InstructorDashboardArchiveDialogComponent,
        InstructorDashboardArchivePopupComponent,
        InstructorDashboardCleanupDialogComponent,
        InstructorDashboardCleanupPopupComponent,
        InstructorDashboardResultDialogComponent,
        InstructorDashboardResultPopupComponent,
        InstructorDashboardExportReposComponent,
        InstructorDashboardExportReposPopupComponent,
    ],
    providers: [
        InstructorDashboardPopupService
    ]
})
export class ArTEMiSInstructorDashboardModule {}
