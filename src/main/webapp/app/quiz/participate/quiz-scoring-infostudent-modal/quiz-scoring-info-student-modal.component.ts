import { Component, OnInit, Input } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Question, QuestionType, ScoringType } from 'app/entities/question';
import { DragAndDropMapping } from '../../../entities/drag-and-drop-mapping';
import { AnswerOption } from '../../../entities/answer-option';
import { MultipleChoiceQuestion } from '../../../entities/multiple-choice-question';
import { DragAndDropQuestion } from '../../../entities/drag-and-drop-question';
import { ShortAnswerQuestion } from '../../../entities/short-answer-question';
import { ShortAnswerSubmittedText } from 'app/entities/short-answer-submitted-text';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-quiz-scoring-infostudent-modal',
    templateUrl: './quiz-scoring-info-student-modal.component.html',
    styles: []
})
export class QuizScoringInfoStudentModalComponent implements OnInit {
    readonly DRAG_AND_DROP = QuestionType.DRAG_AND_DROP;
    readonly MULTIPLE_CHOICE = QuestionType.MULTIPLE_CHOICE;
    readonly SHORT_ANSWER = QuestionType.SHORT_ANSWER;

    readonly ALL_OR_NOTHING = ScoringType.ALL_OR_NOTHING;
    readonly PROPORTIONAL_WITH_PENALTY = ScoringType.PROPORTIONAL_WITH_PENALTY;

    @Input() score: number; // Score of the student that has been achieved
    @Input() questionIndex: number; // Question Index of the question
    @Input() question: Question;
    @Input() dragAndDropMapping = new Array<DragAndDropMapping>();
    @Input() multipleChoiceMapping = new Array<AnswerOption>();
    @Input() shortAnswerText = new Array<ShortAnswerSubmittedText>();
    @Input() correctlyMappedDragAndDropItems: number; // Amount of correctly mapped drag and drop items

    /* Multiple Choice Counting Variables*/
    multipleChoiceCorrectAnswerCorrectlyChosen: number; // Amount of right options chosen by the student
    multipleChoiceWrongAnswerChosen: number; // Amount of wrong options chosen by the student
    correctMultipleChoiceAnswers: number; // Amount of correct options for the question
    forgottenMultipleChoiceRightAnswers: number; // Amount of wrong options for the question
    multipleChoiceAnswerOptions: number; // Amount of all possible options for the question
    inTotalSelectedRightOptions: number; // Amount of correct and wrong options assigned correctly
    inTotalSelectedWrongOptions: number; // Amount of correct and wrong options assigned wrongly
    differenceMultipleChoice: number; // Difference between inTotalSelectedRightOptions and differenceMultipleChoice

    /* Drag and Drop Counting Variables*/
    dragAndDropZones: number; // Amount of drag and drop Zones
    wronglyMappedDragAndDropItems: number; // Amount of wrongly mapped drag and drop item
    differenceDragAndDrop: number; // Difference between the wronglyMappedDragAndDropItems and correctlyMappedDragAndDropItems

    /* Short Answer Counting Variables*/
    shortAnswerSpots: number; // Amount of short answer spots
    shortAnswerCorrectAnswers: number; // A mount of correctly filled out spots
    shortAnswerWrongAnswers: number; // A mount of wrongly filled out spots
    differenceShortAnswer: number; // Difference between shortAnswerCorrectAnswers and shortAnswerWrongAnswers

    /* Plural Variables*/
    questionPoint: string;
    scorePoint: string;
    wrongOption: string;
    rightOption: string;
    rightMap: string;
    wrongMap: string;
    rightGap: string;
    wrongGap: string;

    constructor(
        private modalService: NgbModal,
        private translateService: TranslateService) {
    }

    ngOnInit() {
        this.checkForSingleOrPluralPoints();
        switch (this.question.type) {
            case QuestionType.MULTIPLE_CHOICE:
                this.countMultipleChoice();
                break;
            case QuestionType.DRAG_AND_DROP:
                this.countDragAndDrop();
                break;
            case QuestionType.SHORT_ANSWER:
                this.countShortAnswer();
                break;
        }
    }

    /**
     * opens the pop-up for the explanation of the points
     */
    open(content: any) {
        this.modalService.open(content, {size: 'lg'});
    }

