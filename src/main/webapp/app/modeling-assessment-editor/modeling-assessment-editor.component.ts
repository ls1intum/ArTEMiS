import { Component, OnDestroy, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { JhiAlertService } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DiagramType, UMLModel } from '@ls1intum/apollon';
import { ActivatedRoute, Router } from '@angular/router';
import { ModelingSubmission, ModelingSubmissionService } from '../entities/modeling-submission';
import { ModelingExercise, ModelingExerciseService } from '../entities/modeling-exercise';
import { Result, ResultService } from '../entities/result';
import { AccountService } from 'app/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Conflict, ConflictingResult } from 'app/modeling-assessment-editor/conflict.model';
import { ModelingAssessmentService } from 'app/modeling-assessment-editor/modeling-assessment.service';
import { Feedback } from 'app/entities/feedback';

@Component({
    selector: 'jhi-modeling-assessment-editor',
    templateUrl: './modeling-assessment-editor.component.html',
    styleUrls: ['./modeling-assessment-editor.component.scss'],
})
export class ModelingAssessmentEditorComponent implements OnInit, OnDestroy {
    submission: ModelingSubmission;
    model: UMLModel;
    modelingExercise: ModelingExercise;
    result: Result;
    localFeedbacks: Feedback[];
    conflicts: Conflict[];
    highlightedElementIds: Set<string>;
    ignoreConflicts = false;

    assessmentsAreValid = false;
    busy: boolean;
    userId: number;
    isAuthorized = false;
    isAtLeastInstructor = false;
    showBackButton: boolean;

    constructor(
        private jhiAlertService: JhiAlertService,
        private modalService: NgbModal,
        private router: Router,
        private route: ActivatedRoute,
        private modelingSubmissionService: ModelingSubmissionService,
        private modelingExerciseService: ModelingExerciseService,
        private resultService: ResultService,
        private modelingAssessmentService: ModelingAssessmentService,
        private accountService: AccountService,
        private location: Location,
    ) {}

    ngOnInit() {
        // Used to check if the assessor is the current user
        this.accountService.identity().then(user => {
            this.userId = user.id;
        });
        this.isAtLeastInstructor = this.accountService.hasAnyAuthorityDirect(['ROLE_ADMIN', 'ROLE_INSTRUCTOR']);

        this.route.params.subscribe(params => {
            const submissionId: String = params['submissionId'];
            const exerciseId = Number(params['exerciseId']);
            if (submissionId === 'new') {
                this.loadOptimalSubmission(exerciseId);
            } else {
                this.loadSubmission(Number(submissionId));
            }
        });
        this.showBackButton = !!this.route.snapshot.queryParamMap.get('showBackButton');
    }

    checkAuthorization() {
        this.isAuthorized = this.result && this.result.assessor && this.result.assessor.id === this.userId;
    }

    ngOnDestroy() {}

    loadSubmission(submissionId: number): void {
        this.modelingSubmissionService.getSubmission(submissionId).subscribe(
            (submission: ModelingSubmission) => {
                this.handleReceivedSubmission(submission);
            },
            error => {
                this.onError();
            },
        );
    }

    loadOptimalSubmission(exerciseId: number): void {
        this.modelingSubmissionService.getModelingSubmissionForExerciseWithoutAssessment(exerciseId, true).subscribe(
            (submission: ModelingSubmission) => {
                this.handleReceivedSubmission(submission);

                // Update the url with the new id, without reloading the page, to make the history consistent
                const newUrl = window.location.hash.replace('#', '').replace('new', `${this.submission.id}`);
                this.location.go(newUrl);
            },
            (error: HttpErrorResponse) => {
                if (error.status === 404) {
                    // there is no submission waiting for assessment at the moment
                    this.goToExerciseDashboard();
                    this.jhiAlertService.info('arTeMiSApp.tutorExerciseDashboard.noSubmissions');
                } else {
                    this.onError();
                }
            },
        );
    }

    handleReceivedSubmission(submission: ModelingSubmission): void {
        this.submission = submission;
        this.modelingExercise = this.submission.participation.exercise as ModelingExercise;
        this.result = this.submission.result;
        this.localFeedbacks = this.result.feedbacks;
        if (this.result.feedbacks) {
            this.result = this.modelingAssessmentService.convertResult(this.result);
        } else {
            this.result.feedbacks = [];
        }
        this.submission.participation.results = [this.result];
        this.result.participation = this.submission.participation;
        if (this.modelingExercise.diagramType == null) {
            this.modelingExercise.diagramType = DiagramType.ClassDiagram;
        }
        if (this.submission.model) {
            this.model = JSON.parse(this.submission.model);
        } else {
            this.jhiAlertService.clear();
            this.jhiAlertService.error('modelingAssessmentEditor.messages.noModel');
        }
        if ((this.result.assessor == null || this.result.assessor.id === this.userId) && !this.result.rated) {
            this.jhiAlertService.clear();
            this.jhiAlertService.info('modelingAssessmentEditor.messages.lock');
        }
        this.checkAuthorization();
        this.validateFeedback();
    }

