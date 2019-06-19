import { Component, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { ModelingExercise } from '../entities/modeling-exercise';
import { Participation, ParticipationWebsocketService } from '../entities/participation';
import { ApollonDiagramService } from '../entities/apollon-diagram';
import { DiagramType, ElementType, Selection, UMLModel, UMLRelationshipType } from '@ls1intum/apollon';
import { JhiAlertService } from 'ng-jhipster';
import { Result, ResultService } from '../entities/result';
import { ModelingSubmission, ModelingSubmissionService } from '../entities/modeling-submission';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ComponentCanDeactivate } from '../shared';
import { JhiWebsocketService } from '../core';
import { Observable } from 'rxjs/Observable';
import { TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';
import { ModelingEditorComponent } from 'app/modeling-editor';
import { ModelingAssessmentService } from 'app/entities/modeling-assessment';
import { ComplaintService } from 'app/entities/complaint/complaint.service';
import { Feedback } from 'app/entities/feedback';

@Component({
    selector: 'jhi-modeling-submission',
    templateUrl: './modeling-submission.component.html',
    styleUrls: ['./modeling-submission.component.scss'],
})
// TODO CZ: move assessment stuff to separate assessment result view?
export class ModelingSubmissionComponent implements OnInit, OnDestroy, ComponentCanDeactivate {
    @ViewChild(ModelingEditorComponent, { static: false })
    modelingEditor: ModelingEditorComponent;

    private subscription: Subscription;
    private resultUpdateListener: Subscription;

    participation: Participation;
    modelingExercise: ModelingExercise;
    result: Result | null;

    selectedEntities: string[];
    selectedRelationships: string[];

    submission: ModelingSubmission;

    assessmentResult: Result | null;
    assessmentsNames: Map<string, Map<string, string>>;
    totalScore: number;
    generalFeedbackText: String | null;

    umlModel: UMLModel; // input model for Apollon
    hasElements = false; // indicates if the current model has at least one element
    isActive: boolean;
    isSaving: boolean;
    retryStarted = false;
    autoSaveInterval: number;
    autoSaveTimer: number;

    automaticSubmissionWebsocketChannel: string;

    showComplaintForm = false;
    // indicates if there is a complaint for the result of the submission
    hasComplaint: boolean;
    // the number of complaints that the student is still allowed to submit in the course. this is used for disabling the complain button.
    numberOfAllowedComplaints: number;
    // indicates if the result is older than one week. if it is, the complain button is disabled.
    isTimeOfComplaintValid: boolean;
    // indicates if the assessment due date is in the past. the assessment will not be loaded and displayed to the student if it is not.
    isAfterAssessmentDueDate: boolean;
    isLoading: boolean;

    constructor(
        private jhiWebsocketService: JhiWebsocketService,
        private apollonDiagramService: ApollonDiagramService,
        private modelingSubmissionService: ModelingSubmissionService,
        private modelingAssessmentService: ModelingAssessmentService,
        private complaintService: ComplaintService,
        private resultService: ResultService,
        private jhiAlertService: JhiAlertService,
        private route: ActivatedRoute,
        private modalService: NgbModal,
        private translateService: TranslateService,
        private router: Router,
        private participationWebsocketService: ParticipationWebsocketService,
    ) {
        this.isSaving = false;
        this.autoSaveTimer = 0;
        this.isLoading = true;
    }

    ngOnInit(): void {
        this.subscription = this.route.params.subscribe(params => {
            if (params['participationId']) {
                this.modelingSubmissionService.getDataForModelingEditor(params['participationId']).subscribe(
                    modelingSubmission => {
                        if (!modelingSubmission) {
                            this.jhiAlertService.error('artemisApp.apollonDiagram.submission.noSubmission');
                        }
                        // reconnect participation <--> result
                        if (modelingSubmission.result) {
                            modelingSubmission.participation.results = [modelingSubmission.result];
                        }
                        this.participation = modelingSubmission.participation;
                        this.modelingExercise = this.participation.exercise as ModelingExercise;
                        if (this.modelingExercise.course) {
                            this.complaintService.getNumberOfAllowedComplaintsInCourse(this.modelingExercise.course.id).subscribe((allowedComplaints: number) => {
                                this.numberOfAllowedComplaints = allowedComplaints;
                            });
                        }
                        if (this.modelingExercise.diagramType == null) {
                            this.modelingExercise.diagramType = DiagramType.ClassDiagram;
                        }
                        this.isActive = this.modelingExercise.dueDate == null || new Date() <= moment(this.modelingExercise.dueDate).toDate();
                        this.isAfterAssessmentDueDate = !this.modelingExercise.assessmentDueDate || moment().isAfter(this.modelingExercise.assessmentDueDate);
                        this.submission = modelingSubmission;
                        if (this.submission.model) {
                            this.umlModel = JSON.parse(this.submission.model);
                            this.hasElements = this.umlModel.elements && this.umlModel.elements.length !== 0;
                        }
                        this.subscribeToWebsockets();
                        if (this.submission.result && this.isAfterAssessmentDueDate) {
                            this.result = this.submission.result;
                        }
                        if (this.submission.submitted && this.result && this.result.completionDate) {
                            this.isTimeOfComplaintValid = this.resultService.isTimeOfComplaintValid(this.result, this.modelingExercise);
                            this.modelingAssessmentService.getAssessment(this.submission.id).subscribe((assessmentResult: Result) => {
                                this.assessmentResult = assessmentResult;
                                this.prepareAssessmentData();
                            });
                            this.complaintService.findByResultId(this.result.id).subscribe(res => {
                                this.hasComplaint = !!res.body;
                            });
                        }
                        this.setAutoSaveTimer();
                        this.isLoading = false;
                    },
                    error => {
                        if (error.status === 403) {
                            this.router.navigate(['accessdenied']);
                        }
                    },
                );
            }
        });
        window.scroll(0, 0);
    }

    /**
     * If the submission is submitted, subscribe to new results for the participation.
     * Otherwise, subscribe to the automatic submission (which happens when the submission is un-submitted and the exercise due date is over).
     */
    private subscribeToWebsockets(): void {
        if (this.submission && this.submission.id) {
            if (this.submission.submitted) {
                this.subscribeToNewResultsWebsocket();
            } else {
                this.subscribeToAutomaticSubmissionWebsocket();
            }
        }
    }

    /**
     * Subscribes to the websocket channel for automatic submissions. In the server the AutomaticSubmissionService regularly checks for unsubmitted submissions, if the
     * corresponding exercise has finished. If it has, the submission is automatically submitted and sent over this websocket channel. Here we listen to the channel and update the
     * view accordingly.
     */
    private subscribeToAutomaticSubmissionWebsocket(): void {
        if (!this.submission || !this.submission.id) {
            return;
        }
        this.automaticSubmissionWebsocketChannel = '/user/topic/modelingSubmission/' + this.submission.id;
        this.jhiWebsocketService.subscribe(this.automaticSubmissionWebsocketChannel);
        this.jhiWebsocketService.receive(this.automaticSubmissionWebsocketChannel).subscribe((submission: ModelingSubmission) => {
            if (submission.submitted) {
                this.submission = submission;
                if (this.submission.model) {
                    this.umlModel = JSON.parse(this.submission.model);
                    this.hasElements = this.umlModel.elements && this.umlModel.elements.length !== 0;
                }
                if (this.submission.result && this.submission.result.completionDate && this.isAfterAssessmentDueDate) {
                    this.modelingAssessmentService.getAssessment(this.submission.id).subscribe((assessmentResult: Result) => {
                        this.assessmentResult = assessmentResult;
                        this.prepareAssessmentData();
                    });
                }
                this.jhiAlertService.info('artemisApp.modelingEditor.autoSubmit');
                this.isActive = false;
            }
        });
    }

    /**
     * Subscribes to the websocket channel for new results. When an assessment is submitted the new result is sent over this websocket channel. Here we listen to the channel
     * and show the new assessment information to the student.
     */
    private subscribeToNewResultsWebsocket(): void {
        if (!this.participation || !this.participation.id) {
            return;
        }
        this.resultUpdateListener = this.participationWebsocketService.subscribeForLatestResultOfParticipation(this.participation.id).subscribe((newResult: Result) => {
            if (newResult && newResult.completionDate) {
                this.assessmentResult = newResult;
                this.assessmentResult = this.modelingAssessmentService.convertResult(newResult);
                this.prepareAssessmentData();
                this.isTimeOfComplaintValid = this.resultService.isTimeOfComplaintValid(this.assessmentResult, this.modelingExercise);
                this.jhiAlertService.info('artemisApp.modelingEditor.newAssessment');
            }
        });
    }

    /**
     * This function sets and starts an auto-save timer that automatically saves changes
     * to the model after at most 60 seconds.
     */
    private setAutoSaveTimer(): void {
        if (this.submission.submitted) {
            return;
        }
        this.autoSaveTimer = 0;
        // auto save of submission if there are changes
        this.autoSaveInterval = window.setInterval(() => {
            this.autoSaveTimer++;
            if (this.submission && this.submission.submitted) {
                clearInterval(this.autoSaveInterval);
                this.autoSaveTimer = 0;
            }
            if (this.autoSaveTimer >= 60 && !this.canDeactivate()) {
                this.saveDiagram();
            }
        }, 1000);
    }

    saveDiagram(): void {
        if (this.isSaving) {
            // don't execute the function if it is already currently executing
            return;
        }
        if (!this.submission) {
            this.submission = new ModelingSubmission();
        }
        this.submission.submitted = false;
        this.updateSubmissionModel();
        this.isSaving = true;
        this.autoSaveTimer = 0;

        if (this.submission.id) {
            this.modelingSubmissionService.update(this.submission, this.modelingExercise.id).subscribe(
                response => {
                    this.submission = response.body!;
                    this.result = this.submission.result;
                    this.isSaving = false;
                    this.jhiAlertService.success('artemisApp.modelingEditor.saveSuccessful');
                },
                error => {
                    this.isSaving = false;
                    this.jhiAlertService.error('artemisApp.modelingEditor.error');
                },
            );
        } else {
            this.modelingSubmissionService.create(this.submission, this.modelingExercise.id).subscribe(
                submission => {
                    this.submission = submission.body!;
                    this.result = this.submission.result;
                    this.isSaving = false;
                    this.jhiAlertService.success('artemisApp.modelingEditor.saveSuccessful');
                    this.isActive = this.modelingExercise.dueDate == null || new Date() <= moment(this.modelingExercise.dueDate).toDate();
                    this.subscribeToAutomaticSubmissionWebsocket();
                },
                error => {
                    this.jhiAlertService.error('artemisApp.modelingEditor.error');
                    this.isSaving = false;
                },
            );
        }
    }

    submit(): void {
        if (!this.submission) {
            return;
        }
        this.updateSubmissionModel();
        if (this.isModelEmpty(this.submission.model)) {
            this.jhiAlertService.warning('artemisApp.modelingEditor.empty');
            return;
        }

        let confirmSubmit = true;
        if (this.calculateNumberOfModelElements() < 10) {
            confirmSubmit = window.confirm('Are you sure you want to submit? You cannot edit your model anymore until you get an assessment!');
        }

        if (confirmSubmit) {
            this.submission.submitted = true;
            this.modelingSubmissionService.update(this.submission, this.modelingExercise.id).subscribe(
                response => {
                    this.submission = response.body!;
                    this.umlModel = JSON.parse(this.submission.model);
                    this.result = this.submission.result;
                    // Compass has already calculated a result
                    if (this.result && this.result.assessmentType && this.isAfterAssessmentDueDate) {
                        const participation = this.participation;
                        participation.results = [this.result];
                        this.participation = Object.assign({}, participation);
                        this.modelingAssessmentService.getAssessment(this.submission.id).subscribe((assessmentResult: Result) => {
                            this.assessmentResult = assessmentResult;
                            this.prepareAssessmentData();
                        });
                        this.jhiAlertService.success('artemisApp.modelingEditor.submitSuccessfulWithAssessment');
                    } else {
                        if (this.isActive) {
                            this.jhiAlertService.success('artemisApp.modelingEditor.submitSuccessful');
                        } else {
                            this.jhiAlertService.warning('artemisApp.modelingEditor.submitDeadlineMissed');
                        }
                    }
                    this.retryStarted = false;
                    this.subscribeToWebsockets();
                    if (this.automaticSubmissionWebsocketChannel) {
                        this.jhiWebsocketService.unsubscribe(this.automaticSubmissionWebsocketChannel);
                    }
                },
                err => {
                    this.jhiAlertService.error('artemisApp.modelingEditor.error');
                    this.submission.submitted = false;
                },
            );
        }
    }

    private isModelEmpty(model: string): boolean {
        const umlModel: UMLModel = JSON.parse(model);
        return !umlModel || !umlModel.elements || umlModel.elements.length === 0;
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
        clearInterval(this.autoSaveInterval);
        if (this.automaticSubmissionWebsocketChannel) {
            this.jhiWebsocketService.unsubscribe(this.automaticSubmissionWebsocketChannel);
        }
        if (this.resultUpdateListener) {
            this.resultUpdateListener.unsubscribe();
        }
    }

    /**
     * Updates the model of the submission with the current Apollon model state
     */
    updateSubmissionModel(): void {
        if (!this.modelingEditor || !this.modelingEditor.getCurrentModel()) {
            return;
        }
        const umlModel = this.modelingEditor.getCurrentModel();
        this.hasElements = umlModel.elements && umlModel.elements.length !== 0;
        const diagramJson = JSON.stringify(umlModel);
        if (this.submission && diagramJson) {
            this.submission.model = diagramJson;
        }
    }

    /**
     * Prepare assessment data for displaying the assessment information to the student.
     */
    private prepareAssessmentData(): void {
        this.filterGeneralFeedback();
        this.initializeAssessmentInfo();
    }

    /**
     * Gets the text of the general feedback, if there is one, and removes it from the original feedback list that is displayed in the assessment list.
     */
    private filterGeneralFeedback(): void {
        if (this.assessmentResult && this.assessmentResult.feedbacks && this.submission && this.submission.model) {
            const feedback = this.assessmentResult.feedbacks;
            const generalFeedbackIndex = feedback.findIndex(feedbackElement => feedbackElement.reference == null);
            if (generalFeedbackIndex >= 0) {
                this.generalFeedbackText = feedback[generalFeedbackIndex].detailText;
                feedback.splice(generalFeedbackIndex, 1);
            }
        }
    }

    /**
     * Retrieves names for displaying the assessment and calculates the total score
     */
    private initializeAssessmentInfo(): void {
        if (this.assessmentResult && this.assessmentResult.feedbacks && this.submission && this.submission.model) {
            this.assessmentsNames = this.modelingAssessmentService.getNamesForAssessments(this.assessmentResult, this.umlModel);
            let totalScore = 0;
            for (const feedback of this.assessmentResult.feedbacks) {
                totalScore += feedback.credits;
            }
            this.totalScore = totalScore;
        }
    }

    /**
     * Handles changes of the model element selection in Apollon. This is used for displaying
     * only the feedback of the selected model elements.
     * @param selection the new selection
     */
    onSelectionChanged(selection: Selection) {
        this.selectedEntities = selection.elements;
        for (const selectedEntity of this.selectedEntities) {
            this.selectedEntities.push(...this.getSelectedChildren(selectedEntity));
        }
        this.selectedRelationships = selection.relationships;
    }

    /**
     * Returns the elementIds of all the children of the element with the given elementId
     * or an empty list, if no children exist for this element.
     */
    private getSelectedChildren(elementId: string): string[] {
        if (!this.umlModel || !this.umlModel.elements) {
            return [];
        }
        return this.umlModel.elements.filter(element => element.owner === elementId).map(element => element.id);
    }

    /**
     * Checks whether a model element in the modeling editor is selected.
     */
    isSelected(feedback: Feedback): boolean {
        if ((!this.selectedEntities || this.selectedEntities.length === 0) && (!this.selectedRelationships || this.selectedRelationships.length === 0)) {
            return true;
        }
        if (feedback.referenceType! in UMLRelationshipType) {
            return this.selectedRelationships.indexOf(feedback.referenceId!) > -1;
        } else {
            return this.selectedEntities.indexOf(feedback.referenceId!) > -1;
        }
    }

    // function to check whether there are pending changes
    canDeactivate(): Observable<boolean> | boolean {
        if (this.submission && this.submission.submitted) {
            return true;
        }
        const model: UMLModel = this.modelingEditor.getCurrentModel();
        const jsonModel = JSON.stringify(model);
        if (
            ((!this.submission || !this.submission.model) && model.elements.length > 0 && jsonModel !== '') ||
            (this.submission && this.submission.model && JSON.parse(this.submission.model).version === model.version && this.submission.model !== jsonModel)
        ) {
            return false;
        }
        return true;
    }

    // displays the alert for confirming leaving the page if there are unsaved changes
    @HostListener('window:beforeunload', ['$event'])
    unloadNotification($event: any): void {
        if (!this.canDeactivate()) {
            $event.returnValue = this.translateService.instant('pendingChanges');
        }
    }

    /**
     * starts a retry and resets necessary attributes
     * the retry is only persisted after saving or submitting the model
     */
    retry(): void {
        this.retryStarted = true;
        this.umlModel.assessments = [];
        this.submission = new ModelingSubmission();
        this.assessmentResult = null;
        this.result = null; // TODO: think about how we could visualize old results and assessments after retry
        clearInterval(this.autoSaveInterval);
        this.setAutoSaveTimer();
    }

    /**
     * counts the number of model elements
     * is used in the submit() function
     */
    calculateNumberOfModelElements(): number {
        if (this.submission && this.submission.model) {
            const umlModel = JSON.parse(this.submission.model);
            return umlModel.elements.length + umlModel.relationships.length;
        }
        return 0;
    }
}