    /**
     * counts the variables for Multiple Choice Questions
     */
    private countMultipleChoice() {
        const translationBasePath = 'arTeMiSApp.quizExercise.explanationText.';
        const mcmQuestion = this.question as MultipleChoiceQuestion;
        this.multipleChoiceAnswerOptions = mcmQuestion.answerOptions.length;
        this.correctMultipleChoiceAnswers = mcmQuestion.answerOptions.filter(option => option.isCorrect).length;
        this.multipleChoiceCorrectAnswerCorrectlyChosen = this.multipleChoiceMapping.filter(option => option.isCorrect).length;
        this.multipleChoiceWrongAnswerChosen = this.multipleChoiceMapping.filter(option => !option.isCorrect).length;
        this.forgottenMultipleChoiceRightAnswers = this.correctMultipleChoiceAnswers - this.multipleChoiceCorrectAnswerCorrectlyChosen;
        this.inTotalSelectedRightOptions = this.multipleChoiceCorrectAnswerCorrectlyChosen + (this.multipleChoiceAnswerOptions - this.correctMultipleChoiceAnswers - this.multipleChoiceWrongAnswerChosen);
        this.inTotalSelectedWrongOptions = this.multipleChoiceWrongAnswerChosen + this.forgottenMultipleChoiceRightAnswers;
        this.differenceMultipleChoice = this.inTotalSelectedRightOptions - this.inTotalSelectedWrongOptions;

        if (this.inTotalSelectedRightOptions === 1) {
            this.rightOption = this.translateService.instant(translationBasePath + 'option');
        } else {
            this.rightOption = this.translateService.instant(translationBasePath + 'options');
        }

        if (this.inTotalSelectedWrongOptions === 1) {
            this.wrongOption = this.translateService.instant(translationBasePath + 'option');
        } else {
            this.wrongOption = this.translateService.instant(translationBasePath + 'options');
        }
    }

    /**
     * counts the variables for Drag and Drop Questions
     */
    private countDragAndDrop() {
        const translationBasePath = 'arTeMiSApp.quizExercise.explanationText.';
        const dndQuestion = this.question as DragAndDropQuestion;
        this.dragAndDropZones = dndQuestion.dropLocations.length;
        this.wronglyMappedDragAndDropItems = this.dragAndDropZones - this.correctlyMappedDragAndDropItems;
        this.differenceDragAndDrop = this.correctlyMappedDragAndDropItems - this.wronglyMappedDragAndDropItems;

        if (this.correctlyMappedDragAndDropItems === 1) {
            this.rightMap = this.translateService.instant(translationBasePath + 'item');
        } else {
            this.rightMap = this.translateService.instant(translationBasePath + 'items');
        }

        if (this.wronglyMappedDragAndDropItems === 1) {
            this.wrongMap = this.translateService.instant(translationBasePath + 'item');
        } else {
            this.wrongMap = this.translateService.instant(translationBasePath + 'items');
        }
    }

    /**
     * counts the variables for Short Answer Questions
     */
    private countShortAnswer() {
        const translationBasePath = 'arTeMiSApp.quizExercise.explanationText.';
        const shortAnswer = this.question as ShortAnswerQuestion;
        this.shortAnswerSpots = shortAnswer.spots.length;
        this.shortAnswerCorrectAnswers = this.shortAnswerText.filter(option => option.isCorrect).length;
        this.shortAnswerWrongAnswers = this.shortAnswerSpots - this.shortAnswerCorrectAnswers;
        this.differenceShortAnswer = this.shortAnswerCorrectAnswers - this.shortAnswerWrongAnswers;

        if (this.shortAnswerCorrectAnswers === 1) {
            this.rightGap = this.translateService.instant(translationBasePath + 'textgap');
        } else {
            this.rightGap = this.translateService.instant(translationBasePath + 'textgaps');
        }

        if (this.shortAnswerWrongAnswers === 1) {
            this.wrongGap = this.translateService.instant(translationBasePath + 'textgap');
        } else {
            this.wrongGap = this.translateService.instant(translationBasePath + 'textgaps');
        }
    }

    /**
     * Checks the score of the question and the score the student has achieved, depending on that write either point or points
     */
    private checkForSingleOrPluralPoints() {
        const translationBasePath = 'arTeMiSApp.quizExercise.explanationText.';
        if (this.question.score === 1) {
            this.questionPoint = this.translateService.instant(translationBasePath + 'point');
        } else {
            this.questionPoint = this.translateService.instant(translationBasePath + 'points');
        }

        if (this.score === 1) {
            this.scorePoint = this.translateService.instant(translationBasePath + 'point');
        } else {
            this.scorePoint = this.translateService.instant(translationBasePath + 'points');
        }
    }
}
