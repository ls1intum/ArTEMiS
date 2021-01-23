import { Component, EventEmitter, Input, Output, AfterViewInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Feedback, FeedbackType } from 'app/entities/feedback.model';
import { StructuredGradingCriterionService } from 'app/exercises/shared/structured-grading-criterion/structured-grading-criterion.service';

@Component({
    selector: 'jhi-assessment-detail',
    templateUrl: './assessment-detail.component.html',
    styleUrls: ['./assessment-detail.component.scss'],
})
export class AssessmentDetailComponent implements AfterViewInit {
    @Input() public assessment: Feedback;
    @Output() public assessmentChange = new EventEmitter<Feedback>();
    @Output() public deleteAssessment = new EventEmitter<Feedback>();
    @Input() public disabled = false;
    @Input() public readOnly: boolean;
    disableEditScore = false;

    public FeedbackType_AUTOMATIC = FeedbackType.AUTOMATIC;
    constructor(private translateService: TranslateService, public structuredGradingCriterionService: StructuredGradingCriterionService) {}

    ngAfterViewInit(): void {
        this.computeDisableEditScore();
    }

    /**
     * Emits assessment changes to parent component
     */
    public emitChanges(): void {
        if (this.assessment.type === FeedbackType.AUTOMATIC) {
            this.assessment.type = FeedbackType.AUTOMATIC_ADAPTED;
        }
        this.assessmentChange.emit(this.assessment);
    }
    /**
     * Emits the delete of an assessment
     */
    public delete() {
        const text: string = this.translateService.instant('artemisApp.feedback.delete.question', { id: this.assessment.id ?? '' });
        const confirmation = confirm(text);
        if (confirmation) {
            this.deleteAssessment.emit(this.assessment);
        }
    }

    updateAssessmentOnDrop(event: Event) {
        this.structuredGradingCriterionService.updateFeedbackWithStructuredGradingInstructionEvent(this.assessment, event);
        this.computeDisableEditScore();
        this.assessmentChange.emit(this.assessment);
    }

    private computeDisableEditScore() {
        this.disableEditScore = !!(this.assessment.gradingInstruction && this.assessment.gradingInstruction.usageCount !== 0);
    }
}
