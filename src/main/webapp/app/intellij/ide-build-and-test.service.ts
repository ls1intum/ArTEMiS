import { Injectable } from '@angular/core';
import { ProgrammingExercise } from '../entities/programming-exercise';
import { ProgrammingSubmissionService } from '../programming-submission';
import { ParticipationWebsocketService } from '../entities/participation';
import { Result } from '../entities/result';
import { filter, map, tap } from 'rxjs/operators';
import { JavaBridgeService } from 'app/intellij/java-bridge.service';
import { BuildLogEntryArray } from 'app/entities/build-log';
import { Observable, Subject, Subscription } from 'rxjs';
import { BuildLogService } from 'app/programming-assessment/build-logs/build-log.service';

/**
 * Notifies the IDE about a result, that is currently building and forwards incoming test results.
 *
 */
@Injectable({
    providedIn: 'root',
})
export class IdeBuildAndTestService {
    private buildFinished = new Subject<void>();
    private resultSubsription: Subscription;
    private buildLogSubscription: Subscription;
    private latestResult: Result;

    constructor(
        private submissionService: ProgrammingSubmissionService,
        private participationWebsocketService: ParticipationWebsocketService,
        private javaBridge: JavaBridgeService,
        private buildLogService: BuildLogService,
    ) {}

    /**
     * Trigger a new build for a participation for an exercise and notify the IDE
     *
     * @param exercise The exercise for which a build should get triggered
     */
    buildAndTestExercise(exercise: ProgrammingExercise) {
        const participationId = exercise.studentParticipations[0].id;
        // Trigger a build for the current participation
        this.submissionService.triggerBuild(participationId).subscribe();

        this.listenOnBuildOutputAndForwardChanges(exercise);
    }

    /**
     * Listens on any new builds for the user's participation on the websocket and forwards incoming results to the IDE
     *
     * @param exercise The exercise for which build results should get forwarded
     */
    listenOnBuildOutputAndForwardChanges(exercise: ProgrammingExercise): Observable<void> {
        const participationId = exercise.studentParticipations[0].id;
        this.javaBridge.onBuildStarted();

        // Listen for the new result on the websocket
        if (this.resultSubsription) {
            this.resultSubsription.unsubscribe();
        }
        if (this.buildLogSubscription) {
            this.buildLogSubscription.unsubscribe();
        }
        this.resultSubsription = this.participationWebsocketService
            .subscribeForLatestResultOfParticipation(participationId)
            .pipe(
                filter(Boolean),
                map(result => result as Result),
                filter(result => !this.latestResult || this.latestResult.id < result.id),
                tap(result => {
                    this.latestResult = result;
                    // If there was no compile error, we can forward the test results, otherwise we have to fetch the error output
                    if ((result && result.successful) || (result && !result.successful && result.feedbacks && result.feedbacks.length)) {
                        result.feedbacks.forEach(feedback => this.javaBridge.onTestResult(!!feedback.positive, feedback.detailText!));
                        this.javaBridge.onBuildFinished();
                        this.buildFinished.next();
                    } else {
                        this.forwardBuildLogs(participationId);
                    }
                }),
            )
            .subscribe();

        return this.buildFinished;
    }

    private forwardBuildLogs(participationId: number) {
        this.buildLogSubscription = this.buildLogService
            .getBuildLogs(participationId)
            .pipe(
                map(logs => new BuildLogEntryArray(...logs)),
                tap((logs: BuildLogEntryArray) => {
                    const logErrors = logs.extractErrors();
                    this.javaBridge.onBuildFailed(JSON.stringify(logErrors));
                    this.buildFinished.next();
                }),
            )
            .subscribe();
    }
}
