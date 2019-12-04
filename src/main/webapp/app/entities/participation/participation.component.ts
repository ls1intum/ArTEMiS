import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { Participation } from './participation.model';
import { ParticipationService } from './participation.service';
import { ActivatedRoute } from '@angular/router';
import { areManualResultsAllowed, Exercise, ExerciseType } from '../exercise';
import { ExerciseService } from 'app/entities/exercise';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { ProgrammingSubmissionService } from 'app/programming-submission/programming-submission.service';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { FeatureToggle } from 'app/feature-toggle';

@Component({
    selector: 'jhi-participation',
    templateUrl: './participation.component.html',
})
export class ParticipationComponent implements OnInit, OnDestroy {
    // make constants available to html for comparison
    readonly ExerciseType = ExerciseType;
    readonly ActionType = ActionType;
    readonly FeatureToggle = FeatureToggle;

    participations: StudentParticipation[] = [];
    eventSubscriber: Subscription;
    paramSub: Subscription;
    exercise: Exercise;
    newManualResultAllowed: boolean;

    hasLoadedPendingSubmissions = false;
    presentationScoreEnabled = false;

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    isLoading: boolean;

    constructor(
        private route: ActivatedRoute,
        private participationService: ParticipationService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private exerciseService: ExerciseService,
        private programmingSubmissionService: ProgrammingSubmissionService,
    ) {}

    ngOnInit() {
        this.loadAll();
        this.registerChangeInParticipations();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
        this.dialogErrorSource.unsubscribe();
    }

    loadAll() {
        this.paramSub = this.route.params.subscribe(params => {
            this.isLoading = true;
            this.hasLoadedPendingSubmissions = false;
            this.exerciseService.find(params['exerciseId']).subscribe(exerciseResponse => {
                this.exercise = exerciseResponse.body!;
                this.participationService.findAllParticipationsByExercise(params['exerciseId'], true).subscribe(participationsResponse => {
                    this.participations = participationsResponse.body!;
                    this.isLoading = false;
                });
                if (this.exercise.type === ExerciseType.PROGRAMMING) {
                    this.programmingSubmissionService.getSubmissionStateOfExercise(this.exercise.id).subscribe(() => (this.hasLoadedPendingSubmissions = true));
                }
                this.newManualResultAllowed = areManualResultsAllowed(this.exercise);
                this.presentationScoreEnabled = this.checkPresentationScoreConfig();
            });
        });
    }

    trackId(index: number, item: Participation) {
        return item.id;
    }

    registerChangeInParticipations() {
        this.eventSubscriber = this.eventManager.subscribe('participationListModification', () => this.loadAll());
    }

    checkPresentationScoreConfig(): boolean {
        if (!this.exercise.course) {
            return false;
        }
        return this.exercise.isAtLeastTutor && this.exercise.course.presentationScore !== 0 && this.exercise.presentationScoreEnabled;
    }

    addPresentation(participation: StudentParticipation) {
        if (!this.presentationScoreEnabled) {
            return;
        }
        participation.presentationScore = 1;
        this.participationService.update(participation).subscribe(
            () => {},
            () => {
                this.jhiAlertService.error('artemisApp.participation.addPresentation.error');
            },
        );
    }

    removePresentation(participation: StudentParticipation) {
        if (!this.presentationScoreEnabled) {
            return;
        }
        participation.presentationScore = 0;
        this.participationService.update(participation).subscribe(
            () => {},
            () => {
                this.jhiAlertService.error('artemisApp.participation.removePresentation.error');
            },
        );
    }

    /**
     * Deletes participation
     * @param participationId the id of the participation that we want to delete
     * @param $event passed from delete dialog to represent if checkboxes were checked
     */
    deleteParticipation(participationId: number, $event: { [key: string]: boolean }) {
        const deleteBuildPlan = $event.deleteBuildPlan ? $event.deleteBuildPlan : false;
        const deleteRepository = $event.deleteRepository ? $event.deleteRepository : false;
        this.participationService.delete(participationId, { deleteBuildPlan, deleteRepository }).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'participationListModification',
                    content: 'Deleted an participation',
                });
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
    }
    /**
     * Cleans programming exercise participation
     * @param programmingExerciseParticipation the id of the participation that we want to delete
     */
    cleanupProgrammingExerciseParticipation(programmingExerciseParticipation: StudentParticipation) {
        this.participationService.cleanupBuildPlan(programmingExerciseParticipation).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'participationListModification',
                    content: 'Cleanup the build plan of an participation',
                });
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
    }

    /**
     * Formats the results in the autocomplete overlay.
     *
     * @param participation
     */
    searchResultFormatter = (participation: StudentParticipation) => {
        const { login, name } = participation.student;
        return `${login} (${name})`;
    };

    /**
     * Converts a participation object to a string that can be searched for. This is
     * used by the autocomplete select inside the data table.
     *
     * @param participation Student participation
     */
    searchTextFromParticipation = (participation: StudentParticipation): string => {
        return participation.student.login || '';
    };
}
