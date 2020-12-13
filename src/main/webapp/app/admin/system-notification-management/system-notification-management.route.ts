import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Route } from '@angular/router';
import { JhiResolvePagingParams } from 'ng-jhipster';
import { SystemNotificationManagementUpdateComponent } from 'app/admin/system-notification-management/system-notification-management-update.component';
import { SystemNotification } from 'app/entities/system-notification.model';
import { SystemNotificationService } from 'app/shared/notification/system-notification/system-notification.service';
import { SystemNotificationManagementComponent } from 'app/admin/system-notification-management/system-notification-management.component';
import { SystemNotificationManagementDetailComponent } from 'app/admin/system-notification-management/system-notification-management-detail.component';

@Injectable({ providedIn: 'root' })
export class SystemNotificationManagementResolve implements Resolve<any> {
    constructor(private service: SystemNotificationService) {}

    /**
     * Resolves the route and initializes system notification from id route param
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        if (route.params['id']) {
            return this.service.find(parseInt(route.params['id'], 10));
        }
        return new SystemNotification();
    }
}

export const systemNotificationManagementRoute: Route = {
    path: 'system-notification-management',
    component: SystemNotificationManagementComponent,
    resolve: {
        pagingParams: JhiResolvePagingParams,
    },
    data: {
        pageTitle: 'artemisApp.systemNotification.systemNotifications',
        defaultSort: 'id,asc',
    },
    children: [
        {
            path: 'new',
            component: SystemNotificationManagementUpdateComponent,
            data: {
                pageTitle: 'global.generic.create',
            },
        },
        {
            path: ':id',
            component: SystemNotificationManagementDetailComponent,
            resolve: {
                notification: SystemNotificationManagementResolve,
            },
            data: {
                pageTitle: 'artemisApp.systemNotification.systemNotifications',
                breadcrumbLabelVariable: 'notification.body.id',
            },
            children: [
                {
                    path: 'edit',
                    component: SystemNotificationManagementUpdateComponent,
                    data: {
                        pageTitle: 'global.generic.edit',
                    },
                },
            ],
        },
    ],
};
