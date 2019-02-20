import { Component, OnInit, Input } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Question, QuestionType, ScoringType } from 'app/entities/question';
import { DragAndDropMapping } from '../../../entities/drag-and-drop-mapping';
import { AnswerOption } from '../../../entities/answer-option';
import { MultipleChoiceQuestion } from '../../../entities/multiple-choice-question';
import { DragAndDropQuestion } from '../../../entities/drag-and-drop-question';
import { ShortAnswerQuestion } from '../../../entities/short-answer-question';
import { ShortAnswerSubmittedText } from 'app/entities/short-answer-submitted-text';

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
    @Input() question: Question;
    @Input() DragAndDropMapping = new Array<DragAndDropMapping>();
    @Input() MultipleChoiceMapping = new Array<AnswerOption>();
    @Input() ShortAnswerText = new Array<ShortAnswerSubmittedText>();

    /* Multiple Choice Counting Variables*/
    multipleChoiceCorrectAnswerCorrectlyChosen: number; //Amount of right options chosen by the student
    multipleChoiceWrongAnswerChosen: number; //Amount of wrong options chosen by the student
    amountOfCorrectMultipleChoiceAnswers:number; //Amount of correct options for the question
    forgottenMultipleChoiceRightAnswers: number; //Amount of wrong options for the question
    amountOfMultipleChoiceAnswerOptions: number; //Amount of all possible options for the question
    inTotalSelectedRightOptions: number; //Amount of correct and wrong options assigned correctly
    inTotalSelectedWrongOptions: number; //Amount of correct and wrong options assigned wrongly
    differenceMultipleChoice: number; //Difference between inTotalSelectedRightOptions and differenceMultipleChoice

    /* Drag and Drop Counting Variables*/
    amountOfDragAndDropZones: number; //Amount of drag and drop Zones
    wronglyMappedDragAndDropItems: number; // Amount of wrongly mapped drag and drop item
    @Input() correctlyMappedDragAndDropItems: number; //Amount of correctly mapped drag and drop items
    differenceDragAndDrop: number; //Difference between the wronglyMappedDragAndDropItems and correctlyMappedDragAndDropItems

    /* Short Answer Counting Variables*/
    shortAnswerSpotCount: number; //Amount of short answer spots
    shortAnswerCorrectAnswers: number; //Amount of correctly filled out spots
    shortAnswerWrongAnswers: number; //Amount of wrongly filled out spots
    differenceShortAnswer: number; //Difference between shortAnswerCorrectAnswers and shortAnswerWrongAnswers


    constructor(private modalService: NgbModal) {
    }

    ngOnInit() {
        switch (this.question.type) {
            case QuestionType.MULTIPLE_CHOICE:
                this.countMultipleChoice();
                break;
            case QuestionType.DRAG_AND_DROP:
                this.countDragandDrop();
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
        const mcmQuestion = this.question as MultipleChoiceQuestion;
            this.amountOfMultipleChoiceAnswerOptions = mcmQuestion.answerOptions.length;
            this.amountOfCorrectMultipleChoiceAnswers = mcmQuestion.answerOptions.filter(option => option.isCorrect).length;
            this.multipleChoiceCorrectAnswerCorrectlyChosen = this.MultipleChoiceMapping.filter(option => option.isCorrect).length;
            this.multipleChoiceWrongAnswerChosen = this.MultipleChoiceMapping.filter(option => !option.isCorrect).length;
            this.forgottenMultipleChoiceRightAnswers = this.amountOfCorrectMultipleChoiceAnswers - this.multipleChoiceCorrectAnswerCorrectlyChosen;
            this.inTotalSelectedRightOptions = this.multipleChoiceCorrectAnswerCorrectlyChosen + (this.amountOfMultipleChoiceAnswerOptions - this.amountOfCorrectMultipleChoiceAnswers-this.multipleChoiceWrongAnswerChosen);
            this.inTotalSelectedWrongOptions = this.multipleChoiceWrongAnswerChosen + this.forgottenMultipleChoiceRightAnswers;
            this.differenceMultipleChoice = this.inTotalSelectedRightOptions - this.inTotalSelectedWrongOptions;
    }

    /**
     * counts the variables for Drag and Drop Questions
     */
    private countDragandDrop() {
        const dndQuestion = this.question as DragAndDropQuestion;
            this.amountOfDragAndDropZones = dndQuestion.dropLocations.length;
            this.wronglyMappedDragAndDropItems = this.amountOfDragAndDropZones - this.correctlyMappedDragAndDropItems;
            this.differenceDragAndDrop = this.correctlyMappedDragAndDropItems - this.wronglyMappedDragAndDropItems;
    }

    /**
     * counts the variables for Short Answer Questions
     */
    private countShortAnswer() {
        const shortAnswer = this.question as ShortAnswerQuestion;
            this.shortAnswerSpotCount = shortAnswer.spots.length;
            this.shortAnswerCorrectAnswers = this.ShortAnswerText.filter(option => option.isCorrect).length;
            this.shortAnswerWrongAnswers = this.shortAnswerSpotCount - this.shortAnswerCorrectAnswers;
            this.differenceShortAnswer = this.shortAnswerCorrectAnswers - this.shortAnswerWrongAnswers;
    }

}
