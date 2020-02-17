import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ConnectionNotificationComponent } from 'app/layouts/connection-notification/connection-notification.component';

@NgModule({
    imports: [ArtemisSharedModule],
    declarations: [ConnectionNotificationComponent],
    entryComponents: [],
    exports: [ConnectionNotificationComponent],
})
export class ArtemisConnectionNotificationModule {}
