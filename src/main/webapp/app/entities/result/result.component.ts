import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { Participation, ParticipationService } from '../participation';
import { Result, ResultDetailComponent, ResultService, ResultWebsocketService } from '.';
import { ProgrammingSubmission } from '../programming-submission';
import { JhiWebsocketService, AccountService } from '../../core';
import { RepositoryService } from 'app/entities/repository/repository.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpClient } from '@angular/common/http';
import { ExerciseType } from 'app/entities/exercise';
import { MIN_POINTS_GREEN, MIN_POINTS_ORANGE } from 'app/app.constants';

import * as moment from 'moment';
import { TranslateService } from '@ngx-translate/core';
import { ProgrammingExercise } from 'app/entities/programming-exercise';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

@Component({
    selector: 'jhi-result',
    templateUrl: './result.component.html',
    providers: [ResultService, RepositoryService],
})

/**
 * When using the result component make sure that the reference to the participation input is changed if the result changes
 * e.g. by using Object.assign to trigger ngOnChanges which makes sure that the result is updated
 */
export class ResultComponent implements OnInit, OnChanges, OnDestroy {
    // make constants available to html for comparison
    readonly QUIZ = ExerciseType.QUIZ;
    readonly PROGRAMMING = ExerciseType.PROGRAMMING;
    readonly MODELING = ExerciseType.MODELING;

    @Input() participation: Participation;
    @Input() isBuilding: boolean;
    @Input() short = false;
    @Input() result: Result;
    @Input() showUngradedResults: boolean;
    @Output() newResult = new EventEmitter<object>();

    private resultSubscription: Subscription;
    websocketChannelSubmissions: string;
    textColorClass: string;
    hasFeedback: boolean;
    resultIconClass: string[];
    resultString: string;

    constructor(
        private jhiWebsocketService: JhiWebsocketService,
        private resultService: ResultService,
        private participationService: ParticipationService,
        private repositoryService: RepositoryService,
        private resultWebsocketService: ResultWebsocketService,
        private accountService: AccountService,
        private translate: TranslateService,
        private http: HttpClient,
        private modalService: NgbModal,
    ) {}

    ngOnInit(): void {
        if (this.result) {
            if (this.participation) {
                const exercise = this.participation.exercise;
                if (exercise && exercise.type === ExerciseType.PROGRAMMING) {
                    this.subscribeForProgramingExercise(exercise as ProgrammingExercise);
                }
            }
            this.init();
        } else if (this.participation && this.participation.id) {
            const exercise = this.participation.exercise;

            if (this.participation.results && this.participation.results.length > 0) {
                if (exercise && exercise.type === ExerciseType.MODELING) {
                    // sort results by completionDate descending to ensure the newest result is shown
                    // this is important for modeling exercises since students can have multiple tries
                    // think about if this should be used for all types of exercises
                    this.participation.results.sort((r1: Result, r2: Result) => {
                        if (r1.completionDate > r2.completionDate) {
                            return -1;
                        }
                        if (r1.completionDate < r2.completionDate) {
                            return 1;
                        }
                        return 0;
                    });
                }
                // Make sure result and participation are connected
                this.result = this.participation.results[0];
                this.result.participation = this.participation;
            }

            this.init();

            if (exercise && exercise.type === ExerciseType.PROGRAMMING) {
                this.subscribeForProgramingExercise(exercise as ProgrammingExercise);
            }
        }
    }

    subscribeForProgramingExercise(exercise: ProgrammingExercise) {
        this.accountService.identity().then(user => {
            // only subscribe for the currently logged in user or if the participation is a template/solution participation and the student is at least instructor
            if (
                (this.participation.student && user.id === this.participation.student.id && (exercise.dueDate == null || exercise.dueDate.isAfter(moment()))) ||
                (this.participation.student == null && this.accountService.isAtLeastInstructorInCourse(exercise.course))
            ) {
                // subscribe for new results (e.g. when a programming exercise was automatically tested)
                this.setupResultWebsocket();

                // subscribe for new submissions (e.g. when code was pushed and is currently built)
                this.websocketChannelSubmissions = `/topic/participation/${this.participation.id}/newSubmission`;
                this.jhiWebsocketService.subscribe(this.websocketChannelSubmissions);
                this.jhiWebsocketService.receive(this.websocketChannelSubmissions).subscribe((newProgrammingSubmission: ProgrammingSubmission) => {
                    // TODO handle this case properly, e.g. by animating a progress bar in the result view
                    console.log('Received new submission ' + newProgrammingSubmission.id + ': ' + newProgrammingSubmission.commitHash);
                });
            }
        });
    }

