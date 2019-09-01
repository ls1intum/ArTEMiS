import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { Subscription } from 'rxjs/Subscription';
import { ParticipationSubmissionPopupService } from 'app/entities/participation-submission/participation-submission-popup.service';
import { SubmissionService } from 'app/entities/submission/submission.service';

@Component({
    selector: 'jhi-participation-submission-delete-dialog',
    templateUrl: './participation-submission-delete-dialog.component.html',
})
export class ParticipationSubmissionDeleteDialogComponent implements OnInit {
    submissionId: number;

    constructor(private submissionService: SubmissionService, public activeModal: NgbActiveModal, private eventManager: JhiEventManager) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.submissionService.delete(id).subscribe(() => {
            this.eventManager.broadcast({
                name: 'submissionsModification',
                content: 'Deleted a submission',
            });
            this.activeModal.dismiss(true);
        });
    }

    ngOnInit(): void {}
}

@Component({
    selector: 'jhi-participation-submission-delete-popup',
    template: '',
})
export class ParticipationSubmissionDeletePopupComponent implements OnInit, OnDestroy {
    routeSub: Subscription;
    constructor(private route: ActivatedRoute, private participationSubmissionPopupService: ParticipationSubmissionPopupService) {}

    ngOnInit() {
        this.routeSub = this.route.params.subscribe(params => {
            this.participationSubmissionPopupService.open(ParticipationSubmissionDeleteDialogComponent as Component, params['participationId'], params['submissionId']);
        });
    }

    ngOnDestroy() {
        this.routeSub.unsubscribe();
    }
}
