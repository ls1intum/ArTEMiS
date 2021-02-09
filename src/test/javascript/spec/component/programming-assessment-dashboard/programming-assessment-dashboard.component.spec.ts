import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArtemisTestModule } from '../../test.module';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { JhiLanguageHelper } from 'app/core/language/language.helper';
import { AccountService } from 'app/core/auth/account.service';
import { MockAccountService } from '../../helpers/mocks/service/mock-account.service';
import { MockRouter } from '../../helpers/mocks/mock-router';
import { MockSyncStorage } from '../../helpers/mocks/service/mock-sync-storage.service';
import { MockTranslateService } from '../../helpers/mocks/service/mock-translate.service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { ExerciseType } from 'app/entities/exercise.model';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { SortService } from 'app/shared/service/sort.service';
import { stub } from 'sinon';
import { ProgrammingAssessmentDashboardComponent } from 'app/exercises/programming/assess/programming-assessment-dashboard/programming-assessment-dashboard.component';
import { ProgrammingSubmissionService } from 'app/exercises/programming/participate/programming-submission.service';
import { ProgrammingAssessmentManualResultService } from 'app/exercises/programming/assess/manual-result/programming-assessment-manual-result.service';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { AssessmentType } from 'app/entities/assessment-type.model';

const route = { params: of({ courseId: 3, exerciseId: 22 }) };
const programmingExercise1 = {
    id: 22,
    type: ExerciseType.PROGRAMMING,
    course: { id: 91 },
    numberOfAssessmentsOfCorrectionRounds: {},
} as ProgrammingExercise;
const programmingExercise2 = {
    id: 22,
    type: ExerciseType.PROGRAMMING,
    exerciseGroup: { id: 94, exam: { id: 777, course: { id: 92 } } },
    numberOfAssessmentsOfCorrectionRounds: {},
} as ProgrammingExercise;

const programmingSubmission1 = {
    id: 1,
    submitted: true,
    results: [{ id: 10, assessor: { id: 20, guidedTourSettings: [] } }],
    participation: { id: 41, exercise: programmingExercise1 },
};
const programmingSubmission2 = {
    id: 2,
    submitted: true,
    results: [{ id: 20, assessor: { id: 30, guidedTourSettings: [] } }],
    participation: { id: 41, exercise: programmingExercise2 },
};

