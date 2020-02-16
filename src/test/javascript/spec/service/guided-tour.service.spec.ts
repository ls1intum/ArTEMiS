import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of, Observable } from 'rxjs';
import { CookieService } from 'ngx-cookie';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { TranslateService } from '@ngx-translate/core';
import { ArtemisSharedModule } from 'app/shared';
import { ArtemisTestModule } from '../test.module';
import { NavbarComponent } from 'app/layouts';
import { SERVER_API_URL } from 'app/app.constants';
import { GuidedTour } from 'app/guided-tour/guided-tour.model';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { GuidedTourState, Orientation, UserInteractionEvent } from 'app/guided-tour/guided-tour.constants';
import { GuidedTourComponent } from 'app/guided-tour/guided-tour.component';
import { MockCookieService, MockSyncStorage } from '../mocks';
import { GuidedTourMapping, GuidedTourSetting } from 'app/guided-tour/guided-tour-setting.model';
import { ModelingTaskTourStep, TextTourStep, UserInterActionTourStep } from 'app/guided-tour/guided-tour-step.model';
import { MockAccountService } from '../mocks/mock-account.service';
import { AccountService } from 'app/core/auth/account.service';
import { DeviceDetectorService } from 'ngx-device-detector';
import { Course } from 'app/entities/course/course.model';
import { Exercise, ExerciseType } from 'app/entities/exercise';
import { MockTranslateService } from '../mocks/mock-translate.service';
import { GuidedTourModelingTask, personUML } from 'app/guided-tour/guided-tour-task.model';
import { completedTour } from 'app/guided-tour/tours/general-tour';
import { InitializationState, StudentParticipation } from 'app/entities/participation';
import { SinonStub, stub } from 'sinon';
import { HttpResponse } from '@angular/common/http';
import { ParticipationService } from 'app/entities/participation/participation.service';

chai.use(sinonChai);
const expect = chai.expect;

