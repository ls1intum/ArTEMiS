import { NgModule } from '@angular/core';
import { JhiAlertService } from 'ng-jhipster';

import { ComplaintsComponent } from './complaints.component';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { ComplaintInteractionsComponent } from 'app/complaints/complaint-interactions.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [ArtemisSharedModule, MomentModule, ClipboardModule],
    declarations: [ComplaintsComponent, ComplaintInteractionsComponent],
    exports: [ComplaintsComponent, ComplaintInteractionsComponent],
    providers: [JhiAlertService, ComplaintService],
})
export class ArtemisComplaintsModule {}
