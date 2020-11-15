import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router, NavigationExtras } from '@angular/router';
import { JhiAlertService } from 'ng-jhipster';
import * as moment from 'moment';

import { AccountService } from 'app/core/auth/account.service';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { TextSubmission } from 'app/entities/text-submission.model';
import { TextExercise } from 'app/entities/text-exercise.model';
import { Result } from 'app/entities/result.model';
import { Complaint } from 'app/entities/complaint.model';
import { ComplaintResponse } from 'app/entities/complaint-response.model';
import { ComplaintService } from 'app/complaints/complaint.service';
import { TextAssessmentsService } from 'app/exercises/text/assess/text-assessments.service';
import { TextBlockRef } from 'app/entities/text-block-ref.model';
import { Feedback, FeedbackType } from 'app/entities/feedback.model';
import { notUndefined } from 'app/shared/util/global.utils';
import { TextBlock, TextBlockType } from 'app/entities/text-block.model';
import { TranslateService } from '@ngx-translate/core';
import { NEW_ASSESSMENT_PATH } from 'app/exercises/text/assess-new/text-submission-assessment.route';
import { StructuredGradingCriterionService } from 'app/exercises/shared/structured-grading-criterion/structured-grading-criterion.service';
import { assessmentNavigateBack } from 'app/exercises/shared/navigate-back.util';
import { TextAssessmentBaseComponent } from 'app/exercises/text/assess-new/text-assessment-base.component';
import { now } from 'moment';

@Component({
    selector: 'jhi-text-submission-assessment',
    templateUrl: './text-submission-assessment.component.html',
    styleUrls: ['./text-submission-assessment.component.scss'],
})
export class TextSubmissionAssessmentComponent extends TextAssessmentBaseComponent implements OnInit {
    /*
     * The instance of this component is REUSED for multiple assessments if using the "Assess Next" button!
     * All properties must be initialized with a default value (or null) in the resetComponent() method.
     * For traceability: Keep order in resetComponent() consistent with declaration.
     */

    participation?: StudentParticipation;
    submission?: TextSubmission;
    result?: Result;
    generalFeedback: Feedback;
    unreferencedFeedback: Feedback[];
    textBlockRefs: TextBlockRef[];
    unusedTextBlockRefs: TextBlockRef[];
    complaint?: Complaint;
    totalScore: number;
    isTestRun = false;
    isLoading: boolean;
    saveBusy: boolean;
    submitBusy: boolean;
    cancelBusy: boolean;
    nextSubmissionBusy: boolean;
    isAssessor: boolean;
    isAtLeastInstructor: boolean;
    assessmentsAreValid: boolean;
    noNewSubmissions: boolean;
    hasAssessmentDueDatePassed: boolean;

    /*
     * Non-resetted properties:
     * These properties are not resetted on purpose, as they cannot change between assessments.
     */
    private cancelConfirmationText: string;
    // ExerciseId is updated from Route Subscription directly.
    exerciseId: number;

    private get referencedFeedback(): Feedback[] {
        return this.textBlockRefs.map(({ feedback }) => feedback).filter(notUndefined) as Feedback[];
    }

    private get assessments(): Feedback[] {
        if (Feedback.hasDetailText(this.generalFeedback)) {
            return [this.generalFeedback, ...this.referencedFeedback, ...this.unreferencedFeedback];
        } else {
            return [...this.referencedFeedback, ...this.unreferencedFeedback];
        }
    }

    private get textBlocksWithFeedback(): TextBlock[] {
        return [...this.textBlockRefs, ...this.unusedTextBlockRefs]
            .filter(({ block, feedback }) => block?.type === TextBlockType.AUTOMATIC || !!feedback)
            .map(({ block }) => block!);
    }

    constructor(
        private activatedRoute: ActivatedRoute,
        private router: Router,
        private location: Location,
        private route: ActivatedRoute,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected assessmentsService: TextAssessmentsService,
        private complaintService: ComplaintService,
        translateService: TranslateService,
        protected structuredGradingCriterionService: StructuredGradingCriterionService,
    ) {
        super(jhiAlertService, accountService, assessmentsService, translateService, structuredGradingCriterionService);
        translateService.get('artemisApp.textAssessment.confirmCancel').subscribe((text) => (this.cancelConfirmationText = text));
        this.resetComponent();
    }

