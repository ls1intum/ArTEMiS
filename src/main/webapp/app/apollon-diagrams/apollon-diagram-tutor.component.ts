import { Component, ElementRef, EventEmitter, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { JhiAlertService } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ApollonEditor, ApollonMode, Assessment, DiagramType, UMLModel } from '@ls1intum/apollon';
import { ActivatedRoute, Router } from '@angular/router';
import { ModelingSubmission, ModelingSubmissionService } from '../entities/modeling-submission';
import { ModelingExercise, ModelingExerciseService } from '../entities/modeling-exercise';
import { Result, ResultService } from '../entities/result';
import { ModelingAssessmentService } from '../entities/modeling-assessment';
import { AccountService } from '../core';
import { Submission } from '../entities/submission';
import { HttpErrorResponse } from '@angular/common/http';
import { Conflict } from 'app/entities/modeling-assessment/conflict.model';
import { Feedback } from 'app/entities/feedback';
import { ModelElementType } from 'app/entities/modeling-assessment/uml-element.model';

@Component({
    selector: 'jhi-apollon-diagram-tutor',
    templateUrl: './apollon-diagram-tutor.component.html',
    styleUrls: ['./apollon-diagram-tutor.component.scss'],
    providers: [ModelingAssessmentService]
})
export class ApollonDiagramTutorComponent implements OnInit, OnDestroy {
    @ViewChild('editorContainer')
    editorContainer: ElementRef;
    @Output()
    onNewResult = new EventEmitter<Result>();

    apollonEditor: ApollonEditor | null = null;

    submission: ModelingSubmission;
    modelingExercise: ModelingExercise;
    result: Result;
    conflicts: Map<string, Conflict>;
    elementFeedback: Map<string, Feedback> = new Map();
    assessmentsAreValid = false;
    invalidError = '';
    totalScore = 0;
    busy: boolean;
    done = true;
    timeout: any;
    userId: number;
    isAuthorized: boolean;
    ignoreConflicts: false;

    constructor(
        private jhiAlertService: JhiAlertService,
        private modalService: NgbModal,
        private router: Router,
        private route: ActivatedRoute,
        private modelingSubmissionService: ModelingSubmissionService,
        private modelingExerciseService: ModelingExerciseService,
        private resultService: ResultService,
        private modelingAssessmentService: ModelingAssessmentService,
        private accountService: AccountService
    ) {}

    ngOnInit() {
        // Used to check if the assessor is the current user
        this.accountService.identity().then(user => {
            this.userId = user.id;
        });
        this.isAuthorized = this.accountService.hasAnyAuthorityDirect(['ROLE_ADMIN', 'ROLE_INSTRUCTOR']);
        this.route.params.subscribe(params => {
            const submissionId = Number(params['submissionId']);
            let nextOptimal: boolean;
            this.route.queryParams.subscribe(query => {
                nextOptimal = query['optimal'] === 'true'; // TODO CZ: do we need this flag?
            });

            this.modelingSubmissionService.getSubmission(submissionId).subscribe(res => {
                this.submission = res;
                this.modelingExercise = this.submission.participation.exercise as ModelingExercise;
                this.result = this.submission.result;
                if (this.result.feedbacks) {
                    this.result = this.modelingAssessmentService.convertResult(this.result);
                } else {
                    this.result.feedbacks = [];
                }
                this.updateElementFeedbackMapping(this.result.feedbacks);
                this.submission.participation.results = [this.result];
                this.result.participation = this.submission.participation;
                /**
                 * set diagramType to class diagram if exercise is null, use case or communication
                 * apollon does not support use case and communication yet
                 */
                if (
                    this.modelingExercise.diagramType === null ||
                    this.modelingExercise.diagramType === DiagramType.UseCaseDiagram ||
                    this.modelingExercise.diagramType === DiagramType.ObjectDiagram
                ) {
                    this.modelingExercise.diagramType = DiagramType.ClassDiagram;
                }
                if (this.submission.model) {
                    this.initializeApollonEditor(JSON.parse(this.submission.model));
                } else {
                    this.jhiAlertService.error(`No model could be found for this submission.`);
                }
                if ((this.result.assessor == null || this.result.assessor.id === this.userId) && !this.result.rated) {
                    this.jhiAlertService.info('arTeMiSApp.apollonDiagram.lock');
                }
                if (nextOptimal) {
                    this.modelingAssessmentService.getPartialAssessment(submissionId).subscribe((result: Result) => {
                        this.result = result;
                    });
                }
                if (this.result) {
                    this.calculateTotalScore();
                }
            });
        });
    }

