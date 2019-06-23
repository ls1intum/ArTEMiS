import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import * as moment from 'moment';
import { TranslateModule } from '@ngx-translate/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ChartsModule } from 'ng2-charts';
import { TreeviewModule } from 'ngx-treeview';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { ArTEMiSTestModule } from '../../../test.module';
import { ArTEMiSSharedModule } from 'app/shared';
import { ActivatedRoute } from '@angular/router';
import { Attachment } from 'app/entities/attachment';
import { CourseStatisticsComponent } from 'app/overview';
import { By } from '@angular/platform-browser';
import { Course, CourseScoreCalculationService } from 'app/entities/course';
import { ModelingExercise } from 'app/entities/modeling-exercise';
import { MockSyncStorage } from '../../../mocks';
import { Result } from 'app/entities/result';

chai.use(sinonChai);
const expect = chai.expect;

describe('CourseStatisticsComponent', () => {
    let comp: CourseStatisticsComponent;
    let fixture: ComponentFixture<CourseStatisticsComponent>;
    let courseScoreCalculationService: CourseScoreCalculationService;

    const exercises = [
        {
            type: 'modeling',
            id: 192,
            title: 'test 17.06. 1',
            dueDate: moment('2019-06-17T09:47:12+02:00'),
            assessmentDueDate: moment('2019-06-17T09:55:17+02:00'),
            maxScore: 12.0,
            participations: [
                {
                    id: 248,
                    initializationState: 'FINISHED',
                    initializationDate: moment('2019-06-17T09:29:34.908+02:00'),
                    student: {
                        id: 9,
                        login: 'artemis_test_user_1',
                        firstName: 'ArTEMiS Test User 1',
                        email: 'krusche+testuser_1@in.tum.de',
                        activated: true,
                        langKey: 'en',
                    },
                },
            ],
            diagramType: 'ClassDiagram',
            numberOfParticipations: 0,
            numberOfAssessments: 0,
            numberOfComplaints: 0,
        },
        {
            type: 'modeling',
            id: 193,
            title: 'test 17.06. 2',
            dueDate: moment('2019-06-17T17:50:08+02:00'),
            assessmentDueDate: moment('2019-06-17T17:51:13+02:00'),
            maxScore: 12.0,
            participations: [
                {
                    id: 249,
                    initializationState: 'FINISHED',
                    initializationDate: moment('2019-06-18T10:53:27.997+02:00'),
                    student: {
                        id: 9,
                        login: 'artemis_test_user_1',
                        firstName: 'ArTEMiS Test User 1',
                        email: 'krusche+testuser_1@in.tum.de',
                        activated: true,
                        langKey: 'en',
                    },
                },
            ],
            diagramType: 'ClassDiagram',
            numberOfParticipations: 0,
            numberOfAssessments: 0,
            numberOfComplaints: 0,
        },
        {
            type: 'modeling',
            id: 194,
            title: 'test 18.06. 1',
            dueDate: moment('2019-06-18T07:56:41+02:00'),
            maxScore: 12.0,
            participations: [],
            diagramType: 'ClassDiagram',
            numberOfParticipations: 0,
            numberOfAssessments: 0,
            numberOfComplaints: 0,
        },
        {
            type: 'modeling',
            id: 191,
            title: 'Until 18:20',
            dueDate: moment('2019-06-16T18:15:03+02:00'),
            assessmentDueDate: moment('2019-06-16T18:30:57+02:00'),
            maxScore: 12.0,
            participations: [
                {
                    id: 246,
                    initializationState: 'FINISHED',
                    initializationDate: moment('2019-06-16T18:10:28.293+02:00'),
                    results: [
                        {
                            id: 231,
                            resultString: '11 of 12 points',
                            completionDate: moment('2019-06-17T09:30:17.761+02:00'),
                            successful: false,
                            score: 92,
                            rated: true,
                            hasFeedback: false,
                            assessmentType: 'MANUAL',
                            hasComplaint: false,
                        },
                    ],
                    student: {
                        id: 9,
                        login: 'artemis_test_user_1',
                        firstName: 'ArTEMiS Test User 1',
                        email: 'krusche+testuser_1@in.tum.de',
                        activated: true,
                        langKey: 'en',
                    },
                },
            ],
            diagramType: 'ClassDiagram',
            numberOfParticipations: 0,
            numberOfAssessments: 0,
            numberOfComplaints: 0,
        },
    ] as ModelingExercise[];

    const course = {
        id: 64,
        title: 'Checking statistics',
        description: 'Testing the statistics view',
        shortName: 'CHS',
        studentGroupName: 'jira-users',
        teachingAssistantGroupName: 'artemis-dev',
        instructorGroupName: 'artemis-dev',
        onlineCourse: false,
        registrationEnabled: false,
        exercises: [],
    } as Course;

    const newAttachment = {
        id: 53,
        name: 'TestFile',
        link: '/api/files/attachments/lecture/4/Mein_Test_PDF3.pdf',
        version: 1,
        uploadDate: moment('2019-05-07T08:49:59+02:00'),
        attachmentType: 'FILE',
    } as Attachment;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArTEMiSTestModule, TreeviewModule.forRoot(), RouterTestingModule.withRoutes([]), ArTEMiSSharedModule, ChartsModule],
            declarations: [CourseStatisticsComponent],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        parent: {
                            params: {
                                subscribe: (fn: (value: any) => void) => fn(1),
                            },
                        },
                    },
                },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: LocalStorageService, useClass: MockSyncStorage },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(CourseStatisticsComponent);
                comp = fixture.componentInstance;
                courseScoreCalculationService = TestBed.get(CourseScoreCalculationService);
            });
    });

    it('should group all exercises', () => {
        const courseToAdd = { ...course };
        courseToAdd.exercises = [...exercises];
        spyOn(courseScoreCalculationService, 'getCourse').and.returnValue(courseToAdd);
        fixture.detectChanges();
        comp.ngOnInit();
        fixture.detectChanges();
        expect(comp.groupedExercises.length).to.equal(1);
        const modelingWrapper = fixture.debugElement.query(By.css('#modeling-wrapper'));
        expect(modelingWrapper.query(By.css('h2')).nativeElement.textContent).to.exist;
        expect(modelingWrapper.query(By.css('#absolute-score')).nativeElement.textContent).to.exist;
        expect(modelingWrapper.query(By.css('#max-score')).nativeElement.textContent).to.exist;
        expect(modelingWrapper.query(By.css('#relative-score')).nativeElement.textContent).to.exist;
        expect(modelingWrapper.query(By.css('#presentation-score')).nativeElement.textContent).to.exist;
        expect(comp.groupedExercises[0].notGraded.tooltips).to.include.members(['artemisApp.courseOverview.statistics.exerciseNotGraded']);
        expect(comp.groupedExercises[0].scores.tooltips).to.include.members(['artemisApp.courseOverview.statistics.exerciseAchievedScore']);
        expect(comp.groupedExercises[0].missedScores.tooltips).to.include.members(['artemisApp.courseOverview.statistics.exerciseParticipatedAfterDueDate']);
        expect(comp.groupedExercises[0].missedScores.tooltips).to.include.members(['artemisApp.courseOverview.statistics.exerciseNotParticipated']);
        expect(comp.groupedExercises[0].missedScores.tooltips).to.include.members(['artemisApp.courseOverview.statistics.exerciseMissedScore']);
    });

    it('should transform results correctly', () => {
        const result1 = { resultString: '9 of 26 failed' } as Result;
        expect(comp.absoluteResult(result1)).to.be.null;
        const result2 = { resultString: 'No tests found' } as Result;
        expect(comp.absoluteResult(result2)).to.be.null;
        const result3 = { resultString: null } as Result;
        expect(comp.absoluteResult(result3)).to.equal(0);
        const result4 = { resultString: '15 passed' } as Result;
        expect(comp.absoluteResult(result4)).to.be.null;
        const result5 = { resultString: '20 of 20 points' } as Result;
        expect(comp.absoluteResult(result5)).to.equal(20);
    });
});
