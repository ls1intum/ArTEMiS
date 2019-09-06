import { Component, Input, OnInit } from '@angular/core';
import { Result, ResultService } from './';
import { RepositoryService } from 'app/entities/repository';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Feedback } from '../feedback/index';
import { BuildLogEntry, BuildLogEntryArray } from 'app/entities/build-log';

// Modal -> Result details view
@Component({
    selector: 'jhi-result-detail',
    templateUrl: './result-detail.component.html',
})
export class ResultDetailComponent implements OnInit {
    @Input() result: Result;
    @Input() showTestNames = false;
    isLoading: boolean;
    feedbackList: Feedback[];
    buildLogs: BuildLogEntryArray;

    constructor(public activeModal: NgbActiveModal, private resultService: ResultService, private repositoryService: RepositoryService) {}

    ngOnInit(): void {
        if (this.result.feedbacks && this.result.feedbacks.length > 0) {
            // make sure to reuse existing feedback items and to load feedback at most only once when this component is opened
            this.feedbackList = this.result.feedbacks;
            return;
        }
        this.isLoading = true;
        this.resultService.getFeedbackDetailsForResult(this.result.id).subscribe(res => {
            this.result.feedbacks = res.body!;
            this.feedbackList = res.body!;
            if (!this.feedbackList || this.feedbackList.length === 0) {
                // If we don't have received any feedback, we fetch the buid log outputs
                this.repositoryService.buildlogs(this.result.participation!.id).subscribe((repoResult: BuildLogEntry[]) => {
                    this.buildLogs = new BuildLogEntryArray(...repoResult);
                    this.isLoading = false;
                });
            } else {
                this.isLoading = false;
            }
        });
        this.isLoading = false;
    }
}
