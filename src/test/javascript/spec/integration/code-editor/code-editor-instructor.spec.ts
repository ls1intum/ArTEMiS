import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { TranslateModule } from '@ngx-translate/core';
import { AccountService, JhiLanguageHelper, WindowRef } from 'app/core';
import { ChangeDetectorRef, DebugElement } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { SinonStub, spy, stub } from 'sinon';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { AceEditorModule } from 'ng2-ace-editor';
import { TreeviewModule } from 'ngx-treeview';
import {
    ArtemisCodeEditorModule,
    CodeEditorBuildLogService,
    CodeEditorFileService,
    CodeEditorInstructorContainerComponent,
    CodeEditorRepositoryFileService,
    CodeEditorRepositoryService,
    CodeEditorSessionService,
    DomainService,
    DomainType,
} from 'app/code-editor';
import { ArtemisTestModule } from '../../test.module';
import {
    MockActivatedRoute,
    MockCodeEditorBuildLogService,
    MockCodeEditorRepositoryFileService,
    MockCodeEditorRepositoryService,
    MockCodeEditorSessionService,
    MockParticipationService,
    MockParticipationWebsocketService,
    MockProgrammingExerciseService,
    MockResultService,
    MockSyncStorage,
} from '../../mocks';
import { ArtemisResultModule, Result, ResultService } from 'app/entities/result';
import { ArtemisSharedModule } from 'app/shared';
import { ArtemisProgrammingExerciseModule } from 'app/entities/programming-exercise/programming-exercise.module';
import { ParticipationService, ParticipationWebsocketService } from 'app/entities/participation';
import { ProgrammingExercise, ProgrammingExerciseParticipationService, ProgrammingExerciseService } from 'app/entities/programming-exercise';
import { FileType } from 'app/entities/ace-editor/file-change.model';
import { MockAccountService } from '../../mocks/mock-account.service';
import { MockRouter } from '../../mocks/mock-router.service';
import { problemStatement } from '../../sample/problemStatement.json';
import { MockProgrammingExerciseParticipationService } from '../../mocks/mock-programming-exercise-participation.service';

chai.use(sinonChai);
const expect = chai.expect;

