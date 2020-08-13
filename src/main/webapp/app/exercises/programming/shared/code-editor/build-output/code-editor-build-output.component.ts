import { ParticipationWebsocketService } from 'app/overview/participation-websocket.service';
import { AfterViewInit, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { Observable, of, Subscription } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { BuildLogEntry, BuildLogEntryArray } from 'app/entities/build-log.model';
import { Participation } from 'app/entities/participation/participation.model';
import { CodeEditorSubmissionService } from 'app/exercises/programming/shared/code-editor/service/code-editor-submission.service';
import { CodeEditorBuildLogService } from 'app/exercises/programming/shared/code-editor/service/code-editor-repository.service';
import { Feedback } from 'app/entities/feedback.model';
import { ResultService } from 'app/exercises/shared/result/result.service';
import { hasParticipationChanged } from 'app/overview/participation-utils';
import { Result } from 'app/entities/result.model';
import { Interactable } from '@interactjs/core/Interactable';
import interact from 'interactjs';
import { Annotation } from '../ace/code-editor-ace.component';

@Component({
    selector: 'jhi-code-editor-build-output',
    templateUrl: './code-editor-build-output.component.html',
    styleUrls: ['./code-editor-build-output.scss'],
})
export class CodeEditorBuildOutputComponent implements AfterViewInit, OnInit, OnChanges, OnDestroy {
    @Input()
    participation: Participation;

    @Output()
    onAnnotations = new EventEmitter<Array<Annotation>>();
    @Output()
    onToggleCollapse = new EventEmitter<{ event: any; horizontal: boolean; interactable: Interactable; resizableMinWidth?: number; resizableMinHeight: number }>();
    @Output()
    onError = new EventEmitter<string>();

    isBuilding: boolean;
    rawBuildLogs = new BuildLogEntryArray();
    result: Result;

    /** Resizable constants **/
    resizableMinHeight = 150;
    interactResizable: Interactable;

    private resultSubscription: Subscription;
    private submissionSubscription: Subscription;

    constructor(
        private buildLogService: CodeEditorBuildLogService,
        private resultService: ResultService,
        private participationWebsocketService: ParticipationWebsocketService,
        private submissionService: CodeEditorSubmissionService,
    ) {}

    ngOnInit(): void {
        this.setupSubmissionWebsocket();
    }

    /**
     * @function ngAfterViewInit
     * @desc After the view was initialized, we create an interact.js resizable object,
     *       designate the edges which can be used to resize the target element and set min and max values.
     *       The 'resizemove' callback function processes the event values and sets new width and height values for the element.
     */
    ngAfterViewInit(): void {
        this.resizableMinHeight = window.screen.height / 6;
        this.interactResizable = interact('.resizable-buildoutput');
    }

    /**
     * @function ngOnChanges
     * @desc We need to update the participation results under certain conditions:
     *       - Participation changed => reset websocket connection and load initial result
     * @param {SimpleChanges} changes
     *
     */
    ngOnChanges(changes: SimpleChanges): void {
        const participationChange = hasParticipationChanged(changes);
        if (participationChange) {
            this.setupResultWebsocket();
        }
        // If the participation changes and it has results, fetch the result details to decide if the build log should be shown
        if (participationChange && this.participation.results && this.participation.results.length) {
            const latestResult = this.participation.results.reduce((acc, x) => (x.id > acc.id ? x : acc));
            of(latestResult)
                .pipe(
                    switchMap((result) => (result && !result.feedbacks ? this.loadAndAttachResultDetails(result) : of(result))),
                    tap((result) => (this.result = result)),
                    switchMap((result) => this.fetchBuildResults(result)),
                    map((buildLogsFromServer) => BuildLogEntryArray.fromBuildLogs(buildLogsFromServer!)),
                    tap((buildLogsFromServer: BuildLogEntryArray) => {
                        this.rawBuildLogs = buildLogsFromServer;
                    }),
                    catchError(() => {
                        this.rawBuildLogs = new BuildLogEntryArray();
                        return Observable.of();
                    }),
                )
                .subscribe(() => this.extractAnnotations());
        } else {
            if (!this.resultSubscription && this.participation) {
                this.setupResultWebsocket();
            }
        }
    }

    /**
     * Extracts annotations from
     * - the build logs as compilation errors
     * - the result feedbacks as static code analysis issues
     * and emits them to the parent component
     */
    private extractAnnotations() {
        const buildLogErrors = this.rawBuildLogs.extractErrors();
        const codeAnalysisIssues = (this.result.feedbacks || []).filter(Feedback.isStaticCodeAnalysisFeedback).map<Annotation>((f) => ({
            text: f.detailText || '',
            fileName: 'src/' + (f.reference?.split(':')[0] || '').split('.').join('/') + '.java', // TODO support other files
            row: parseInt(f.reference?.split(':')[1] || '0', 10) - 1,
            column: 0,
            type: 'warning', // TODO encode type in feedback
            timestamp: this.result.completionDate != null ? new Date(this.result.completionDate.toString()).valueOf() : 0,
        }));
        this.onAnnotations.emit([...buildLogErrors, ...codeAnalysisIssues]);
    }

    /**
     * Subscribe to incoming submissions, translating to the state isBuilding = true (a pending submission without result exists) vs = false (no pending submission).
     */
    private setupSubmissionWebsocket() {
        this.submissionSubscription = this.submissionService
            .getBuildingState()
            .pipe(tap((isBuilding: boolean) => (this.isBuilding = isBuilding)))
            .subscribe();
    }

    /**
     * Set up the websocket for retrieving build results.
     * Online updates the build logs if the result is new, otherwise doesn't react.
     */
    private setupResultWebsocket() {
        if (this.resultSubscription) {
            this.resultSubscription.unsubscribe();
        }
        this.resultSubscription = this.participationWebsocketService
            .subscribeForLatestResultOfParticipation(this.participation.id, true)
            .pipe(
                // Ignore initial null result from service
                filter((result) => !!result),
                tap((result) => (this.result = result!)),
                switchMap((result) => this.fetchBuildResults(result)),
                tap((buildLogsFromServer: BuildLogEntry[]) => {
                    this.rawBuildLogs = BuildLogEntryArray.fromBuildLogs(buildLogsFromServer);
                }),
                catchError(() => {
                    this.onError.emit('failedToLoadBuildLogs');
                    this.rawBuildLogs = new BuildLogEntryArray();
                    return Observable.of(null);
                }),
            )
            .subscribe(() => this.extractAnnotations());
    }

    /**
     * @function loadResultDetails
     * @desc Fetches details for the result (if we received one) and attach them to the result.
     * Mutates the input parameter result.
     */
    loadAndAttachResultDetails(result: Result): Observable<Result> {
        return this.resultService.getFeedbackDetailsForResult(result.id).pipe(
            map((res) => res && res.body),
            map((feedbacks: Feedback[]) => {
                result.feedbacks = feedbacks;
                return result;
            }),
            catchError(() => of(result)),
        );
    }

    /**
     * @function getBuildLogs
     * @desc Gets the buildlogs for the current participation
     */
    getBuildLogs() {
        return this.buildLogService.getBuildLogs();
    }

    /**
     * Decides if the build log should be shown.
     * If All tests were successful or there is test feedback -> don't show build logs.
     * Else -> show build logs.
     * @param result
     */
    fetchBuildResults(result: Result | null): Observable<BuildLogEntry[] | null> {
        if ((result && result.successful) || (result && !result.successful && result.feedbacks && result.feedbacks.length)) {
            return of([]);
        } else {
            return this.getBuildLogs();
        }
    }

    /**
     * @function toggleEditorCollapse
     * @desc Calls the parent (editorComponent) toggleCollapse method
     * @param $event
     * @param {boolean} horizontal
     */
    toggleEditorCollapse($event: any) {
        this.onToggleCollapse.emit({
            event: $event,
            horizontal: false,
            interactable: this.interactResizable,
            resizableMinWidth: undefined,
            resizableMinHeight: this.resizableMinHeight,
        });
    }

    ngOnDestroy() {
        if (this.resultSubscription) {
            this.resultSubscription.unsubscribe();
        }
        if (this.submissionSubscription) {
            this.submissionSubscription.unsubscribe();
        }
    }
}