    ngOnDestroy() {
        clearTimeout(this.timeout);
        if (this.apollonEditor !== null) {
            this.apollonEditor.destroy();
        }
    }

    /**
     * Updates the mapping of elementIds to Feedback elements. This should be called after getting the
     * (updated) Feedback list from the server.
     *
     * @param feedbacks new Feedback elements to insert
     */
    private updateElementFeedbackMapping(feedbacks: Feedback[]) {
        if (!feedbacks) {
            return;
        }
        for (const feedback of feedbacks) {
            this.elementFeedback.set(feedback.referenceId, feedback);
        }
    }

    /**
     * Initializes the Apollon editor with the Feedback List in Assessment mode.
     * The Feedback elements are converted to Assessment objects needed by Apollon before they are added to
     * the initial model which is then passed to Apollon.
     */
    private initializeApollonEditor(initialModel: UMLModel) {
        if (this.apollonEditor !== null) {
            this.apollonEditor.destroy();
        }

        const feedbacks: Feedback[] = this.result.feedbacks;
        const assessments: { [id: string]: Assessment } = feedbacks.reduce((acc, feedback) => ({
            ...acc,
            [feedback.referenceId]: {
                score: feedback.credits,
                feedback: feedback.text,
            }
        }), {});
        initialModel.assessments = assessments;

        this.apollonEditor = new ApollonEditor(this.editorContainer.nativeElement, {
            mode: ApollonMode.Assessment,
            readonly: false,
            model: initialModel,
            type: this.modelingExercise.diagramType
        });
    }

    saveAssessment() {
        this.calculateTotalScore();
        this.removeCircularDependencies();
        this.result.feedbacks = this.generateFeedbackFromAssessment();
        this.modelingAssessmentService.save(this.result.feedbacks, this.submission.id).subscribe(
            (result: Result) => {
                this.result = result;
                this.updateElementFeedbackMapping(result.feedbacks);
                this.onNewResult.emit(this.result);
                this.jhiAlertService.success('arTeMiSApp.apollonDiagram.assessment.saveSuccessful');
            },
            () => {
                this.jhiAlertService.error('arTeMiSApp.apollonDiagram.assessment.saveFailed');
            }
        );
    }

    submit() {
        this.calculateTotalScore();
        this.removeCircularDependencies();
        this.result.feedbacks = this.generateFeedbackFromAssessment();
        this.modelingAssessmentService.save(this.result.feedbacks, this.submission.id, true, this.ignoreConflicts).subscribe(
            (result: Result) => {
                result.participation.results = [result];
                this.result = result;
                this.updateElementFeedbackMapping(result.feedbacks);
                this.jhiAlertService.success('arTeMiSApp.apollonDiagram.assessment.submitSuccessful');
                this.conflicts = undefined;
                this.done = false;
            },
            (error: HttpErrorResponse) => {
                if (error.status === 409) {
                    this.conflicts = new Map();
                    (error.error as Conflict[]).forEach(conflict => this.conflicts.set(conflict.elementInConflict.id, conflict));
                    this.highlightElementsWithConflict();
                    this.jhiAlertService.error('arTeMiSApp.apollonDiagram.assessment.submitFailedWithConflict');
                } else {
                    this.jhiAlertService.error('arTeMiSApp.apollonDiagram.assessment.submitFailed');
                }
            }
        );
    }

