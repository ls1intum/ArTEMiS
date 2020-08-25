import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { IProgrammingSubmissionService } from 'app/exercises/programming/participate/programming-submission.service';
import { IParticipationWebsocketService } from 'app/overview/participation-websocket.service';
import { SinonSpy, SinonStub, spy, stub } from 'sinon';
import { MockProgrammingSubmissionService } from '../helpers/mocks/service/mock-programming-submission.service';
import { Result } from 'app/entities/result.model';
import { BehaviorSubject, of } from 'rxjs';
import { Feedback, FeedbackType, STATIC_CODE_ANALYSIS_FEEDBACK_IDENTIFIER } from 'app/entities/feedback.model';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { IBuildLogService } from 'app/exercises/programming/shared/service/build-log.service';
import { MockParticipationWebsocketService } from '../helpers/mocks/service/mock-participation-websocket.service';
import { MockCodeEditorBuildLogService } from '../helpers/mocks/service/mock-code-editor-build-log.service';
import { OrionBuildAndTestService } from 'app/shared/orion/orion-build-and-test.service';
import { OrionConnectorService } from 'app/shared/orion/orion-connector.service';
import { MockOrionConnectorService } from '../helpers/mocks/service/mock-orion-connector.service';
import { ProgrammingSubmission } from 'app/entities/programming-submission.model';

chai.use(sinonChai);
const expect = chai.expect;

