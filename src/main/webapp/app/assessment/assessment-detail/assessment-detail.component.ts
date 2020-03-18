import { Component, EventEmitter, Input, Output } from '@angular/core';
import { HighlightColors } from 'app/exercises/text/assess/highlight-colors';
import { TextBlock } from 'app/entities/text-block.model';
import { Feedback, FeedbackType } from 'app/entities/feedback.model';
import { convertFromHtmlLinebreaks, sanitize } from 'app/utils/text.utils';

@Component({
    selector: 'jhi-assessment-detail',
    templateUrl: './assessment-detail.component.html',
    styleUrls: ['./assessment-detail.component.scss'],
})
export class AssessmentDetailComponent {
    @Input() public assessment: Feedback;
    @Input() public block: TextBlock | undefined;
    @Output() public assessmentChange = new EventEmitter<Feedback>();
    @Input() public highlightColor: HighlightColors.Color;
    @Output() public deleteAssessment = new EventEmitter<Feedback>();
    @Input() public disabled = false;

    public FeedbackType_AUTOMATIC = FeedbackType.AUTOMATIC;

    public emitChanges(): void {
        if (this.assessment.type === FeedbackType.AUTOMATIC) {
            this.assessment.type = FeedbackType.AUTOMATIC_ADAPTED;
        }
        this.assessmentChange.emit(this.assessment);
    }

    public delete() {
        const referencedText = convertFromHtmlLinebreaks(this.text);
        const confirmationMessage = referencedText ? `Delete Assessment for "${referencedText}"?` : 'Delete Assessment?';
        const confirmation = confirm(confirmationMessage);
        if (confirmation) {
            this.deleteAssessment.emit(this.assessment);
        }
    }

    private get text(): string {
        return this.block?.text || this.assessment.reference || '';
    }

    public get sanitizedText(): string {
        return sanitize(this.text);
    }
    allowDrop(ev: any) {
        ev.preventDefault();
    }
    drop(ev: any) {
        ev.preventDefault();
        const data = ev.dataTransfer.getData('text');
        const instruction = JSON.parse(data);
        const credits = instruction.credits;
        const feedback = instruction.feedback;
        this.assessment.credits = credits;
        this.assessment.detailText = feedback;
        this.assessmentChange.emit(this.assessment);
    }
}
