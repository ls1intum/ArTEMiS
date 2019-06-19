import { Feedback } from 'app/entities/feedback';

enum FeedbackType {
    POSITIVE = 'positive',
    NEGATIVE = 'negative',
    NEUTRAL = 'neutral',
}

export class TextResultBlock {
    constructor(public text: string, public startIndex: number, public feedback?: Feedback) {
        this.text = text;
        this.startIndex = startIndex;
        this.feedback = feedback;
    }

    get length(): number {
        return this.text.length;
    }

    get endIndex(): number {
        return this.startIndex + this.length;
    }

    get feedbackType(): FeedbackType {
        if (!this.feedback || this.feedback.credits === 0 || this.feedback.credits == null) {
            return FeedbackType.NEUTRAL;
        } else if (this.feedback.credits > 0) {
            return FeedbackType.POSITIVE;
        } else if (this.feedback.credits < 0) {
            return FeedbackType.NEGATIVE;
        }
        return FeedbackType.NEUTRAL;
    }

    get cssClass(): string {
        return this.feedbackType ? `text-with-feedback ${this.feedbackType}-feedback` : '';
    }

    get icon(): string {
        if (this.feedbackType === FeedbackType.POSITIVE) {
            return 'check';
        } else if (this.feedbackType === FeedbackType.NEGATIVE) {
            return 'times';
        } else {
            // if (this.feedbackType === FeedbackType.NEUTRAL)
            return 'dot';
        }
    }

    get iconCssClass(): string {
        return this.feedbackType ? `feedback-icon ${this.feedbackType}-feedback` : '';
    }

    get feedbackCssClass(): string {
        if (this.feedbackType === FeedbackType.POSITIVE) {
            return 'alert alert-success';
        } else if (this.feedbackType === FeedbackType.NEGATIVE) {
            return 'alert alert-danger';
        } else {
            // if (this.feedbackType === FeedbackType.NEUTRAL)
            return 'alert alert-secondary';
        }
    }
}
