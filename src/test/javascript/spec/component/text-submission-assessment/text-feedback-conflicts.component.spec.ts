import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ArtemisAssessmentSharedModule } from 'app/assessment/assessment-shared.module';
import { ArtemisTestModule } from '../../test.module';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute, RouterModule, Router, convertToParamMap } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { MockComponent } from 'ng-mocks';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { TextFeedbackConflictsComponent } from 'app/exercises/text/assess-new/conflicts/text-feedback-conflicts.component';
import { TextAssessmentsService } from 'app/exercises/text/assess/text-assessments.service';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { AssessmentInstructionsModule } from 'app/assessment/assessment-instructions/assessment-instructions.module';
import { ArtemisConfirmIconModule } from 'app/shared/confirm-icon/confirm-icon.module';
import { TextSharedModule } from 'app/exercises/text/shared/text-shared.module';
import { TextSubmissionAssessmentComponent } from 'app/exercises/text/assess-new/text-submission-assessment.component';
import { TextAssessmentAreaComponent } from 'app/exercises/text/assess-new/text-assessment-area/text-assessment-area.component';
import { TextblockAssessmentCardComponent } from 'app/exercises/text/assess-new/textblock-assessment-card/textblock-assessment-card.component';
import { TextblockFeedbackEditorComponent } from 'app/exercises/text/assess-new/textblock-feedback-editor/textblock-feedback-editor.component';
import { ManualTextblockSelectionComponent } from 'app/exercises/text/assess-new/manual-textblock-selection/manual-textblock-selection.component';
import { TextFeedbackConflictsHeaderComponent } from 'app/exercises/text/assess-new/conflicts/conflicts-header/text-feedback-conflicts-header.component';
import { SubmissionExerciseType, SubmissionType } from 'app/entities/submission.model';
import { TextSubmission } from 'app/entities/text-submission.model';
import { Result } from 'app/entities/result.model';
import { Feedback } from 'app/entities/feedback.model';
import { TextBlock } from 'app/entities/text-block.model';
import * as moment from 'moment';
import { FeedbackConflict, FeedbackConflictType } from 'app/entities/feedback-conflict';
import { ExerciseType } from 'app/entities/exercise.model';
import { AssessmentType } from 'app/entities/assessment-type.model';
import { Course } from 'app/entities/course.model';
import { TextExercise } from 'app/entities/text-exercise.model';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { ParticipationType } from 'app/entities/participation/participation.model';

