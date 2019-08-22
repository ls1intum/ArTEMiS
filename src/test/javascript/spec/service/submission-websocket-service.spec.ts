import * as chai from 'chai';
import * as moment from 'moment';
import { SinonStub, spy, stub } from 'sinon';
import { BehaviorSubject, of, Subject } from 'rxjs';
import * as sinonChai from 'sinon-chai';
import { MockWebsocketService } from '../mocks/mock-websocket.service';
import { MockParticipationWebsocketService } from '../mocks/mock-participation-websocket.service';
import { MockHttpService } from '../mocks/mock-http.service';
import { ISubmissionWebsocketService, ProgrammingSubmissionState, ProgrammingSubmissionWebsocketService } from 'app/submission/programming-submission-websocket.service';
import { IParticipationWebsocketService } from 'app/entities/participation/participation-websocket.service';
import { MockAlertService } from '../mocks/mock-alert.service';
import { Submission } from 'app/entities/submission';
import { Result } from 'app/entities/result';

chai.use(sinonChai);
const expect = chai.expect;

describe('SubmissionWebsocketService', () => {
    let websocketService: MockWebsocketService;
    let http: MockHttpService;
    let participationWebsocketService: IParticipationWebsocketService;
    let alertService: MockAlertService;
    let submissionWebsocketService: ISubmissionWebsocketService;

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
        submissionWebsocketService = new ProgrammingSubmissionWebsocketService(websocketService, http, participationWebsocketService, alertService);
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
        const fetchLatestPendingSubmissionSpy = spy(submissionWebsocketService, 'fetchLatestPendingSubmission');
        // @ts-ignore
        const setupWebsocketSubscriptionSpy = spy(submissionWebsocketService, 'setupWebsocketSubscription');
        // @ts-ignore
        const subscribeForNewResultSpy = spy(submissionWebsocketService, 'subscribeForNewResult');
        // @ts-ignore
        submissionWebsocketService.submissionSubjects = { [participationId]: cachedSubject };

        const returnedObservable = submissionWebsocketService.getLatestPendingSubmission(participationId);
        expect(fetchLatestPendingSubmissionSpy).to.not.have.been.called;
        expect(setupWebsocketSubscriptionSpy).to.not.have.been.called;
        expect(subscribeForNewResultSpy).to.not.have.been.called;
    });

    it('should query http endpoint and setup the websocket subscriptions if no subject is cached for the provided participation', async () => {
        httpGetStub.returns(of(currentSubmission));
        const submission = await new Promise(resolve => submissionWebsocketService.getLatestPendingSubmission(participationId).subscribe(s => resolve(s)));
        expect(submission).to.deep.equal([ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission]);
        expect(wsSubscribeStub).to.have.been.calledOnceWithExactly(submissionTopic);
        expect(wsReceiveStub).to.have.been.calledOnceWithExactly(submissionTopic);
        expect(participationWsLatestResultStub).to.have.been.calledOnceWithExactly(participationId);
    });

    it('should emit a null value when a new result comes in for the given participation to signal that the building process is over', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(currentSubmission));
        submissionWebsocketService.getLatestPendingSubmission(participationId).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([[ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission]]);
        // Result comes in for submission.
        result.submission = currentSubmission;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([
            [ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission],
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
        ]);
    });

    it('should NOT emit a null value when a new result comes that does not belong to the currentSubmission', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(currentSubmission));
        submissionWebsocketService.getLatestPendingSubmission(participationId).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([[ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission]]);
        // Result comes in for submission.
        result.submission = currentSubmission2;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([[ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission]]);
    });

    it('should emit the newest submission when it was received through the websocket connection', () => {
        const returnedSubmissions: Array<Submission | null> = [];
        // No latest pending submission found.
        httpGetStub.returns(of(null));
        submissionWebsocketService.getLatestPendingSubmission(participationId).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([[ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null]]);
        // New submission comes in.
        wsSubmissionSubject.next(currentSubmission2);
        expect(returnedSubmissions).to.deep.equal([
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
            [ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission2],
        ]);
        // Result comes in for submission.
        result.submission = currentSubmission2;
        wsLatestResultSubject.next(result);
        expect(returnedSubmissions).to.deep.equal([
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
            [ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission2],
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
        ]);
    });

    it('should emit a null value when the result waiting timer runs out.', async () => {
        // Set the timer to 10ms for testing purposes.
        // @ts-ignore
        submissionWebsocketService.EXPECTED_RESULT_CREATION_TIME_MS = 10;
        const returnedSubmissions: Array<Submission | null> = [];
        httpGetStub.returns(of(null));
        submissionWebsocketService.getLatestPendingSubmission(participationId).subscribe(s => returnedSubmissions.push(s));
        expect(returnedSubmissions).to.deep.equal([[ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null]]);
        wsSubmissionSubject.next(currentSubmission2);
        expect(returnedSubmissions).to.deep.equal([
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
            [ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission2],
        ]);
        // Wait 10ms.
        await new Promise(resolve => setTimeout(() => resolve(), 10));
        expect(returnedSubmissions).to.deep.equal([
            [ProgrammingSubmissionState.HAS_NO_PENDING_SUBMISSION, null],
            [ProgrammingSubmissionState.IS_BUILDING_PENDING_SUBMISSION, currentSubmission2],
            [ProgrammingSubmissionState.HAS_FAILED_SUBMISSION, null],
        ]);
    });
});