describe('IdeBuildAndTestService', () => {
    let submissionService: IProgrammingSubmissionService;
    let participationService: IParticipationWebsocketService;
    let javaBridge: OrionConnectorService;
    let buildLogService: IBuildLogService;
    let ideBuildAndTestService: OrionBuildAndTestService;

    let onBuildFinishedSpy: SinonSpy;
    let onBuildStartedSpy: SinonSpy;
    let onTestResultSpy: SinonSpy;
    let onBuildFailedSpy: SinonSpy;
    let buildLogsStub: SinonStub;
    let participationSubscriptionStub: SinonStub;

    const feedbacks = [{ id: 2, positive: false, detailText: 'abc' } as Feedback, { id: 3, positive: true, detailText: 'cde' } as Feedback];
    const feedbacksWithStaticCodeAnalysis = [
        ...feedbacks,
        { id: 3, positive: false, detailText: 'fgh', type: FeedbackType.AUTOMATIC, text: STATIC_CODE_ANALYSIS_FEEDBACK_IDENTIFIER + 'a' } as Feedback,
        { id: 4, positive: false, detailText: 'ijk', type: FeedbackType.AUTOMATIC, text: STATIC_CODE_ANALYSIS_FEEDBACK_IDENTIFIER + 'b' } as Feedback,
    ];
    const result = { id: 1 } as Result;
    const submissionFailed = { id: 1, buildFailed: true } as ProgrammingSubmission;
    const submissionSuccess = { id: 1, buildFailed: false } as ProgrammingSubmission;
    const exercise = { id: 42, studentParticipations: [{ id: 32 }] } as ProgrammingExercise;
    const logs = [
        {
            time: '2019-05-15T10:32:11+02:00',
            log: '[ERROR] COMPILATION ERROR : ',
        },
    ];

    beforeEach(() => {
        submissionService = new MockProgrammingSubmissionService();
        participationService = new MockParticipationWebsocketService();
        javaBridge = new MockOrionConnectorService();
        buildLogService = new MockCodeEditorBuildLogService();

        ideBuildAndTestService = new OrionBuildAndTestService(submissionService, participationService, javaBridge, buildLogService);

        onBuildFinishedSpy = spy(javaBridge, 'onBuildFinished');
        onBuildStartedSpy = spy(javaBridge, 'onBuildStarted');
        onTestResultSpy = spy(javaBridge, 'onTestResult');
        onBuildFailedSpy = spy(javaBridge, 'onBuildFailed');
        buildLogsStub = stub(buildLogService, 'getBuildLogs');
        participationSubscriptionStub = stub(participationService, 'subscribeForLatestResultOfParticipation');
    });

    afterEach(() => {
        onBuildFinishedSpy.restore();
        onBuildStartedSpy.restore();
        onTestResultSpy.restore();
        onBuildFailedSpy.restore();
        buildLogsStub.restore();
        participationSubscriptionStub.restore();
    });

    it('should fetch the build logs if submission is not available', () => {
        buildLogsStub.returns(of(logs));
        result.feedbacks = feedbacks;
        const subject = new BehaviorSubject(result);
        participationSubscriptionStub.returns(subject);

        ideBuildAndTestService.listenOnBuildOutputAndForwardChanges(exercise);

        expect(participationSubscriptionStub).to.have.been.calledOnceWithExactly(exercise.studentParticipations[0].id, true);
        expect(onBuildStartedSpy).to.have.been.called.calledOnce;
        expect(onTestResultSpy).to.not.have.been.called;
        expect(onBuildFinishedSpy).to.not.have.been.called;
        expect(onBuildFailedSpy).to.have.been.calledOnce;
        expect(buildLogsStub).to.have.been.calledOnce;
    });

    it('should fetch the build logs if submission is available and the submission could not be build', () => {
        buildLogsStub.returns(of(logs));
        result.feedbacks = feedbacks;
        result.submission = submissionFailed;
        const subject = new BehaviorSubject(result);
        participationSubscriptionStub.returns(subject);

        ideBuildAndTestService.listenOnBuildOutputAndForwardChanges(exercise);

        expect(participationSubscriptionStub).to.have.been.calledOnceWithExactly(exercise.studentParticipations[0].id, true);
        expect(onBuildStartedSpy).to.have.been.called.calledOnce;
        expect(onTestResultSpy).to.not.have.been.called;
        expect(onBuildFinishedSpy).to.not.have.been.called;
        expect(onBuildFailedSpy).to.have.been.calledOnce;
        expect(buildLogsStub).to.have.been.calledOnce;
    });

    it('should forward all testcase feedback if build was successful', () => {
        result.feedbacks = feedbacks;
        result.submission = submissionSuccess;
        const subject = new BehaviorSubject(result);
        participationSubscriptionStub.returns(subject);

        ideBuildAndTestService.listenOnBuildOutputAndForwardChanges(exercise);

        expect(participationSubscriptionStub).to.have.been.calledOnceWithExactly(exercise.studentParticipations[0].id, true);
        expect(onBuildStartedSpy).to.have.been.called.calledOnce;
        expect(onTestResultSpy).to.have.been.calledTwice;
        expect(onBuildFinishedSpy).to.have.been.calledOnce;
        expect(onBuildFailedSpy).to.not.have.been.called;
        expect(buildLogsStub).to.not.have.been.called;
    });

    it('should filter out static code analysis feedback before forwarding the test case feedback', () => {
        result.feedbacks = feedbacksWithStaticCodeAnalysis;
        result.submission = submissionSuccess;
        const subject = new BehaviorSubject(result);
        participationSubscriptionStub.returns(subject);

        ideBuildAndTestService.listenOnBuildOutputAndForwardChanges(exercise);

        expect(participationSubscriptionStub).to.have.been.calledOnceWithExactly(exercise.studentParticipations[0].id, true);
        expect(onBuildStartedSpy).to.have.been.called.calledOnce;
        expect(onTestResultSpy).to.have.been.calledTwice;
        feedbacks.forEach((feedback, index) => {
            expect(onTestResultSpy.getCall(index)).to.have.been.calledWithExactly(feedback.positive, feedback.text, feedback.detailText);
        });
        expect(onBuildFinishedSpy).to.have.been.calledOnce;
        expect(onBuildFailedSpy).to.not.have.been.called;
        expect(buildLogsStub).to.not.have.been.called;
    });
});
