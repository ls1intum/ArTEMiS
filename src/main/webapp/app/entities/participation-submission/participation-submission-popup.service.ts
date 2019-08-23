import { Component, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { Participation, ParticipationService } from 'app/entities/participation';

@Injectable({ providedIn: 'root' })
export class ParticipationSubmissionPopupService {
    private ngbModalRef: NgbModalRef | null;

    constructor(private datePipe: DatePipe, private modalService: NgbModal, private router: Router, private participationService: ParticipationService) {
        this.ngbModalRef = null;
    }

    open(component: Component, id?: number | any): Promise<NgbModalRef> {
        return new Promise<NgbModalRef>((resolve, reject) => {
            if (this.ngbModalRef != null) {
                resolve(this.ngbModalRef);
            }

            if (id) {
                this.ngbModalRef = this.participationModalRef(component, id);
                resolve(this.ngbModalRef);
            }
        });
    }

    participationModalRef(component: Component, submissionId: number): NgbModalRef {
        const modalRef = this.modalService.open(component, { size: 'lg', backdrop: 'static' });
        modalRef.componentInstance.submissionId = submissionId;
        modalRef.result.then(
            result => {
                this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                this.ngbModalRef = null;
            },
            reason => {
                this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                this.ngbModalRef = null;
            },
        );
        return modalRef;
    }
}
