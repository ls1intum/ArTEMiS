import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AlertService } from 'app/core/alert/alert.service';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { Result } from 'app/entities/result/result.model';
import { HttpErrorResponse } from '@angular/common/http';
import { Moment } from 'moment';
import { ComplaintResponseService } from 'app/entities/complaint-response/complaint-response.service';
import { filter } from 'rxjs/operators';
import { ComplaintResponse } from 'app/entities/complaint-response/complaint-response.model';
import { Complaint, ComplaintType } from 'app/entities/complaint/complaint.model';

@Component({
    selector: 'jhi-complaint-form',
    templateUrl: './complaints.component.html',
    styleUrls: ['complaints.component.scss'],
    providers: [],
})
export class ComplaintsComponent implements OnInit {
    @Input() resultId: number;
    @Input() allowedComplaints: number; // the number of complaints that a student can still submit in the course
    @Input() complaintType: ComplaintType;
    @Output() submit: EventEmitter<void> = new EventEmitter();
    complaintText = '';
    alreadySubmitted: boolean;
    submittedDate: Moment;
    accepted: boolean;
    handled: boolean;
    complaintResponse: ComplaintResponse;
    ComplaintType = ComplaintType;
    loaded = true;

    readonly maxComplaintNumberPerStudent = 3; // please note that this number has to be the same as in Constant.java on the server

    constructor(private complaintService: ComplaintService, private jhiAlertService: AlertService, private complaintResponseService: ComplaintResponseService) {}

    ngOnInit(): void {
        this.complaintService
            .findByResultId(this.resultId)
            .pipe(filter(res => !!res.body))
            .subscribe(
                res => {
                    this.complaintText = res.body!.complaintText;
                    this.alreadySubmitted = true;
                    this.submittedDate = res.body!.submittedTime!;
                    this.accepted = res.body!.accepted;
                    this.handled = this.accepted !== undefined;

                    if (this.handled) {
                        this.complaintResponseService.findByComplaintId(res.body!.id).subscribe(complaintResponse => (this.complaintResponse = complaintResponse.body!));
                    }
                },
                (err: HttpErrorResponse) => {
                    this.onError(err.message);
                },
            );
    }

    createComplaint(): void {
        this.loaded = false;
        const complaint = new Complaint();
        complaint.complaintText = this.complaintText;
        complaint.result = new Result();
        complaint.result.id = this.resultId;
        complaint.complaintType = this.complaintType;

        this.complaintService.create(complaint).subscribe(
            res => {
                this.submittedDate = res.body!.submittedTime!;
                this.alreadySubmitted = true;
                if (complaint.complaintType === ComplaintType.COMPLAINT) {
                    this.allowedComplaints--;
                }
                this.loaded = true;
                this.submit.emit();
            },
            (err: HttpErrorResponse) => {
                this.loaded = true;
                if (err && err.error && err.error.errorKey === 'toomanycomplaints') {
                    this.jhiAlertService.error('artemisApp.complaint.tooManyComplaints', { maxComplaintNumber: this.maxComplaintNumberPerStudent });
                } else {
                    this.onError(err.message);
                }
            },
        );
    }

    private onError(error: string) {
        console.error(error);
        this.jhiAlertService.error('error.http.400', null, undefined);
    }
}