    /**
     * Calculates the total score of the current assessment.
     * Returns an error if the total score cannot be calculated
     * because a score is not a number/empty.
     * This function originally checked whether the total score is negative
     * or greater than the max. score, but we decided to remove the restriction
     * and instead set the score boundaries on the server.
     */
    private calculateTotalScore() {
        if (!this.result.feedbacks || this.result.feedbacks.length === 0) {
            this.totalScore = 0;
        }
        let totalScore = 0;
        for (const feedback of this.result.feedbacks) {
            totalScore += feedback.credits;
            // TODO: due to the JS rounding problems, it might be the case that we get something like 16.999999999999993 here, so we better round this number
            if (feedback.credits == null) {
                this.assessmentsAreValid = false;
                return (this.invalidError = 'The score field must be a number and can not be empty!');
            }
        }
        this.totalScore = totalScore;
        this.assessmentsAreValid = true;
        this.invalidError = '';
    }

    /**
     * Removes the circular dependencies in the nested objects.
     * Otherwise, we would get a JSON error when trying to send the submission to the server.
     */
    private removeCircularDependencies() {
        this.submission.result.participation = null;
        this.submission.result.submission = null;
    }

    /**
     * Gets the assessments from Apollon and creates/updates the corresponding Feedback entries in the
     * element feedback mapping.
     * Returns an array containing all feedback entries from the mapping.
     */
    private generateFeedbackFromAssessment(): Feedback[] {
        for (const elementId in this.apollonEditor.model.assessments) {
            const assessment: Assessment = this.apollonEditor.model.assessments[elementId];
            const existingFeedback: Feedback = this.elementFeedback.get(elementId);
            if (existingFeedback) {
                existingFeedback.credits = assessment.score;
                existingFeedback.text = assessment.feedback;
            } else {
                // TODO CZ: replace UNKNOWN element type
                this.elementFeedback.set(elementId,
                    new Feedback(elementId, ModelElementType.UNKNOWN, assessment.score, assessment.feedback));
            }
        }
        return [...this.elementFeedback.values()];
    }

    assessNextOptimal(attempts: number) {
        if (attempts > 4) {
            this.busy = false;
            this.done = true;
            this.jhiAlertService.info('assessmentDashboard.noSubmissionFound');
            return;
        }
        this.busy = true;
        this.timeout = setTimeout(
            () => {
                this.modelingAssessmentService.getOptimalSubmissions(this.modelingExercise.id).subscribe(optimal => {
                    const nextOptimalSubmissionIds = optimal.body.map((submission: Submission) => submission.id);
                    if (nextOptimalSubmissionIds.length === 0) {
                        this.assessNextOptimal(attempts + 1);
                    } else {
                        // TODO: Workaround We have to fake path change to make angular reload the component
                        const addition = this.router.url.includes('apollon-diagrams2') ? '' : '2';
                        this.router.navigateByUrl(
                            `/apollon-diagrams${addition}/exercise/${this.modelingExercise.id}/${nextOptimalSubmissionIds.pop()}/tutor`
                        );
                    }
                });
            },
            attempts === 0 ? 0 : 500 + (attempts - 1) * 1000
        );
    }

    previousState() {
        this.router.navigate(['course', this.modelingExercise.course.id, 'exercise', this.modelingExercise.id, 'assessment']);
    }

    private highlightElementsWithConflict() {
        const model = this.apollonEditor.model;
        const entitiesToHighlight: string[] = Object.keys(model.elements).filter((id: string) => {
            if (this.conflicts.has(id)) {
                return true;
            }
            return false;
        });
        const relationshipsToHighlight: string[] = Object.keys(model.relationships).filter(id => this.conflicts.has(id));

        entitiesToHighlight.forEach(id => {
            document.getElementById(id).style.fill = 'rgb(248, 214, 217)';
        });

        // TODO MJ highlight relation entities. currently do not have unique id
        // relationshipsToHighlight.forEach(id => {
        //     document.getElementById(id).style.color = 'rgb(248, 214, 217)';
        // })
    }
}
