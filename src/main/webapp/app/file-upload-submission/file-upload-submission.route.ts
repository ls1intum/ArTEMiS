import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { FileUploadSubmissionComponent } from './file-upload-submission.component';
import { PendingChangesGuard } from 'app/shared';

export const fileUploadSubmissionRoute: Routes = [
    {
        path: 'file-upload-submission/:participationId',
        component: FileUploadSubmissionComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'artemisApp.fileUploadExercise.home.title',
        },
        canActivate: [UserRouteAccessService],
        canDeactivate: [PendingChangesGuard],
    },
];