    /**
     * This method is called before the component is REUSED!
     * All properties MUST be set to a default value (e.g. null) to prevent data corruption by state leaking into following new assessments.
     */
    private resetComponent(): void {
        this.participation = undefined;
        this.submission = undefined;
        this.exercise = undefined;
        this.result = undefined;
        this.generalFeedback = new Feedback();
        this.unreferencedFeedback = [];
        this.textBlockRefs = [];
        this.unusedTextBlockRefs = [];
        this.complaint = undefined;
        this.totalScore = 0;

        this.isLoading = true;
        this.saveBusy = false;
        this.submitBusy = false;
        this.cancelBusy = false;
        this.nextSubmissionBusy = false;
        this.isAssessor = false;
        this.isAtLeastInstructor = false;
        this.assessmentsAreValid = false;
        this.noNewSubmissions = false;
    }

    /**
     * Life cycle hook to indicate component creation is done
     */
    async ngOnInit() {
        await super.ngOnInit();
        this.route.queryParamMap.subscribe((queryParams) => {
            this.isTestRun = queryParams.get('testRun') === 'true';
        });

        this.activatedRoute.paramMap.subscribe((paramMap) => (this.exerciseId = Number(paramMap.get('exerciseId'))));
        this.activatedRoute.data.subscribe(({ studentParticipation }) => this.setPropertiesFromServerResponse(studentParticipation));
    }

    private setPropertiesFromServerResponse(studentParticipation: StudentParticipation) {
        this.resetComponent();

        if (studentParticipation == undefined) {
            // Show "No New Submission" banner on .../submissions/new/assessment route
            this.noNewSubmissions = this.isNewAssessmentRoute;
            return;
        }

        this.participation = studentParticipation;
        this.submission = this.participation!.submissions![0] as TextSubmission;
        this.exercise = this.participation?.exercise as TextExercise;
        this.result = this.submission?.result;

        this.hasAssessmentDueDatePassed = !!this.exercise!.assessmentDueDate && moment(this.exercise!.assessmentDueDate).isBefore(now());

        this.prepareTextBlocksAndFeedbacks();
        this.getComplaint();
        this.updateUrlIfNeeded();

        this.checkPermissions(this.result);
        this.totalScore = this.computeTotalScore(this.assessments);
        this.isLoading = false;

        // track feedback in athene
        this.assessmentsService.trackAssessment(this.submission, 'start');
    }

    private updateUrlIfNeeded() {
        if (this.isNewAssessmentRoute) {
            // Update the url with the new id, without reloading the page, to make the history consistent
            const newUrl = this.router
                .createUrlTree(['course-management', this.course?.id, 'text-exercises', this.exercise?.id, 'submissions', this.submission?.id, 'assessment'])
                .toString();
            this.location.go(newUrl);
        }
    }

    private get isNewAssessmentRoute(): boolean {
        return this.activatedRoute.routeConfig?.path === NEW_ASSESSMENT_PATH;
    }

    private checkPermissions(result?: Result): void {
        this.isAssessor = result?.assessor?.id === this.userId;
        // case distinction for exam mode
        this.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(this.course!);
    }

    /**
     * Save the assessment
     */
    save(): void {
        if (!this.assessmentsAreValid) {
            this.jhiAlertService.error('artemisApp.textAssessment.error.invalidAssessments');
            return;
        }

        // track feedback in athene
        this.assessmentsService.trackAssessment(this.submission, 'save');

        this.saveBusy = true;
        this.assessmentsService.save(this.exercise!.id!, this.result!.id!, this.assessments, this.textBlocksWithFeedback).subscribe(
            (response) => this.handleSaveOrSubmitSuccessWithAlert(response, 'artemisApp.textAssessment.saveSuccessful'),
            (error: HttpErrorResponse) => this.handleError(error),
        );
    }