    onError(): void {
        this.submission = undefined;
        this.modelingExercise = undefined;
        this.result = undefined;
        this.model = undefined;
        this.jhiAlertService.clear();
        this.jhiAlertService.error('modelingAssessmentEditor.messages.loadSubmissionFailed');
    }

    onSaveAssessment() {
        this.removeCircularDependencies();
        if (this.localFeedbacks === undefined || this.localFeedbacks === null) {
            this.localFeedbacks = [];
        }
        this.modelingAssessmentService.saveAssessment(this.localFeedbacks, this.submission.id).subscribe(
            (result: Result) => {
                this.result = result;
                this.jhiAlertService.clear();
                this.jhiAlertService.success('modelingAssessmentEditor.messages.saveSuccessful');
            },
            () => {
                this.jhiAlertService.clear();
                this.jhiAlertService.error('modelingAssessmentEditor.messages.saveFailed');
            },
        );
    }

    onSubmitAssessment() {
        this.removeCircularDependencies();
        if (this.localFeedbacks === undefined || this.localFeedbacks === null) {
            this.localFeedbacks = [];
        }
        // TODO: we should warn the tutor if not all model elements have been assessed, and ask him to confirm that he really wants to submit the assessment
        // in case he says no, we should potentially highlight the elements that are not yet assessed
        this.modelingAssessmentService.saveAssessment(this.localFeedbacks, this.submission.id, true, this.ignoreConflicts).subscribe(
            (result: Result) => {
                result.participation.results = [result];
                this.result = result;
                this.jhiAlertService.clear();
                this.jhiAlertService.success('modelingAssessmentEditor.messages.submitSuccessful');
                this.conflicts = undefined;
                this.ignoreConflicts = false;
            },
            (error: HttpErrorResponse) => {
                if (error.status === 409) {
                    this.conflicts = error.error as Conflict[];
                    this.conflicts.forEach((conflict: Conflict) => {
                        this.modelingAssessmentService.convertResult(conflict.result);
                        conflict.conflictingResults.forEach((conflictingResult: ConflictingResult) => this.modelingAssessmentService.convertResult(conflictingResult.result));
                    });
                    this.highlightConflictingElements();
                    this.jhiAlertService.clear();
                    this.jhiAlertService.error('modelingAssessmentEditor.messages.submitFailedWithConflict');
                } else {
                    this.jhiAlertService.clear();
                    this.jhiAlertService.error('modelingAssessmentEditor.messages.submitFailed');
                }
            },
        );
    }

    onShowConflictResolution() {
        this.modelingAssessmentService.addLocalConflicts(this.submission.id, this.conflicts);
        this.jhiAlertService.clear();
        this.router.navigate(['modeling-exercise', this.modelingExercise.id, 'submissions', this.submission.id, 'assessment', 'conflict']);
    }

    onFeedbackChanged(feedbacks: Feedback[]) {
        this.localFeedbacks = feedbacks;
        this.validateFeedback();
    }

    assessNextOptimal() {
        this.busy = true;
        this.modelingAssessmentService.getOptimalSubmissions(this.modelingExercise.id).subscribe(
            (optimal: number[]) => {
                this.busy = false;
                if (optimal.length === 0) {
                    this.jhiAlertService.clear();
                    this.jhiAlertService.info('assessmentDashboard.noSubmissionFound');
                } else {
                    this.jhiAlertService.clear();
                    this.router.onSameUrlNavigation = 'reload';
                    // navigate to root and then to new assessment page to trigger re-initialization of the components
                    this.router
                        .navigateByUrl('/', { skipLocationChange: true })
                        .then(() => this.router.navigateByUrl(`modeling-exercise/${this.modelingExercise.id}/submissions/${optimal.pop()}/assessment?showBackButton=true`));
                }
            },
            () => {
                this.busy = false;
                this.jhiAlertService.clear();
                this.jhiAlertService.info('assessmentDashboard.noSubmissionFound');
            },
        );
    }

    private highlightConflictingElements() {
        this.highlightedElementIds = new Set<string>();
        this.conflicts.forEach((conflict: Conflict) => {
            this.highlightedElementIds.add(conflict.modelElementId);
        });
    }

    /**
     * Removes the circular dependencies in the nested objects.
     * Otherwise, we would get a JSON error when trying to send the submission to the server.
     */
    private removeCircularDependencies() {
        this.submission.result.participation = null;
        this.submission.result.submission = null;
    }

    private validateFeedback() {
        if (!this.result.feedbacks) {
            this.assessmentsAreValid = false;
            return;
        }
        for (const feedback of this.result.feedbacks) {
            if (feedback.credits == null) {
                this.assessmentsAreValid = false;
                return;
            }
        }
        this.assessmentsAreValid = true;
    }

    goToExerciseDashboard() {
        if (this.modelingExercise && this.modelingExercise.course) {
            this.router.navigateByUrl(`/course/${this.modelingExercise.course.id}/exercise/${this.modelingExercise.id}/tutor-dashboard`);
        } else {
            this.location.back();
        }
    }
}
