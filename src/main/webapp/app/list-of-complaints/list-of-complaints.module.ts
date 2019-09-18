import { NgModule } from '@angular/core';
import { JhiAlertService } from 'ng-jhipster';

import { ArtemisSharedModule } from '../shared';
import { ListOfComplaintsComponent } from './list-of-complaints.component';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { RouterModule } from '@angular/router';
import { listOfComplaintsRoute } from 'app/list-of-complaints/list-of-complaints.route';
import { SortByModule } from 'app/components/pipes';

const ENTITY_STATES = [...listOfComplaintsRoute];

@NgModule({
    imports: [ArtemisSharedModule, MomentModule, ClipboardModule, RouterModule.forChild(ENTITY_STATES), SortByModule],
    declarations: [ListOfComplaintsComponent],
    exports: [ListOfComplaintsComponent],
    providers: [JhiAlertService, ComplaintService],
})
export class ArtemisListOfComplaintsModule {}