    /**
     * Submit the assessment
     */
    submit(): void {
        if (!this.result?.id) {
            return; // We need to have saved the result before
        }

        if (!this.assessmentsAreValid) {
            this.jhiAlertService.error('artemisApp.textAssessment.error.invalidAssessments');
            return;
        }

        // track feedback in athene
        this.assessmentsService.trackAssessment(this.submission, 'submit');

        this.submitBusy = true;
        this.assessmentsService.submit(this.exercise!.id!, this.result!.id!, this.assessments, this.textBlocksWithFeedback).subscribe(
            (response) => this.handleSaveOrSubmitSuccessWithAlert(response, 'artemisApp.textAssessment.submitSuccessful'),
            (error: HttpErrorResponse) => this.handleError(error),
        );
    }

    protected handleSaveOrSubmitSuccessWithAlert(response: HttpResponse<Result>, translationKey: string): void {
        super.handleSaveOrSubmitSuccessWithAlert(response, translationKey);
        // eslint-disable-next-line chai-friendly/no-unused-expressions
        response.body!.feedbacks?.forEach((newFeedback) => {
            newFeedback.conflictingTextAssessments = this.result?.feedbacks?.find((feedback) => feedback.id === newFeedback.id)?.conflictingTextAssessments;
        });
        this.result = response.body!;
        this.submission!.result = this.result;
        this.saveBusy = this.submitBusy = false;
    }

    /**
     * Cancel the assessment
     */
    cancel(): void {
        const confirmCancel = window.confirm(this.cancelConfirmationText);
        this.cancelBusy = true;
        if (confirmCancel && this.exercise && this.submission) {
            this.assessmentsService.cancelAssessment(this.exercise!.id!, this.submission!.id!).subscribe(() => this.navigateBack());
        }
    }

    /**
     * Go to next submission
     */
    async nextSubmission(): Promise<void> {
        this.nextSubmissionBusy = true;
        await this.router.navigate(['/course-management', this.course?.id, 'text-exercises', this.exercise?.id, 'submissions', 'new', 'assessment']);
    }

    /**
     * if the conflict badge is clicked, navigate to conflict page and add the submission to the extras.
     * @param feedbackId - selected feedback id with conflicts.
     */
    async navigateToConflictingSubmissions(feedbackId: number): Promise<void> {
        const tempSubmission = this.submission!;
        tempSubmission!.result!.completionDate = undefined;
        tempSubmission!.result!.submission = undefined;
        tempSubmission!.result!.participation = undefined;
        const navigationExtras: NavigationExtras = { state: { submission: tempSubmission } };
        await this.router.navigate(
            ['/course-management', this.course?.id, 'text-exercises', this.exercise?.id, 'submissions', this.submission?.id, 'text-feedback-conflict', feedbackId],
            navigationExtras,
        );
    }

    /**
     * Sends the current (updated) assessment to the server to update the original assessment after a complaint was accepted.
     * The corresponding complaint response is sent along with the updated assessment to prevent additional requests.
     *
     * @param complaintResponse the response to the complaint that is sent to the server along with the assessment update
     */
    updateAssessmentAfterComplaint(complaintResponse: ComplaintResponse): void {
        this.validateFeedback();
        if (!this.assessmentsAreValid) {
            this.jhiAlertService.error('artemisApp.textAssessment.error.invalidAssessments');
            return;
        }

        this.assessmentsService.updateAssessmentAfterComplaint(this.assessments, this.textBlocksWithFeedback, complaintResponse, this.submission?.id!).subscribe(
            (response) => this.handleSaveOrSubmitSuccessWithAlert(response, 'artemisApp.textAssessment.updateAfterComplaintSuccessful'),
            () => {
                this.jhiAlertService.clear();
                this.jhiAlertService.error('artemisApp.textAssessment.updateAfterComplaintFailed');
            },
        );
    }

    navigateBack() {
        assessmentNavigateBack(this.location, this.router, this.exercise!, this.submission!, this.isTestRun);
    }

