import * as chai from 'chai';
import * as moment from 'moment';
import { SinonStub, spy, stub } from 'sinon';
import { BehaviorSubject, of, Subject } from 'rxjs';
import * as sinonChai from 'sinon-chai';
import { MockWebsocketService } from '../mocks/mock-websocket.service';
import { MockParticipationWebsocketService } from '../mocks/mock-participation-websocket.service';
import { MockHttpService } from '../mocks/mock-http.service';
import {
    ExerciseSubmissionState,
    IProgrammingSubmissionService,
    ProgrammingSubmissionService,
    ProgrammingSubmissionState,
} from 'app/programming-submission/programming-submission.service';
import { IParticipationWebsocketService } from 'app/entities/participation/participation-websocket.service';
import { MockAlertService } from '../mocks/mock-alert.service';
import { Submission } from 'app/entities/submission';
import { Result } from 'app/entities/result';
import { StudentParticipation } from 'app/entities/participation';
import { SERVER_API_URL } from 'app/app.constants';
import { ProgrammingSubmission } from 'app/entities/programming-submission';

chai.use(sinonChai);
const expect = chai.expect;

describe('ProgrammingSubmissionService', () => {
    let websocketService: MockWebsocketService;
    let http: MockHttpService;
    let participationWebsocketService: IParticipationWebsocketService;
    let alertService: MockAlertService;
    let submissionService: IProgrammingSubmissionService;

    let httpGetStub: SinonStub;
    let wsSubscribeStub: SinonStub;
    let wsReceiveStub: SinonStub;
    let participationWsLatestResultStub: SinonStub;

    let wsSubmissionSubject: Subject<Submission | null>;
    let wsLatestResultSubject: Subject<Result | null>;

    const participationId = 1;
    const submissionTopic = `/topic/participation/${participationId}/newSubmission`;
    const currentSubmission = { id: 11, submissionDate: moment().subtract(20, 'seconds') } as any;
    const currentSubmission2 = { id: 12, submissionDate: moment().subtract(20, 'seconds') } as any;
    const result = { id: 31 } as any;

    beforeEach(() => {
        websocketService = new MockWebsocketService();
        http = new MockHttpService();
        participationWebsocketService = new MockParticipationWebsocketService();
        alertService = new MockAlertService();

        httpGetStub = stub(http, 'get');
        wsSubscribeStub = stub(websocketService, 'subscribe');
        wsSubmissionSubject = new Subject<Submission | null>();
        wsReceiveStub = stub(websocketService, 'receive').returns(wsSubmissionSubject);
        wsLatestResultSubject = new Subject<Result | null>();
        participationWsLatestResultStub = stub(participationWebsocketService, 'subscribeForLatestResultOfParticipation').returns(wsLatestResultSubject as any);

        // @ts-ignore
        submissionService = new ProgrammingSubmissionService(websocketService, http, participationWebsocketService, alertService);
    });

    afterEach(() => {
        httpGetStub.restore();
        wsSubscribeStub.restore();
        wsReceiveStub.restore();
        participationWsLatestResultStub.restore();
    });

    it('should return cached subject as Observable for provided participation if exists', () => {
        const cachedSubject = new BehaviorSubject(null);
        // @ts-ignore
        const fetchLatestPendingSubmissionSpy = spy(submissionService, 'fetchLatestPendingSubmissionByParticipationId');
        // @ts-ignore
        const setupWebsocketSubscriptionSpy = spy(submissionService, 'setupWebsocketSubscription');
        // @ts-ignore
        const subscribeForNewResultSpy = spy(submissionService, 'subscribeForNewResult');
        // @ts-ignore
        submissionService.submissionSubjects = { [participationId]: cachedSubject };

        const returnedObservable = submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10);
        expect(fetchLatestPendingSubmissionSpy).to.not.have.been.called;
        expect(setupWebsocketSubscriptionSpy).to.not.have.been.called;
        expect(subscribeForNewResultSpy).to.not.have.been.called;
    });

    it('should query http endpoint and setup the websocket subscriptions if no subject is cached for the provided participation', async () => {
        httpGetStub.returns(of(currentSubmission));
        const submission = await new Promise(resolve => submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10).subscribe(s => resolve(s)));
        expect(submission).to.deep.equal({ submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission, participationId });
        expect(wsSubscribeStub).to.have.been.calledOnceWithExactly(submissionTopic);
        expect(wsReceiveStub).to.have.been.calledOnceWithExactly(submissionTopic);
        expect(participationWsLatestResultStub).to.have.been.calledOnceWithExactly(participationId);
    });

    it('should emit a null value when a new result comes in for the given participation to signal that the building process is over', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(currentSubmission));
        submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([{ submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission, participationId }]);
        // Result comes in for submission.
        result.submission = currentSubmission;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([
            { submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission, participationId },
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
        ]);
    });

    it('should NOT emit a null value when a new result comes that does not belong to the currentSubmission', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(currentSubmission));
        submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([{ submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission, participationId }]);
        // Result comes in for submission.
        result.submission = currentSubmission2;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([{ submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission, participationId }]);
    });

    it('should emit the newest submission when it was received through the websocket connection', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        // No latest pending submission found.
        httpGetStub.returns(of(null));
        submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([{ submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId }]);
        // New submission comes in.
        wsSubmissionSubject.next(currentSubmission2);
        expect(returnedSubmissions).to.deep.equal([
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
            { submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission2, participationId },
        ]);
        // Result comes in for submission.
        result.submission = currentSubmission2;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
            { submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission2, participationId },
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
        ]);
    });

    it('should emit a null value when the result waiting timer runs out.', async () => {
        // Set the timer to 10ms for testing purposes.
        // @ts-ignore
        submissionService.EXPECTED_RESULT_CREATION_TIME_MS = 10;
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(null));
        submissionService.getLatestPendingSubmissionByParticipationId(participationId, 10).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([{ submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId }]);
        wsSubmissionSubject.next(currentSubmission2);
        expect(returnedSubmissions).to.deep.equal([
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
            { submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission2, participationId },
        ]);
        // Wait 10ms.
        await new Promise(resolve => setTimeout(() => resolve(), 10));
        expect(returnedSubmissions).to.deep.equal([
            { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId },
            { submissionState: ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, submission: currentSubmission2, participationId },
            { submissionState: ProgrammingSubmissionState.HAS_FAILED_SUBMISSION, submission: null, participationId },
        ]);
    });

    it('should fetch the latest pending submissions for all participations of an exercise if preloadLatestPendingSubmissionsForExercise is called', async () => {
        const exerciseId = 3;
        const participation1 = { id: 2 } as StudentParticipation;
        const participation2 = { id: 3 } as StudentParticipation;
        // This participation will not be cached.
        const participation3 = { id: 4 } as StudentParticipation;
        let submissionState, submission;

        const pendingSubmissions = { [participation1.id]: currentSubmission, [participation2.id]: currentSubmission2 };

        // @ts-ignore
        const fetchLatestPendingSubmissionSpy = spy(submissionService, 'fetchLatestPendingSubmissionByParticipationId');

        httpGetStub.returns(of(pendingSubmissions));

        // This load the submissions for participation 1 and 2, but not for 3.
        submissionService.getSubmissionStateOfExercise(exerciseId).toPromise();
        submissionService.getLatestPendingSubmissionByParticipationId(participation1.id, exerciseId).subscribe(({ submissionState: state, submission: sub }) => {
            submissionState = state;
            submission = sub;
        });

        expect(httpGetStub).to.have.been.calledOnceWithExactly('undefinedapi/programming-exercises/3/latest-pending-submissions');
        // Fetching the latest pending submission should not trigger a rest call for a cached submission.
        expect(fetchLatestPendingSubmissionSpy).not.to.have.been.called;
        expect(submissionState).to.equal(ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION);
        expect(submission).to.equal(currentSubmission);

        // Fetching the latest pending submission should trigger a rest call if the submission is not cached.
        await submissionService.getLatestPendingSubmissionByParticipationId(participation3.id, exerciseId);
        expect(fetchLatestPendingSubmissionSpy).to.have.been.calledOnceWithExactly(participation3.id);
    });

    it('should not throw an error on getSubmissionStateOfExercise if state is an empty object', async () => {
        const exerciseId = 10;
        const submissionState: ExerciseSubmissionState = {};
        // @ts-ignore
        const fetchLatestPendingSubmissionByExerciseIdSpy = spy(submissionService, 'fetchLatestPendingSubmissionByExerciseId');
        httpGetStub.withArgs(SERVER_API_URL + `api/programming-exercises/${exerciseId}/latest-pending-submissions`).returns(of(submissionState));

        let receivedSubmissionState: ExerciseSubmissionState;
        submissionService.getSubmissionStateOfExercise(exerciseId).subscribe(state => (receivedSubmissionState = state));
        expect(fetchLatestPendingSubmissionByExerciseIdSpy).to.have.been.calledOnceWithExactly(exerciseId);
        expect(receivedSubmissionState).to.deep.equal(submissionState);
    });

    it('should correctly return the submission state based on the servers response', async () => {
        const exerciseId = 10;
        const submissionState = { 1: { id: 11, submissionDate: moment().subtract(2, 'hours') } as ProgrammingSubmission, 2: null };
        const expectedSubmissionState = {
            1: { submissionState: ProgrammingSubmissionState.HAS_FAILED_SUBMISSION, submission: submissionState['1'], participationId: 1 },
            2: { submissionState: ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, submission: null, participationId: 2 },
        };
        // @ts-ignore
        const fetchLatestPendingSubmissionByExerciseIdSpy = spy(submissionService, 'fetchLatestPendingSubmissionByExerciseId');
        httpGetStub.withArgs(SERVER_API_URL + `api/programming-exercises/${exerciseId}/latest-pending-submissions`).returns(of(submissionState));

        let receivedSubmissionState: ExerciseSubmissionState;
        submissionService.getSubmissionStateOfExercise(exerciseId).subscribe(state => (receivedSubmissionState = state));
        expect(fetchLatestPendingSubmissionByExerciseIdSpy).to.have.been.calledOnceWithExactly(exerciseId);
        expect(receivedSubmissionState).to.deep.equal(expectedSubmissionState);
    });
});