describe('GuidedTourService', () => {
    const tour: GuidedTour = {
        settingsKey: 'tour',
        steps: [
            new TextTourStep({ highlightSelector: '.random-selector', headlineTranslateKey: '', contentTranslateKey: '' }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithUserInteraction: GuidedTour = {
        settingsKey: 'tour_user_interaction',
        steps: [
            new UserInterActionTourStep({
                highlightSelector: '.random-selector',
                headlineTranslateKey: '',
                contentTranslateKey: '',
                userInteractionEvent: UserInteractionEvent.CLICK,
            }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithCourseAndExercise: GuidedTour = {
        settingsKey: 'tour_with_course_and_exercise',
        steps: [
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '' }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithModelingTask: GuidedTour = {
        settingsKey: 'tour_modeling_task',
        steps: [
            new ModelingTaskTourStep({
                headlineTranslateKey: '',
                contentTranslateKey: '',
                modelingTask: new GuidedTourModelingTask(personUML.name, ''),
                userInteractionEvent: UserInteractionEvent.MODELING,
            }),
        ],
    };

    describe('Service method', () => {
        let service: GuidedTourService;
        let httpMock: any;
        const expected = new GuidedTourSetting('guided_tour_key', 1, GuidedTourState.STARTED);

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule, ArtemisSharedModule, HttpClientTestingModule],
                providers: [
                    { provide: DeviceDetectorService },
                    { provide: LocalStorageService, useClass: MockSyncStorage },
                    { provide: SessionStorageService, useClass: MockSyncStorage },
                ],
            })
                .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
                .compileComponents();

            service = TestBed.get(GuidedTourService);
            httpMock = TestBed.get(HttpTestingController);
        });

        afterEach(() => {
            httpMock.verify();
        });

        it('should call the correct update URL and return the right JSON object', () => {
            service.guidedTourSettings = [];
            service['updateGuidedTourSettings']('guided_tour_key', 1, GuidedTourState.STARTED).subscribe();
            const req = httpMock.expectOne({ method: 'PUT' });
            const resourceUrl = SERVER_API_URL + 'api/guided-tour-settings';
            expect(req.request.url).equal(`${resourceUrl}`);
            expect(service.guidedTourSettings).to.eql([expected]);
        });

        it('should call the correct delete URL', () => {
            service.guidedTourSettings = [new GuidedTourSetting('guided_tour_key', 1, GuidedTourState.STARTED)];
            service['deleteGuidedTourSetting']('guided_tour_key').subscribe();
            const req = httpMock.expectOne({ method: 'DELETE' });
            const resourceUrl = SERVER_API_URL + 'api/guided-tour-settings';
            expect(req.request.url).equal(`${resourceUrl}/guided_tour_key`);
            expect(service.guidedTourSettings).to.eql([]);
        });
    });

    describe('Guided tour methods', () => {
        let guidedTourComponent: GuidedTourComponent;
        let guidedTourComponentFixture: ComponentFixture<GuidedTourComponent>;
        let router: Router;
        let guidedTourService: GuidedTourService;
        let participationService: ParticipationService;

        let findParticipationStub: SinonStub;
        let deleteParticipationStub: SinonStub;
        let deleteGuidedTourSettingStub: SinonStub;
        let navigationStub: SinonStub;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [
                    ArtemisTestModule,
                    ArtemisSharedModule,
                    RouterTestingModule.withRoutes([
                        {
                            path: 'overview',
                            component: NavbarComponent,
                        },
                    ]),
                ],
                declarations: [NavbarComponent, GuidedTourComponent],
                providers: [
                    { provide: LocalStorageService, useClass: MockSyncStorage },
                    { provide: SessionStorageService, useClass: MockSyncStorage },
                    { provide: CookieService, useClass: MockCookieService },
                    { provide: AccountService, useClass: MockAccountService },
                    { provide: DeviceDetectorService },
                    { provide: TranslateService, useClass: MockTranslateService },
                ],
            })
                .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
                .overrideTemplate(NavbarComponent, '<div class="random-selector"></div>')
                .compileComponents()
                .then(() => {
                    guidedTourComponentFixture = TestBed.createComponent(GuidedTourComponent);
                    guidedTourComponent = guidedTourComponentFixture.componentInstance;

                    const navBarComponentFixture = TestBed.createComponent(NavbarComponent);
                    const navBarComponent = navBarComponentFixture.componentInstance;

                    router = TestBed.get(Router);
                    guidedTourService = TestBed.get(GuidedTourService);
                    participationService = TestBed.get(ParticipationService);

                    findParticipationStub = stub(participationService, 'findParticipation');
                    deleteParticipationStub = stub(participationService, 'deleteForGuidedTour');
                    // @ts-ignore
                    deleteGuidedTourSettingStub = stub(guidedTourService, 'deleteGuidedTourSetting');
                    navigationStub = stub(router, 'navigateByUrl');
                });
        });

        function prepareGuidedTour(tour: GuidedTour) {
            // Prepare GuidedTourService and GuidedTourComponent
            spyOn(guidedTourService, 'init').and.returnValue(of());
            spyOn(guidedTourService, 'getLastSeenTourStepIndex').and.returnValue(0);
            spyOn<any>(guidedTourService, 'checkSelectorValidity').and.returnValue(true);
            spyOn<any>(guidedTourService, 'checkTourState').and.returnValue(true);
            spyOn<any>(guidedTourService, 'updateGuidedTourSettings').and.returnValue(of());
            spyOn<any>(guidedTourService, 'enableTour').and.callFake(() => {
                guidedTourService['availableTourForComponent'] = tour;
                guidedTourService.currentTour = tour;
            });
        }

        async function startCourseOverviewTour(tour: GuidedTour) {
            guidedTourComponent.ngAfterViewInit();

            await guidedTourComponentFixture.ngZone!.run(() => {
                router.navigateByUrl('/overview');
            });

            // Start course overview tour
            expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            guidedTourService['enableTour'](tour);
            guidedTourService['startTour']();
            guidedTourComponentFixture.detectChanges();
            expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.exist;
            expect(guidedTourService.isOnFirstStep).to.be.true;
            expect(guidedTourService.currentTourStepDisplay).to.equal(1);
            expect(guidedTourService.currentTourStepCount).to.equal(2);
        }

        describe('Tours without user interaction', () => {
            beforeEach(async () => {
                prepareGuidedTour(tour);
                await startCourseOverviewTour(tour);
            });

            it('should start and finish the course overview guided tour', async () => {
                // Navigate to next step
                const nextButton = guidedTourComponentFixture.debugElement.query(By.css('.next-button'));
                expect(nextButton).to.exist;
                nextButton.nativeElement.click();
                expect(guidedTourService.isOnLastStep).to.be.true;

                // Finish guided tour
                nextButton.nativeElement.click();
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            });

            it('should start and skip the tour', () => {
                const skipButton = guidedTourComponentFixture.debugElement.query(By.css('.close'));
                expect(skipButton).to.exist;
                skipButton.nativeElement.click();
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            });

            it('should prevent backdrop from advancing', () => {
                const backdrop = guidedTourComponentFixture.debugElement.queryAll(By.css('.guided-tour-overlay'));
                expect(backdrop).to.exist;
                expect(backdrop.length).to.equal(4);
                backdrop.forEach(overlay => {
                    overlay.nativeElement.click();
                });
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourService.isOnFirstStep).to.be.true;
            });
        });

        describe('Tours with user interaction', () => {
            beforeEach(async () => {
                prepareGuidedTour(tourWithUserInteraction);
                await startCourseOverviewTour(tourWithUserInteraction);
            });

            it('should disable the next button', () => {
                guidedTourComponentFixture.detectChanges();
                const nextButton = guidedTourComponentFixture.debugElement.nativeElement.querySelector('.next-button').disabled;
                expect(nextButton).to.exist;
            });
        });

        describe('Tour for a certain course and exercise', () => {
            const guidedTourMapping = { courseShortName: 'tutorial', tours: { tour_with_course_and_exercise: 'git' } } as GuidedTourMapping;
            const exercise1 = { id: 1, shortName: 'git', type: ExerciseType.PROGRAMMING } as Exercise;
            const exercise2 = { id: 2, shortName: 'test', type: ExerciseType.PROGRAMMING } as Exercise;
            const exercise3 = { id: 3, shortName: 'git', type: ExerciseType.MODELING } as Exercise;
            const course1 = { id: 1, shortName: 'tutorial', exercises: [exercise2, exercise1] } as Course;
            const course2 = { id: 2, shortName: 'test' } as Course;

            function resetCurrentTour(): void {
                guidedTourService['currentCourse'] = null;
                guidedTourService['currentExercise'] = null;
                guidedTourService.currentTour = completedTour;
                guidedTourService.resetTour();
            }

            function currentCourseAndExerciseNull(): void {
                expect(guidedTourService.currentTour).to.be.null;
                expect(guidedTourService['currentCourse']).to.be.null;
                expect(guidedTourService['currentExercise']).to.be.null;
            }

            beforeEach(async () => {
                guidedTourService.guidedTourMapping = guidedTourMapping;
                prepareGuidedTour(tourWithCourseAndExercise);
                resetCurrentTour();
            });

            it('should start the tour for the matching course title', () => {
                let courses = [course1];
                // enable tour for matching course title
                guidedTourService.enableTourForCourseOverview(courses, tourWithCourseAndExercise);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);
                expect(guidedTourService['currentCourse']).to.equal(course1);
                expect(guidedTourService['currentExercise']).to.be.null;
                resetCurrentTour();

                courses = [course2];
                // disable tour for not matching titles
                guidedTourService.enableTourForCourseOverview(courses, tourWithCourseAndExercise);
                currentCourseAndExerciseNull();
            });

            it('should start the tour for the matching exercise short name', () => {
                // disable tour for exercises without courses
                guidedTourService.currentTour = null;
                guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // disable tour for not matching course and exercise identifiers
                exercise2.course = course2;
                guidedTourService.enableTourForExercise(exercise2, tourWithCourseAndExercise);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // disable tour for not matching course identifier
                exercise3.course = course2;
                guidedTourService.enableTourForExercise(exercise3, tourWithCourseAndExercise);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // enable tour for matching course and exercise identifiers
                exercise1.course = course1;
                guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);
                expect(guidedTourService['currentCourse']).to.equal(course1);
                expect(guidedTourService['currentExercise']).to.equal(exercise1);
            });

            it('should start the tour for the matching course / exercise short name', () => {
                guidedTourService.currentTour = null;

                // enable tour for matching course / exercise short name
                guidedTourService.enableTourForCourseExerciseComponent(course1, tourWithCourseAndExercise);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);

                course1.exercises.forEach(exercise => {
                    exercise.course = course1;
                    if (exercise === exercise1) {
                        expect(guidedTourService['isGuidedTourAvailableForExercise'](exercise)).to.be.true;
                    } else {
                        expect(guidedTourService['isGuidedTourAvailableForExercise'](exercise)).to.be.false;
                    }
                });

                // disable tour for not matching course without exercise
                guidedTourService.currentTour = null;
                guidedTourService.enableTourForCourseExerciseComponent(course2, tourWithCourseAndExercise);
                expect(guidedTourService.currentTour).to.be.null;

                // disable tour for not matching course but matching exercise identifier
                guidedTourService.currentTour = null;
                course2.exercises = [exercise3];
                guidedTourService.enableTourForCourseExerciseComponent(course2, tourWithCourseAndExercise);
                expect(guidedTourService.currentTour).to.be.null;
            });

            describe('Tour with student participation', () => {
                const studentParticipation1 = { id: 1, student: { id: 1 }, exercise: exercise1, initializationState: InitializationState.INITIALIZED } as StudentParticipation;
                const studentParticipation2 = { id: 2, student: { id: 1 }, exercise: exercise3, initializationState: InitializationState.INITIALIZED } as StudentParticipation;
                const httpResponse1 = { body: studentParticipation1 } as HttpResponse<StudentParticipation>;
                const httpResponse2 = { body: studentParticipation2 } as HttpResponse<StudentParticipation>;
                const exercise4 = { id: 4, title: 'git', type: ExerciseType.MODELING } as Exercise;

                function prepareParticipation(exercise: Exercise, studentParticipation: StudentParticipation, httpResponse: HttpResponse<StudentParticipation>) {
                    exercise.course = course1;
                    exercise.studentParticipations = [studentParticipation];
                    findParticipationStub.reset();
                    deleteParticipationStub.reset();
                    deleteGuidedTourSettingStub.reset();
                    navigationStub.reset();
                    findParticipationStub.returns(Observable.of(httpResponse));
                    deleteParticipationStub.returns(Observable.of(null));
                    deleteGuidedTourSettingStub.returns(Observable.of(null));
                }

                it('should find and delete the student participation for exercise', () => {
                    course1.exercises.push(exercise4);

                    prepareParticipation(exercise1, studentParticipation1, httpResponse1);
                    guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise);
                    guidedTourService.restartTour();
                    expect(findParticipationStub).to.have.been.calledOnceWithExactly(1);
                    expect(deleteParticipationStub).to.have.been.calledOnceWithExactly(1, { deleteBuildPlan: true, deleteRepository: true });
                    expect(deleteGuidedTourSettingStub).to.have.been.calledOnceWith('tour_with_course_and_exercise');
                    expect(navigationStub).to.have.been.calledOnceWith('/overview/1/exercises');

                    prepareParticipation(exercise4, studentParticipation2, httpResponse2);
                    guidedTourService.enableTourForExercise(exercise4, tourWithCourseAndExercise);
                    guidedTourService.restartTour();
                    expect(findParticipationStub).to.have.been.calledOnceWithExactly(4);
                    expect(deleteParticipationStub).to.have.been.calledOnceWithExactly(2, { deleteBuildPlan: false, deleteRepository: false });
                    expect(deleteGuidedTourSettingStub).to.have.been.calledOnceWith('tour_with_course_and_exercise');
                    expect(navigationStub).to.have.been.calledOnceWith('/overview/1/exercises');

                    const index = course1.exercises.findIndex(exercise => (exercise.id = exercise4.id));
                    course1.exercises.splice(index, 1);
                });
            });
        });

        describe('Dot calculation', () => {
            it('should calculate the n-small dot display', () => {
                // Initially the getLastSeenTourStepIndex is 0 because we don't access the user guided settings
                expect(guidedTourService.calculateNSmallDot(0)).to.be.false;
                expect(guidedTourService.calculateNSmallDot(10)).to.be.true;

                // We update the getLastSeenTourStepIndex to check whether it is called correctly if the last seen step is bigger than the max dots value
                spyOn(guidedTourService, 'getLastSeenTourStepIndex').and.returnValue(12);
                expect(guidedTourService.calculateNSmallDot(14)).to.be.true;
            });

            it('should calculate the p-small dot', () => {
                // The p-small class is not displayed if the total count of steps is smaller than the max dots value
                expect(guidedTourService.calculatePSmallDot(0)).to.be.false;
                spyOn(guidedTourService, 'getLastSeenTourStepIndex').and.returnValue(15);
                expect(guidedTourService.calculatePSmallDot(8)).to.be.true;
            });
        });

        describe('Modeling check', () => {
            it('should enable the next step if the results are correct', inject(
                [],
                fakeAsync(() => {
                    const enableNextStep = spyOn<any>(guidedTourService, 'enableNextStepClick').and.returnValue(of());
                    guidedTourService.currentTour = tourWithModelingTask;
                    guidedTourService.updateModelingResult(personUML.name, true);
                    tick(0);
                    expect(enableNextStep.calls.count()).to.equal(1);
                }),
            ));
        });
    });
});
