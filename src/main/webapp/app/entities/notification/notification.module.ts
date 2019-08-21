import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared';
import { NotificationComponent, notificationRoute, NotificationService } from './';
import { RouterModule } from '@angular/router';

@NgModule({
    imports: [ArtemisSharedModule, RouterModule.forChild(notificationRoute)],
    declarations: [NotificationComponent],
    entryComponents: [NotificationComponent],
    exports: [],
    providers: [NotificationService],
})
export class ArtemisNotificationModule {}
