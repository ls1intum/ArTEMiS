import { Routes } from '@angular/router';

import { UserRouteAccessService } from '../core';
import { CodeEditorComponent } from './code-editor.component';
import { PendingChangesGuard } from 'app/shared';

export const codeEditorRoute: Routes = [
    {
        path: 'code-editor/:participationId',
        component: CodeEditorComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arTeMiSApp.editor.home.title',
            flushRepositoryCacheAfter: 900000, // 15 min
            participationCache: {},
            repositoryCache: {},
        },
        canActivate: [UserRouteAccessService],
        canDeactivate: [PendingChangesGuard],
    },
];
