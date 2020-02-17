import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ConfirmIconComponent } from 'app/components/confirm-icon/confirm-icon.component';

@NgModule({
    imports: [ArtemisSharedModule],
    declarations: [ConfirmIconComponent],
    exports: [ConfirmIconComponent],
})
export class ArtemisConfirmIconModule {}
