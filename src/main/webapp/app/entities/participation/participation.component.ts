import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { Participation } from './participation.model';
import { ParticipationService } from './participation.service';
import { ActivatedRoute } from '@angular/router';
import { areManualResultsAllowed, Exercise, ExerciseType } from '../exercise';
import { ExerciseService } from 'app/entities/exercise';
import { HttpErrorResponse } from '@angular/common/http';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { ProgrammingSubmissionService } from 'app/programming-submission/programming-submission.service';

@Component({
    selector: 'jhi-participation',
    templateUrl: './participation.component.html',
})
export class ParticipationComponent implements OnInit, OnDestroy {
    // make constants available to html for comparison
    readonly QUIZ = ExerciseType.QUIZ;
    readonly PROGRAMMING = ExerciseType.PROGRAMMING;
    readonly MODELING = ExerciseType.MODELING;

    participations: StudentParticipation[];
    eventSubscriber: Subscription;
    paramSub: Subscription;
    exercise: Exercise;
    predicate: string;
    reverse: boolean;
    newManualResultAllowed: boolean;

    hasLoadedPendingSubmissions = false;
    presentationScoreEnabled = false;

    constructor(
        private route: ActivatedRoute,
        private participationService: ParticipationService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private exerciseService: ExerciseService,
        private programmingSubmissionService: ProgrammingSubmissionService,
    ) {
        this.reverse = true;
        this.predicate = 'id';
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInParticipations();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    loadAll() {
        this.paramSub = this.route.params.subscribe(params => {
            this.hasLoadedPendingSubmissions = false;
            this.exerciseService.find(params['exerciseId']).subscribe(exerciseResponse => {
                this.exercise = exerciseResponse.body!;
                this.participationService.findAllParticipationsByExercise(params['exerciseId'], true).subscribe(participationsResponse => {
                    this.participations = participationsResponse.body!;
                });
                if (this.exercise.type === this.PROGRAMMING) {
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
            },
            error => this.onError(error),
        );
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    callback() {}
}