describe('CodeEditorInstructorIntegration', () => {
    let container: CodeEditorInstructorContainerComponent;
    let containerFixture: ComponentFixture<CodeEditorInstructorContainerComponent>;
    let containerDebugElement: DebugElement;
    let codeEditorRepositoryFileService: CodeEditorRepositoryFileService;
    let codeEditorRepositoryService: CodeEditorRepositoryService;
    let participationWebsocketService: ParticipationWebsocketService;
    let resultService: ResultService;
    let programmingExerciseParticipationService: ProgrammingExerciseParticipationService;
    let buildLogService: CodeEditorBuildLogService;
    let participationService: ParticipationService;
    let programmingExerciseService: ProgrammingExerciseService;
    let domainService: DomainService;
    let route: ActivatedRoute;
    let router: Router;

    let checkIfRepositoryIsCleanStub: SinonStub;
    let getRepositoryContentStub: SinonStub;
    let subscribeForLatestResultOfParticipationStub: SinonStub;
    let getFeedbackDetailsForResultStub: SinonStub;
    let getBuildLogsStub: SinonStub;
    let getFileStub: SinonStub;
    let saveFilesStub: SinonStub;
    let commitStub: SinonStub;
    let findWithLatestResultStub: SinonStub;
    let findWithParticipationsStub: SinonStub;
    let getLatestResultWithFeedbacksStub: SinonStub;

    let checkIfRepositoryIsCleanSubject: Subject<{ isClean: boolean }>;
    let getRepositoryContentSubject: Subject<{ [fileName: string]: FileType }>;
    let subscribeForLatestResultOfParticipationSubject: BehaviorSubject<Result>;
    let findWithParticipationsSubject: Subject<{ body: ProgrammingExercise }>;
    let routeSubject: Subject<Params>;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot(),
                ArtemisTestModule,
                AceEditorModule,
                TreeviewModule.forRoot(),
                ArtemisSharedModule,
                ArtemisProgrammingExerciseModule,
                ArtemisResultModule,
                ArtemisCodeEditorModule,
            ],
            declarations: [],
            providers: [
                JhiLanguageHelper,
                WindowRef,
                CodeEditorFileService,
                ChangeDetectorRef,
                { provide: Router, useClass: MockRouter },
                { provide: AccountService, useClass: MockAccountService },
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: ResultService, useClass: MockResultService },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: CodeEditorRepositoryService, useClass: MockCodeEditorRepositoryService },
                { provide: CodeEditorRepositoryFileService, useClass: MockCodeEditorRepositoryFileService },
                { provide: CodeEditorBuildLogService, useClass: MockCodeEditorBuildLogService },
                { provide: CodeEditorSessionService, useClass: MockCodeEditorSessionService },
                { provide: ParticipationWebsocketService, useClass: MockParticipationWebsocketService },
                { provide: ResultService, useClass: MockResultService },
                { provide: ParticipationService, useClass: MockParticipationService },
                { provide: ProgrammingExerciseParticipationService, useClass: MockProgrammingExerciseParticipationService },
                { provide: ProgrammingExerciseService, useClass: MockProgrammingExerciseService },
            ],
        })
            .compileComponents()
            .then(() => {
                containerFixture = TestBed.createComponent(CodeEditorInstructorContainerComponent);
                container = containerFixture.componentInstance;
                containerDebugElement = containerFixture.debugElement;

                codeEditorRepositoryService = containerDebugElement.injector.get(CodeEditorRepositoryService);
                codeEditorRepositoryFileService = containerDebugElement.injector.get(CodeEditorRepositoryFileService);
                participationWebsocketService = containerDebugElement.injector.get(ParticipationWebsocketService);
                resultService = containerDebugElement.injector.get(ResultService);
                buildLogService = containerDebugElement.injector.get(CodeEditorBuildLogService);
                participationService = containerDebugElement.injector.get(ParticipationService);
                programmingExerciseParticipationService = containerDebugElement.injector.get(ProgrammingExerciseParticipationService);
                programmingExerciseService = containerDebugElement.injector.get(ProgrammingExerciseService);
                domainService = containerDebugElement.injector.get(DomainService);
                route = containerDebugElement.injector.get(ActivatedRoute);
                router = containerDebugElement.injector.get(Router);

                checkIfRepositoryIsCleanSubject = new Subject<{ isClean: boolean }>();
                getRepositoryContentSubject = new Subject<{ [fileName: string]: FileType }>();
                subscribeForLatestResultOfParticipationSubject = new BehaviorSubject<Result>(null);
                findWithParticipationsSubject = new Subject<{ body: ProgrammingExercise }>();

                routeSubject = new Subject<Params>();
                // @ts-ignore
                (route as MockActivatedRoute).setSubject(routeSubject);

                checkIfRepositoryIsCleanStub = stub(codeEditorRepositoryService, 'getStatus');
                getRepositoryContentStub = stub(codeEditorRepositoryFileService, 'getRepositoryContent');
                subscribeForLatestResultOfParticipationStub = stub(participationWebsocketService, 'subscribeForLatestResultOfParticipation');
                getFeedbackDetailsForResultStub = stub(resultService, 'getFeedbackDetailsForResult');
                getLatestResultWithFeedbacksStub = stub(programmingExerciseParticipationService, 'getLatestResultWithFeedback').returns(throwError('no result'));
                getBuildLogsStub = stub(buildLogService, 'getBuildLogs');
                getFileStub = stub(codeEditorRepositoryFileService, 'getFile');
                saveFilesStub = stub(codeEditorRepositoryFileService, 'updateFiles');
                commitStub = stub(codeEditorRepositoryService, 'commit');
                findWithLatestResultStub = stub(participationService, 'findWithLatestResult');

                findWithParticipationsStub = stub(programmingExerciseService, 'findWithTemplateAndSolutionParticipation');
                findWithParticipationsStub.returns(findWithParticipationsSubject);

                subscribeForLatestResultOfParticipationStub.returns(subscribeForLatestResultOfParticipationSubject);
                getRepositoryContentStub.returns(getRepositoryContentSubject);
                checkIfRepositoryIsCleanStub.returns(checkIfRepositoryIsCleanSubject);
            });
    });

    afterEach(() => {
        checkIfRepositoryIsCleanStub.restore();
        getRepositoryContentStub.restore();
        subscribeForLatestResultOfParticipationStub.restore();
        getFeedbackDetailsForResultStub.restore();
        getBuildLogsStub.restore();
        getFileStub.restore();
        saveFilesStub.restore();
        commitStub.restore();
        findWithLatestResultStub.restore();
        findWithParticipationsStub.restore();
        getLatestResultWithFeedbacksStub.restore();

        subscribeForLatestResultOfParticipationSubject = new BehaviorSubject<Result>(null);
        subscribeForLatestResultOfParticipationStub.returns(subscribeForLatestResultOfParticipationSubject);

        routeSubject = new Subject<Params>();
        // @ts-ignore
        (route as MockActivatedRoute).setSubject(routeSubject);

        findWithParticipationsSubject = new Subject<{ body: ProgrammingExercise }>();
        findWithParticipationsStub.returns(findWithParticipationsSubject);

        checkIfRepositoryIsCleanSubject = new Subject<{ isClean: boolean }>();
        checkIfRepositoryIsCleanStub.returns(checkIfRepositoryIsCleanSubject);

        getRepositoryContentSubject = new Subject<{ [p: string]: FileType }>();
        getRepositoryContentStub.returns(getRepositoryContentSubject);
    });

    it('should load the exercise and select the template participation if no participation id is provided', () => {
        jest.resetModules();
        const exercise = {
            id: 1,
            problemStatement,
            participations: [{ id: 2, repositoryUrl: 'test' }],
            templateParticipation: { id: 3, repositoryUrl: 'test2', results: [{ id: 9, successful: true }] },
            solutionParticipation: { id: 4, repositoryUrl: 'test3' },
        } as ProgrammingExercise;
        exercise.participations = exercise.participations.map(p => ({ ...p, exercise }));
        exercise.templateParticipation = { ...exercise.templateParticipation, exercise };
        exercise.solutionParticipation = { ...exercise.solutionParticipation, exercise };

        getFeedbackDetailsForResultStub.returns(of([]));
        const setDomainSpy = spy(domainService, 'setDomain');
        // @ts-ignore
        (container.router as MockRouter).setUrl('code-editor-instructor/1');
        container.ngOnInit();
        routeSubject.next({ exerciseId: 1 });

        expect(container.grid).not.to.exist;
        expect(findWithParticipationsStub).to.have.been.calledOnceWithExactly(exercise.id);
        expect(container.loadingState).to.equal(container.LOADING_STATE.INITIALIZING);

        findWithParticipationsSubject.next({ body: exercise });

        expect(getLatestResultWithFeedbacksStub).not.to.have.been.called;
        expect(setDomainSpy).to.have.been.calledOnce;
        expect(setDomainSpy).to.have.been.calledOnceWithExactly([DomainType.PARTICIPATION, exercise.templateParticipation]);
        expect(container.exercise).to.deep.equal(exercise);
        expect(container.selectedRepository).to.equal(container.REPOSITORY.TEMPLATE);
        expect(container.selectedParticipation).to.deep.equal(container.selectedParticipation);
        expect(container.loadingState).to.equal(container.LOADING_STATE.CLEAR);
        expect(container.domainChangeSubscription).to.exist;

        containerFixture.detectChanges();
        expect(container.grid).to.exist;

        checkIfRepositoryIsCleanSubject.next({ isClean: true });
        getRepositoryContentSubject.next({ file: FileType.FILE, folder: FileType.FOLDER });
        containerFixture.detectChanges();

        // Result is successful, build logs should not be retrieved
        expect(getBuildLogsStub).to.not.have.been.called;
        // Once called by each build-output & instructions
        expect(getFeedbackDetailsForResultStub).to.have.been.calledTwice;

        expect(container.grid).to.exist;
        expect(container.fileBrowser).to.exist;
        expect(container.actions).to.exist;
        expect(container.instructions).to.exist;
        expect(container.instructions.participation.id).to.deep.equal(exercise.templateParticipation.id);
        expect(container.resultComp).to.exist;
        expect(container.buildOutput).to.exist;

        // Called once by each build-output, instructions, result and twice by instructor-exercise-status (=templateParticipation,solutionParticipation) &
        expect(subscribeForLatestResultOfParticipationStub.callCount).to.equal(5);
    });

    it('should go into error state when loading the exercise failed', () => {
        const exercise = { id: 1, participations: [{ id: 2 }], templateParticipation: { id: 3 }, solutionParticipation: { id: 4 } } as ProgrammingExercise;
        const setDomainSpy = spy(domainService, 'setDomain');
        container.ngOnInit();
        routeSubject.next({ exerciseId: 1 });

        expect(container.grid).not.to.exist;
        expect(findWithParticipationsStub).to.have.been.calledOnceWithExactly(exercise.id);
        expect(container.loadingState).to.equal(container.LOADING_STATE.INITIALIZING);

        findWithParticipationsSubject.error('fatal error');

        expect(setDomainSpy).to.not.have.been.called;
        expect(container.loadingState).to.equal(container.LOADING_STATE.FETCHING_FAILED);
        expect(container.selectedRepository).to.be.undefined;

        containerFixture.detectChanges();
        expect(container.grid).not.to.exist;
    });

    it('should load test repository if specified in url', () => {
        const exercise = {
            id: 1,
            problemStatement,
            participations: [{ id: 2 }],
            templateParticipation: { id: 3 },
            solutionParticipation: { id: 4 },
        } as ProgrammingExercise;
        const setDomainSpy = spy(domainService, 'setDomain');
        // @ts-ignore
        (container.router as MockRouter).setUrl('code-editor-instructor/1/test');
        container.ngOnDestroy();
        container.ngOnInit();
        routeSubject.next({ exerciseId: 1 });

        expect(container.grid).not.to.exist;
        expect(findWithParticipationsStub).to.have.been.calledOnceWithExactly(exercise.id);
        expect(container.loadingState).to.equal(container.LOADING_STATE.INITIALIZING);

        findWithParticipationsSubject.next({ body: exercise });

        expect(setDomainSpy).to.have.been.calledOnceWithExactly([DomainType.TEST_REPOSITORY, exercise]);
        expect(container.selectedParticipation).not.to.exist;
        expect(container.selectedRepository).to.equal(container.REPOSITORY.TEST);
        expect(getBuildLogsStub).not.to.have.been.called;
        expect(getFeedbackDetailsForResultStub).not.to.have.been.called;

        containerFixture.detectChanges();

        expect(container.grid).to.exist;
        expect(container.fileBrowser).to.exist;
        expect(container.actions).to.exist;
        expect(container.instructions).to.exist;
        expect(container.instructions.participation).to.deep.equal(exercise.templateParticipation);
        expect(container.resultComp).not.to.exist;
        expect(container.buildOutput).not.to.exist;
    });

    it('should be able to switch between the repos and update the child components accordingly', () => {
        const exercise = {
            id: 1,
            problemStatement,
            participations: [{ id: 2, repositoryUrl: 'test' }],
            templateParticipation: { id: 3, repositoryUrl: 'test2' },
            solutionParticipation: { id: 4, repositoryUrl: 'test3' },
        } as ProgrammingExercise;

        const setDomainSpy = spy(domainService, 'setDomain');

        // Start with assignment repository
        // @ts-ignore
        (container.router as MockRouter).setUrl('code-editor-instructor/1/2');
        container.ngOnInit();
        routeSubject.next({ exerciseId: 1, participationId: 2 });
        findWithParticipationsSubject.next({ body: exercise });

        containerFixture.detectChanges();

        expect(container.selectedRepository).to.equal(container.REPOSITORY.ASSIGNMENT);
        expect(container.selectedParticipation).to.deep.equal({ ...exercise.participations[0], exercise });
        expect(container.grid).to.exist;
        expect(container.fileBrowser).to.exist;
        expect(container.actions).to.exist;
        expect(container.instructions).to.exist;
        expect(container.resultComp).to.exist;
        expect(container.buildOutput).to.exist;
        expect(container.buildOutput.participation).to.deep.equal({ ...exercise.participations[0], exercise });
        expect(container.instructions.participation).to.deep.equal({ ...exercise.participations[0], exercise });

        // New select solution repository
        // @ts-ignore
        (container.router as MockRouter).setUrl('code-editor-instructor/1/4');
        routeSubject.next({ exerciseId: 1, participationId: 4 });

        containerFixture.detectChanges();

        expect(container.selectedRepository).to.equal(container.REPOSITORY.SOLUTION);
        expect(container.selectedParticipation).to.deep.equal({ ...exercise.solutionParticipation, exercise });
        expect(container.grid).to.exist;
        expect(container.fileBrowser).to.exist;
        expect(container.actions).to.exist;
        expect(container.instructions).to.exist;
        expect(container.resultComp).to.exist;
        expect(container.buildOutput).to.exist;
        expect(container.buildOutput.participation).to.deep.equal({ ...exercise.solutionParticipation, exercise });
        expect(container.instructions.participation).to.deep.equal({ ...exercise.solutionParticipation, exercise });

        expect(findWithParticipationsStub).to.have.been.calledOnceWithExactly(exercise.id);
        expect(setDomainSpy).to.have.been.calledTwice;
        expect(setDomainSpy).to.have.been.calledWith([DomainType.PARTICIPATION, exercise.participations[0]]);
        expect(setDomainSpy).to.have.been.calledWith([DomainType.PARTICIPATION, exercise.solutionParticipation]);
    });

    it('should not be able to select a repository without repositoryUrl', () => {
        const exercise = {
            id: 1,
            problemStatement,
            participations: [{ id: 2, repositoryUrl: 'test' }],
            templateParticipation: { id: 3 },
            solutionParticipation: { id: 4, repositoryUrl: 'test3' },
        } as ProgrammingExercise;

        const setDomainSpy = spy(domainService, 'setDomain');

        // Start with assignment repository
        // @ts-ignore
        (container.router as MockRouter).setUrl('code-editor-instructor/1/3');
        container.ngOnInit();
        routeSubject.next({ exerciseId: 1, participationId: 3 });
        findWithParticipationsSubject.next({ body: exercise });

        containerFixture.detectChanges();

        expect(setDomainSpy).to.have.been.calledOnce;
        expect(setDomainSpy).to.have.been.calledOnceWithExactly([DomainType.PARTICIPATION, exercise.solutionParticipation]);
        expect(container.selectedRepository).to.equal(container.REPOSITORY.SOLUTION);
        expect(container.selectedParticipation).to.deep.equal({ ...exercise.solutionParticipation, exercise });
        expect(container.grid).to.exist;
        expect(container.fileBrowser).to.exist;
        expect(container.actions).to.exist;
        expect(container.instructions).to.exist;
        expect(container.resultComp).to.exist;
        expect(container.buildOutput).to.exist;
        expect(container.buildOutput.participation).to.deep.equal({ ...exercise.solutionParticipation, exercise });
        expect(container.instructions.participation).to.deep.equal({ ...exercise.solutionParticipation, exercise });
    });
});