describe('TextFeedbackConflictsComponent', () => {
    let component: TextFeedbackConflictsComponent;
    let fixture: ComponentFixture<TextFeedbackConflictsComponent>;
    let textAssessmentsService: TextAssessmentsService;
    let router: Router;

    const exercise = {
        id: 20,
        type: ExerciseType.TEXT,
        assessmentType: AssessmentType.MANUAL,
        course: { id: 123, isAtLeastInstructor: true } as Course,
    } as TextExercise;
    const participation: StudentParticipation = ({
        type: ParticipationType.STUDENT,
        exercise,
    } as unknown) as StudentParticipation;
    const textSubmission = ({
        submissionExerciseType: SubmissionExerciseType.TEXT,
        id: 2278,
        submitted: true,
        type: SubmissionType.MANUAL,
        submissionDate: moment('2019-07-09T10:47:33.244Z'),
        text: 'First text. Second text.',
        participation,
    } as unknown) as TextSubmission;
    textSubmission.result = ({
        id: 2374,
        resultString: '1 of 12 points',
        completionDate: moment('2019-07-09T11:51:23.251Z'),
        successful: false,
        score: 8,
        rated: true,
        hasFeedback: true,
        hasComplaint: false,
        textSubmission,
    } as unknown) as Result;
    textSubmission.result.feedbacks = [
        {
            id: 1,
            detailText: 'First Feedback',
            credits: 1.5,
            reference: 'First text id',
        } as Feedback,
    ];
    textSubmission.blocks = [
        ({
            id: 'First text id',
            text: 'First text.',
            startIndex: 0,
            endIndex: 11,
            textSubmission,
        } as unknown) as TextBlock,
        ({
            id: 'second text id',
            text: 'Second text.',
            startIndex: 12,
            endIndex: 24,
            textSubmission,
        } as unknown) as TextBlock,
    ];
    textSubmission.result.feedbacks[0].conflictingTextAssessments = [
        {
            id: 1,
            conflict: true,
            conflictingFeedbackId: 5,
            createdAt: moment('2019-07-09T11:51:23.251Z'),
            type: FeedbackConflictType.INCONSISTENT_COMMENT,
            markedAsNoConflict: false,
        } as FeedbackConflict,
    ];

    const conflictingSubmission = ({
        submissionExerciseType: SubmissionExerciseType.TEXT,
        id: 2280,
        submitted: true,
        type: SubmissionType.MANUAL,
        submissionDate: moment('2019-07-09T10:47:33.244Z'),
        text: 'First Conflicting Submission Text.',
    } as unknown) as TextSubmission;
    conflictingSubmission.result = ({
        id: 2375,
        completionDate: moment('2020-02-10T11:51:23.251Z'),
        successful: false,
        score: 3,
        rated: true,
        hasFeedback: true,
        hasComplaint: false,
        conflictingSubmission,
    } as unknown) as Result;
    conflictingSubmission.result.feedbacks = [
        {
            id: 5,
            detailText: 'Conflicting feedback',
            credits: 1.5,
            reference: 'Conflicting text id',
        } as Feedback,
    ];
    conflictingSubmission.blocks = [
        ({
            id: 'Conflicting text id',
            text: 'First Conflicting Submission Text.',
            startIndex: 0,
            endIndex: 34,
            conflictingSubmission,
        } as unknown) as TextBlock,
    ];

    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [
                ArtemisTestModule,
                ArtemisSharedModule,
                ArtemisAssessmentSharedModule,
                AssessmentInstructionsModule,
                TranslateModule.forRoot(),
                ArtemisConfirmIconModule,
                RouterModule,
                RouterTestingModule,
                TextSharedModule,
            ],
            declarations: [
                TextFeedbackConflictsComponent,
                TextSubmissionAssessmentComponent,
                TextAssessmentAreaComponent,
                TextblockAssessmentCardComponent,
                TextblockFeedbackEditorComponent,
                ManualTextblockSelectionComponent,
                TextFeedbackConflictsHeaderComponent,
            ],
            providers: [{ provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ feedbackId: 1 }) } } }],
        })
            .overrideModule(ArtemisTestModule, {
                remove: {
                    declarations: [MockComponent(FaIconComponent)],
                    exports: [MockComponent(FaIconComponent)],
                },
            })
            .compileComponents();
    });

    beforeEach(() => {
        router = TestBed.inject(Router);
        spyOn(router, 'getCurrentNavigation').and.returnValues({ extras: { state: { submission: textSubmission } } } as any);
        fixture = TestBed.createComponent(TextFeedbackConflictsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should set passed parameters correctly in constructor', () => {
        expect(component.leftSubmission).toBe(textSubmission);
        expect(component.leftFeedbackId).toBe(1);
        expect(component.exercise).toBe(exercise);
    });

    it('should use jhi-text-feedback-conflicts-header', () => {
        const headerComponent = fixture.debugElement.query(By.directive(TextFeedbackConflictsHeaderComponent));
        expect(headerComponent).toBeTruthy();
    });

    it('should set conflicting submission correctly', () => {
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();
        expect(component.rightSubmission).toBe(conflictingSubmission);
        expect(component.rightTotalScore).toBe(1.5);
        expect(component.feedbackConflicts).toBe(textSubmission.result!.feedbacks![0].conflictingTextAssessments!);
        expect(component.rightTextBlockRefs[0].feedback).toBe(conflictingSubmission.result!.feedbacks![0]);
    });

    it('should use jhi-text-assessment-area', () => {
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();
        const textAssessmentAreaComponent = fixture.debugElement.query(By.directive(TextAssessmentAreaComponent));
        expect(textAssessmentAreaComponent).toBeTruthy();
    });

    it('should solve conflict by overriding left submission', () => {
        textAssessmentsService = fixture.debugElement.injector.get(TextAssessmentsService);
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();

        expect(component.isOverrideDisabled).toBe(true);

        const textAssessmentArea = fixture.debugElement.query(By.directive(TextAssessmentAreaComponent));
        const textAssessmentAreaComponent = textAssessmentArea.componentInstance as TextAssessmentAreaComponent;
        const textBlockRef = textAssessmentAreaComponent.textBlockRefs[0];
        textBlockRef.feedback!.detailText = 'Correcting feedback text.';
        textBlockRef.feedback!.credits = 2;
        textAssessmentAreaComponent.textBlockRefsChangeEmit();

        expect(component.leftTotalScore).toBe(2);
        expect(component.isOverrideDisabled).toBe(false);

        spyOn(textAssessmentsService, 'submit').and.returnValue(
            of(
                new HttpResponse({
                    body: component.leftSubmission!.result,
                }),
            ),
        );
        component.overrideLeftSubmission();
        expect(textAssessmentsService.submit).toHaveBeenCalledWith(
            exercise.id!,
            textSubmission.result!.id!,
            [component.leftTextBlockRefs[0].feedback!],
            [component.leftTextBlockRefs[0].block!],
        );
    });

    it('should be able to select conflicting feedback', () => {
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();

        const textBlockAssessmentAreas = fixture.debugElement.queryAll(By.directive(TextblockAssessmentCardComponent));
        textBlockAssessmentAreas.forEach((textBlockAssessmentCardArea) => {
            const textBlockAssessmentCardComponent = textBlockAssessmentCardArea.componentInstance as TextblockAssessmentCardComponent;
            if (textBlockAssessmentCardComponent.textBlockRef === component.rightTextBlockRefs[0]) {
                textBlockAssessmentCardComponent.select();
            }
        });

        expect(component.selectedRightFeedbackId).toBeTruthy();
        expect(component.selectedRightFeedbackId).toBe(conflictingSubmission.result!.feedbacks![0].id);
    });

    it('should be able to un-select conflicting feedback', () => {
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();

        const textBlockAssessmentAreas = fixture.debugElement.queryAll(By.directive(TextblockAssessmentCardComponent));
        textBlockAssessmentAreas.forEach((textBlockAssessmentCardArea) => {
            const textBlockAssessmentCardComponent = textBlockAssessmentCardArea.componentInstance as TextblockAssessmentCardComponent;
            if (textBlockAssessmentCardComponent.textBlockRef === component.rightTextBlockRefs[0]) {
                textBlockAssessmentCardComponent.select();
                fixture.detectChanges();
                textBlockAssessmentCardComponent.select();
            }
        });

        expect(component.selectedRightFeedbackId).toBeFalsy();
        expect(component.selectedRightFeedbackId).toBe(undefined);
    });

    it('should not be able to select conflicting feedback for left submission', () => {
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();

        const textBlockAssessmentAreas = fixture.debugElement.queryAll(By.directive(TextblockAssessmentCardComponent));
        textBlockAssessmentAreas.forEach((textBlockAssessmentCardArea) => {
            const textBlockAssessmentCardComponent = textBlockAssessmentCardArea.componentInstance as TextblockAssessmentCardComponent;
            if (textBlockAssessmentCardComponent.textBlockRef === component.leftTextBlockRefs[0]) {
                spyOn(textBlockAssessmentCardComponent, 'didSelect');
                textBlockAssessmentCardComponent.select();
                expect(textBlockAssessmentCardComponent.didSelect).toHaveBeenCalledTimes(0);
            }
        });

        expect(component.selectedRightFeedbackId).toBeFalsy();
        expect(component.selectedRightFeedbackId).toBe(undefined);
    });

    it('should mark selected as no conflict', () => {
        textAssessmentsService = fixture.debugElement.injector.get(TextAssessmentsService);
        component['setPropertiesFromServerResponse']([conflictingSubmission]);
        fixture.detectChanges();

        component.didSelectConflictingFeedback(conflictingSubmission.result!.feedbacks![0].id!);

        const feedbackConflict = textSubmission.result!.feedbacks![0].conflictingTextAssessments![0];
        feedbackConflict.conflict = false;
        feedbackConflict.markedAsNoConflict = true;
        spyOn(textAssessmentsService, 'solveFeedbackConflict').and.returnValue(
            of(
                new HttpResponse({
                    body: feedbackConflict,
                }),
            ),
        );
        component.markSelectedAsNoConflict();
        expect(textAssessmentsService.solveFeedbackConflict).toHaveBeenCalledWith(exercise.id!, feedbackConflict.id!);
    });

    it('should switch submissions when it changed in the header', () => {
        const secondConflictingSubmission = Object.assign({}, conflictingSubmission);
        secondConflictingSubmission.id! += 1;
        secondConflictingSubmission.result!.id! += 1;
        secondConflictingSubmission.result!.feedbacks![0].id! += 1;

        component['setPropertiesFromServerResponse']([conflictingSubmission, secondConflictingSubmission]);
        fixture.detectChanges();

        const textFeedbackConflictsHeader = fixture.debugElement.query(By.directive(TextFeedbackConflictsHeaderComponent));
        const textFeedbackConflictsHeaderComponent = textFeedbackConflictsHeader.componentInstance as TextFeedbackConflictsHeaderComponent;
        expect(textFeedbackConflictsHeaderComponent.numberOfConflicts).toBe(2);
        textFeedbackConflictsHeaderComponent.onNextConflict();
        expect(component.rightSubmission).toBe(secondConflictingSubmission);
        textFeedbackConflictsHeaderComponent.onPrevConflict();
        expect(component.rightSubmission).toBe(conflictingSubmission);
    });
});
