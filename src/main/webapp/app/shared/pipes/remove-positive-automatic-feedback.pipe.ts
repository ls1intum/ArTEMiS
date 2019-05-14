import { PipeTransform, Pipe } from '@angular/core';
import { Feedback } from 'app/entities/feedback';

@Pipe({ name: 'removepositiveautomaticfeedback' })
export class RemovePositiveAutomaticFeedbackPipe implements PipeTransform {
    transform(feedbacks: Feedback[]): any {
        // Automatic feedback that is positive gets removed as it will only contain the testname, but not any useful feedback for the student
        // Manual feedback gets not filtered as some feedback might be provided even if the feedback is positive
        return feedbacks.filter(feedback => !(feedback.type === 'AUTOMATIC' && feedback.positive));
    }
}
