import { BaseEntity } from './../../shared';
import { QuestionStatistic } from '../question-statistic';
import { Exercise } from '../exercise';

export const enum ScoringType {
    'ALL_OR_NOTHING',
    'PROPORTIONAL_CORRECT_OPTIONS',
    'TRUE_FALSE_NEUTRAL'
}

// IMPORTANT NOTICE: The following strings have to be consistent with the ones defined in Question.java
export const enum QuestionType {
    MULTIPLE_CHOICE = 'multiple-choice',
    DRAG_AND_DROP = 'drag-and-drop'
}

export abstract class Question implements BaseEntity {

    public id: number;
    public title: string;
    public text: string;
    public hint: string;
    public explanation: string;
    public score: number;
    public scoringType: ScoringType;
    public randomizeOrder = true;   // default value
    public invalid = false;         // default value
    public questionStatistic: QuestionStatistic;
    public exercise: Exercise;
    public exportQuiz = false;      // default value
    public type: QuestionType;

    constructor(type: QuestionType) {
        this.type = type;
    }
}