    private setupResultWebsocket() {
        if (this.resultSubscription) {
            this.resultSubscription.unsubscribe();
        }
        this.resultWebsocketService.subscribeResultForParticipation(this.participation.id).then(observable => {
            this.resultSubscription = observable
                .pipe(distinctUntilChanged(({ id: id1 }: Result, { id: id2 }: Result) => id1 === id2))
                .subscribe(result => this.handleNewResult(result));
        });
    }

    handleNewResult(newResult: Result) {
        if (newResult.rated !== undefined && newResult.rated !== null && newResult.rated === false) {
            // do not handle unrated results
            return;
        }
        this.result = newResult;
        // Reconnect the new result with the existing participation
        this.result.participation = this.participation;
        this.participation.results = [this.result];
        this.newResult.emit({
            newResult,
        });
        this.init();
    }

    init() {
        if (this.result && (this.result.score || this.result.score === 0) && (this.result.rated === true || this.result.rated == null || this.showUngradedResults)) {
            this.textColorClass = this.getTextColorClass();
            this.hasFeedback = this.getHasFeedback();
            this.resultIconClass = this.getResultIconClass();
            this.resultString = this.buildResultString();
        } else {
            // make sure that we do not display results that are 'rated=false' or that do not have a score
            this.result = null;
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.participation) {
            this.ngOnInit();
        }
    }

    ngOnDestroy() {
        if (this.resultSubscription) {
            this.resultSubscription.unsubscribe();
        }
        if (this.websocketChannelSubmissions) {
            this.jhiWebsocketService.unsubscribe(this.websocketChannelSubmissions);
        }
    }

    buildResultString() {
        if (this.result.resultString === 'No tests found') {
            return this.translate.instant('arTeMiSApp.editor.buildFailed');
        }
        return this.result.resultString;
    }

    getHasFeedback() {
        if (this.result.resultString === 'No tests found') {
            return true;
        } else if (this.result.hasFeedback === null) {
            return false;
        }
        return this.result.hasFeedback;
    }

    showDetails(result: Result) {
        if (!result.participation) {
            result.participation = this.participation;
        }
        const modalRef = this.modalService.open(ResultDetailComponent, { keyboard: true, size: 'lg' });
        modalRef.componentInstance.result = result;
    }

    downloadBuildResult(participationId: number) {
        this.participationService.downloadArtifact(participationId).subscribe(artifact => {
            const fileURL = URL.createObjectURL(artifact);
            const a = document.createElement('a');
            a.href = fileURL;
            a.target = '_blank';
            a.download = 'artifact';
            document.body.appendChild(a);
            a.click();
        });
    }

    /**
     * Get the css class for the entire text as a string
     *
     * @return {string} the css class
     */
    getTextColorClass() {
        if (this.result.score == null) {
            if (this.result.successful) {
                return 'text-success';
            }
            return 'text-danger';
        }
        if (this.result.score > MIN_POINTS_GREEN) {
            return 'text-success';
        }
        if (this.result.score > MIN_POINTS_ORANGE) {
            return 'result-orange';
        }
        return 'text-danger';
    }

    /**
     * Get the icon type for the result icon as an array
     *
     */
    getResultIconClass(): string[] {
        if (this.result.score == null) {
            if (this.result.successful) {
                return ['far', 'check-circle'];
            }
            return ['far', 'times-circle'];
        }
        if (this.result.score > 80) {
            return ['far', 'check-circle'];
        }
        return ['far', 'times-circle'];
    }
}