    /**
     * Validate the feedback of the assessment
     */
    validateFeedback(): void {
        const hasReferencedFeedback = this.referencedFeedback.filter(Feedback.isValid).length > 0;
        const hasUnreferencedFeedback = this.unreferencedFeedback.filter(Feedback.isValid).length > 0;
        const hasGeneralFeedback = Feedback.hasDetailText(this.generalFeedback);

        this.assessmentsAreValid = hasReferencedFeedback || hasGeneralFeedback || hasUnreferencedFeedback;

        this.totalScore = this.computeTotalScore(this.assessments);
    }

    private prepareTextBlocksAndFeedbacks(): void {
        if (!this.result) {
            return;
        }
        const feedbacks = this.result.feedbacks || [];
        this.unreferencedFeedback = feedbacks.filter((feedbackElement) => feedbackElement.reference == undefined && feedbackElement.type === FeedbackType.MANUAL_UNREFERENCED);
        const generalFeedbackIndex = feedbacks.findIndex((feedbackElement) => feedbackElement.reference == undefined && feedbackElement.type !== FeedbackType.MANUAL_UNREFERENCED);
        if (generalFeedbackIndex !== -1) {
            this.generalFeedback = feedbacks[generalFeedbackIndex];
            feedbacks.splice(generalFeedbackIndex, 1);
        } else {
            this.generalFeedback = new Feedback();
        }

        const matchBlocksWithFeedbacks = TextAssessmentsService.matchBlocksWithFeedbacks(this.submission?.blocks || [], feedbacks);
        this.sortAndSetTextBlockRefs(matchBlocksWithFeedbacks, this.textBlockRefs, this.unusedTextBlockRefs, this.submission);
    }

    /**
     * Invoked by Child @Output when adding/removing text blocks. Recalculating refs to keep order and prevent duplicate text displayed.
     */
    public recalculateTextBlockRefs(): void {
        // This is racing with another @Output, so we wait one loop
        setTimeout(() => {
            const refs = [...this.textBlockRefs, ...this.unusedTextBlockRefs].filter(({ block, feedback }) => block!.type === TextBlockType.AUTOMATIC || !!feedback);
            this.textBlockRefs = [];
            this.unusedTextBlockRefs = [];

            this.sortAndSetTextBlockRefs(refs, this.textBlockRefs, this.unusedTextBlockRefs, this.submission);
        });
    }

    private getComplaint(): void {
        if (!this.result?.hasComplaint) {
            return;
        }

        this.isLoading = true;
        this.complaintService.findByResultId(this.result!.id!).subscribe(
            (res) => {
                if (!res.body) {
                    return;
                }
                this.complaint = res.body;
                this.isLoading = false;
            },
            (err: HttpErrorResponse) => {
                this.handleError(err.error);
            },
        );
    }

    /**
     * Boolean which determines whether the user can override a result.
     * If no exercise is loaded, for example during loading between exercises, we return false.
     * Instructors can always override a result.
     * Tutors can override their own results within the assessment due date, if there is no complaint about their assessment.
     * They cannot override a result anymore, if there is a complaint. Another tutor must handle the complaint.
     */
    get canOverride(): boolean {
        if (this.exercise) {
            if (this.isAtLeastInstructor) {
                // Instructors can override any assessment at any time.
                return true;
            }
            if (this.complaint && this.isAssessor) {
                // If there is a complaint, the original assessor cannot override the result anymore.
                return false;
            }
            let isBeforeAssessmentDueDate = true;
            // Add check as the assessmentDueDate must not be set for exercises
            if (this.exercise.assessmentDueDate) {
                isBeforeAssessmentDueDate = moment().isBefore(this.exercise.assessmentDueDate!);
            }
            // tutors are allowed to override one of their assessments before the assessment due date.
            return this.isAssessor && isBeforeAssessmentDueDate;
        }
        return false;
    }

    get readOnly(): boolean {
        return !this.isAtLeastInstructor && !!this.complaint && this.isAssessor;
    }

    protected handleError(error: HttpErrorResponse): void {
        super.handleError(error);
        this.saveBusy = this.submitBusy = false;
    }
}