describe('FileUploadAssessmentDashboardComponent', () => {
    let component: ProgrammingAssessmentDashboardComponent;
    let fixture: ComponentFixture<ProgrammingAssessmentDashboardComponent>;
    let exerciseService: ExerciseService;
    let programmingSubmissionService: ProgrammingSubmissionService;
    let programmingAssessmentService: ProgrammingAssessmentManualResultService;
    let accountService: AccountService;
    let sortService: SortService;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule, TranslateModule.forRoot(), ArtemisTestModule],
            declarations: [ProgrammingAssessmentDashboardComponent],
            providers: [
                JhiLanguageHelper,
                { provide: Router, useClass: route },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: Router, useClass: MockRouter },
                { provide: AccountService, useClass: MockAccountService },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            paramMap: convertToParamMap({
                                exerciseId: programmingExercise2.id,
                            }),
                        },
                    },
                },
            ],
        })
            .overrideTemplate(ProgrammingAssessmentDashboardComponent, '')
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(ProgrammingAssessmentDashboardComponent);
                component = fixture.componentInstance;
                exerciseService = fixture.debugElement.injector.get(ExerciseService);
                programmingSubmissionService = fixture.debugElement.injector.get(ProgrammingSubmissionService);
                programmingAssessmentService = fixture.debugElement.injector.get(ProgrammingAssessmentManualResultService);
                accountService = fixture.debugElement.injector.get(AccountService);
                sortService = fixture.debugElement.injector.get(SortService);
            });
    }));

    it('should set parameters and call functions on init', fakeAsync(() => {
        // setup
        const exerciseServiceFind = stub(exerciseService, 'find');
        exerciseServiceFind.returns(of(new HttpResponse({ body: programmingExercise1 })));
        spyOn<any>(component, 'setPermissions');
        // test for init values
        expect(component).toBeTruthy();
        expect(component.submissions).toEqual([]);
        expect(component.reverse).toEqual(false);
        expect(component.predicate).toEqual('id');
        expect(component.filteredSubmissions).toEqual([]);

        // call
        component.ngOnInit();
        tick(500);

        // check
        expect(exerciseServiceFind).toHaveBeenCalledWith(programmingExercise2.id);
        expect(component['setPermissions']).toHaveBeenCalled();
        expect(component.exercise).toEqual(programmingExercise1 as ProgrammingExercise);
    }));

    it('should get Submissions', fakeAsync(() => {
        // test getSubmissions
        const exerciseServiceFind = stub(exerciseService, 'find');
        const getProgrammingSubmissionsForExerciseByCorrectionRoundStub = stub(programmingSubmissionService, 'getProgrammingSubmissionsForExerciseByCorrectionRound');
        const isAtLeastInstructorInCourseStub = stub(accountService, 'isAtLeastInstructorInCourse');
        exerciseServiceFind.returns(of(new HttpResponse({ body: programmingExercise1 })));
        getProgrammingSubmissionsForExerciseByCorrectionRoundStub.returns(of(new HttpResponse({ body: [programmingSubmission1] })));
        isAtLeastInstructorInCourseStub.returns(true);
        spyOn<any>(component, 'setPermissions');
        const getSubmissionSpy = spyOn<any>(component, 'getSubmissions');
        getSubmissionSpy.and.callThrough();
        // call
        component.ngOnInit();
        tick(500);
        // check
        expect(component['setPermissions']).toHaveBeenCalled();
        expect(component['getSubmissions']).toHaveBeenCalled();
        expect(getProgrammingSubmissionsForExerciseByCorrectionRoundStub).toHaveBeenCalled();
        expect(getProgrammingSubmissionsForExerciseByCorrectionRoundStub).toHaveBeenCalledWith(programmingExercise2.id, { submittedOnly: true });
        expect(exerciseServiceFind).toHaveBeenCalledWith(programmingExercise2.id);
        expect(component.submissions).toEqual([programmingSubmission1]);
        expect(component.filteredSubmissions).toEqual([programmingSubmission1]);
    }));

    it('should not get Submissions', fakeAsync(() => {
        const exerciseServiceFind = stub(exerciseService, 'find');
        const getProgrammingSubmissionsForExerciseByCorrectionRoundStub = stub(programmingSubmissionService, 'getProgrammingSubmissionsForExerciseByCorrectionRound');
        const isAtLeastInstructorInCourseStub = stub(accountService, 'isAtLeastInstructorInCourse');

        exerciseServiceFind.returns(of(new HttpResponse({ body: programmingExercise1 })));
        getProgrammingSubmissionsForExerciseByCorrectionRoundStub.returns(of(new HttpResponse({ body: [] })));
        isAtLeastInstructorInCourseStub.returns(true);
        // findExerciseStub.returns(of(new HttpResponse({ body: fileUploadExercise, headers: new HttpHeaders() })));
        exerciseServiceFind.returns(of(new HttpResponse({ body: programmingExercise2, headers: new HttpHeaders() })));
        const getSubmissionSpy = spyOn<any>(component, 'getSubmissions');
        getSubmissionSpy.and.callThrough();
        component.exercise = programmingExercise2;

        // call
        component.ngOnInit();

        tick(100);
        // check
        expect(component['getSubmissions']).toHaveBeenCalled();
        expect(exerciseServiceFind).toHaveBeenCalledWith(programmingExercise2.id);
        expect(component.submissions).toEqual([]);
        expect(component.filteredSubmissions).toEqual([]);
    }));

    it('should update filtered submissions', () => {
        // test updateFilteredSubmissions

        // setup
        component.ngOnInit();
        component.updateFilteredSubmissions([programmingSubmission1]);
        // check
        expect(component.filteredSubmissions).toEqual([programmingSubmission1]);
    });

    it('should cancelAssessment', fakeAsync(() => {
        // test cancelAssessment
        const windowSpy = spyOn(window, 'confirm').and.returnValue(true);
        const modelAssServiceCancelAssSpy = spyOn(programmingAssessmentService, 'cancelAssessment').and.returnValue(of(1));
        component.exercise = programmingExercise2;
        // call
        component.cancelAssessment(programmingSubmission2);
        tick();

        // check
        expect(modelAssServiceCancelAssSpy).toHaveBeenCalledWith(programmingSubmission2.id);
        expect(windowSpy).toHaveBeenCalled();
    }));

    it('should sortRows', () => {
        // test cancelAssessment
        const sortServiceSpy = spyOn(sortService, 'sortByProperty');
        component.predicate = 'predicate';
        component.reverse = false;
        component.submissions = [programmingSubmission2];
        component.sortRows();

        expect(sortServiceSpy).toHaveBeenCalledWith([programmingSubmission2], 'predicate', false);
    });

    it('should assessmentTypeTranslationKey', () => {
        const result = { id: 55, assessmentType: AssessmentType.SEMI_AUTOMATIC };
        expect(component.assessmentTypeTranslationKey(result)).toEqual(`artemisApp.AssessmentType.${result.assessmentType}`);
        expect(component.assessmentTypeTranslationKey(undefined)).toEqual(`artemisApp.AssessmentType.null`);
    });

    describe('shouldGetAssessmentLink', () => {
        it('should get assessment link for exam exercise', () => {
            const submission = { id: 6, participation: { id: 8 } };
            component.exercise = programmingExercise1;
            expect(component.getAssessmentLink(submission)).toEqual([
                '/course-management',
                component.exercise.course?.id,
                'programming-exercises',
                component.exercise.id,
                'code-editor',
                submission.participation.id,
                'assessment',
            ]);
        });

        it('should get assessment link for normal exercise', () => {
            const submission = { id: 7, participation: { id: 9 } };
            component.exercise = programmingExercise2;
            expect(component.getAssessmentLink(submission)).toEqual([
                '/course-management',
                component.exercise.exerciseGroup?.exam?.course?.id,
                'programming-exercises',
                component.exercise.id,
                'code-editor',
                submission.participation.id,
                'assessment',
            ]);
        });
    });
});
