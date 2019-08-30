import { Component, EventEmitter, Input, OnInit, Output, ViewChild, ChangeDetectorRef } from '@angular/core';
import { MultipleChoiceQuestion } from 'app/entities/multiple-choice-question';
import { AnswerOption } from 'app/entities/answer-option';
import { ArtemisMarkdown } from 'app/components/util/markdown.service';
import { MarkdownEditorComponent } from 'app/markdown-editor';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CorrectOptionCommand, ExplanationCommand, IncorrectOptionCommand, DomainCommand, HintCommand } from 'app/markdown-editor/domainCommands';
import { EditQuizQuestion } from 'app/quiz/edit/edit-quiz-question.interface';

@Component({
    selector: 'jhi-edit-multiple-choice-question',
    templateUrl: './edit-multiple-choice-question.component.html',
    styleUrls: ['../../../quiz.scss'],
    providers: [ArtemisMarkdown],
})
export class EditMultipleChoiceQuestionComponent implements OnInit, EditQuizQuestion {
    @ViewChild('markdownEditor', { static: false })
    private markdownEditor: MarkdownEditorComponent;

    @Input()
    question: MultipleChoiceQuestion;
    @Input()
    questionIndex: number;

    @Output()
    questionUpdated = new EventEmitter();
    @Output()
    questionDeleted = new EventEmitter();

    /** Ace Editor configuration constants **/
    questionEditorText = '';

    /** Status boolean for collapse status **/
    isQuestionCollapsed: boolean;

    /** Set default preview of the markdown editor as preview for the multiple choice question **/
    get showPreview(): boolean {
        return this.markdownEditor && this.markdownEditor.previewMode;
    }
    showMultipleChoiceQuestionPreview = true;

    hintCommand = new HintCommand();
    correctCommand = new CorrectOptionCommand();
    incorrectCommand = new IncorrectOptionCommand();
    explanationCommand = new ExplanationCommand();

    /** DomainCommands for the multiple choice question **/
    commandMultipleChoiceQuestions: DomainCommand[] = [this.correctCommand, this.incorrectCommand, this.explanationCommand, this.hintCommand];

    constructor(private artemisMarkdown: ArtemisMarkdown, private modalService: NgbModal, private changeDetector: ChangeDetectorRef) {}

    ngOnInit(): void {
        this.questionEditorText = this.generateMarkdown();
    }

    /**
     * @function generateMarkdown
     * @desc Generate the markdown text for this question
     * 1. First the question text, hint, and explanation are added using ArtemisMarkdown
     * 2. After an empty line, the answer options are added
     * 3. For each answer option: text, hint and explanation are added using ArtemisMarkdown
     */
    generateMarkdown(): string {
        const markdownText =
            this.artemisMarkdown.generateTextHintExplanation(this.question) +
            '\n\n' +
            this.question
                .answerOptions!.map(answerOption => (answerOption.isCorrect ? '[correct]' : '[wrong]') + ' ' + this.artemisMarkdown.generateTextHintExplanation(answerOption))
                .join('\n');
        return markdownText;
    }

    /**
     * @function open
     * @desc open the modal for the help dialog
     * @param content
     */
    open(content: any) {
        this.modalService.open(content, { size: 'lg' });
    }

    /**
     * @function prepareForSave
     * @desc 1. Triggers the saving process by cleaning up the question and calling the markdown parse function
     *       to get the newest values in the editor to update the question attributes
     *       2. Notify parent component about changes to check the validity of new values of the question attributes
     */
    prepareForSave(): void {
        this.cleanupQuestion();
        this.markdownEditor.parse();
        this.questionUpdated.emit();
    }

    /**
     * @function cleanupQuestion
     * @desc Clear the question to avoid double assignments of one attribute
     */
    private cleanupQuestion() {
        // Reset Question Object
        this.question.answerOptions = [];
        this.question.text = null;
        this.question.explanation = null;
        this.question.hint = null;
        this.question.hasCorrectOption = null;
    }

    /**
     * @function domainCommandsFound
     * @desc 1. Gets a tuple of text and domainCommandIdentifiers and assigns text values according to the domainCommandIdentifiers a
     *          multiple choice question the to the multiple choice question attributes.
     *          (question text, explanation, hint, answerOption (correct/wrong)
     *       2. The tupple order is the same as the order of the commands in the markdown text inserted by the user
     *       3. resetMultipleChoicePreview() is triggered to notify the parent component
     *       about the changes within the question and to cacheValidation() since the assigned values have changed
     * @param domainCommands containing tuples of [text, domainCommandIdentifiers]
     */
    domainCommandsFound(domainCommands: [string, DomainCommand][]): void {
        this.cleanupQuestion();
        let currentAnswerOption;

        for (const [text, command] of domainCommands) {
            if (command === null && text.length > 0) {
                this.question.text = text;
            }
            if (command instanceof CorrectOptionCommand || command instanceof IncorrectOptionCommand) {
                currentAnswerOption = new AnswerOption();
                if (command instanceof CorrectOptionCommand) {
                    currentAnswerOption.isCorrect = true;
                } else {
                    currentAnswerOption.isCorrect = false;
                }
                currentAnswerOption.text = text;
                this.question.answerOptions!.push(currentAnswerOption);
            } else if (command instanceof ExplanationCommand) {
                if (currentAnswerOption) {
                    currentAnswerOption.explanation = text;
                } else {
                    this.question.explanation = text;
                }
            } else if (command instanceof HintCommand) {
                if (currentAnswerOption) {
                    currentAnswerOption.hint = text;
                } else {
                    this.question.hint = text;
                }
            }
        }
        this.resetMultipleChoicePreview();
    }

    /**
     * @function resetMultipleChoicePreview
     * @desc  Reset the preview function of the multiple choice question in order to cause a change
     *        so the parent component is notified
     *        and the check for the question validity is triggered
     */
    private resetMultipleChoicePreview() {
        this.showMultipleChoiceQuestionPreview = false;
        this.changeDetector.detectChanges();
        this.showMultipleChoiceQuestionPreview = true;
        this.changeDetector.detectChanges();
    }

    /**
     * @function deleteQuestion
     * @desc Delete this question from the quiz
     */
    deleteQuestion(): void {
        this.questionDeleted.emit();
    }
}
