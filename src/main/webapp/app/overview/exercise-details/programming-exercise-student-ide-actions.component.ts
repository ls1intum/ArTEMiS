import { Component, HostBinding, Input, OnInit } from '@angular/core';
import { CourseExerciseService } from 'app/entities/course/course.service';
import { AlertService } from 'app/core/alert/alert.service';
import { SourceTreeService } from 'app/components/util/sourceTree.service';
import { ActivatedRoute } from '@angular/router';
import { FeatureToggle } from 'app/feature-toggle/feature-toggle.service';
import { Participation } from 'app/entities/participation/participation.model';
import { Exercise, ParticipationStatus } from 'app/entities/exercise/exercise.model';
import { isStartExerciseAvailable } from 'app/entities/exercise/exercise-utils';
import { ProgrammingExercise } from 'app/entities/programming-exercise/programming-exercise.model';
import { ProgrammingExerciseStudentParticipation } from 'app/entities/participation/programming-exercise-student-participation.model';
import { OrionState } from 'app/orion/orion';
import { OrionConnectorService } from 'app/orion/orion-connector.service';
import { OrionBuildAndTestService } from 'app/orion/orion-build-and-test.service';
import { catchError, filter, tap } from 'rxjs/operators';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { participationStatus } from 'app/entities/exercise/exercise-utils';

@Component({
    selector: 'jhi-programming-exercise-student-ide-actions',
    templateUrl: './programming-exercise-student-ide-actions.component.html',
    styleUrls: ['../course-overview.scss'],
    providers: [SourceTreeService],
})
export class ProgrammingExerciseStudentIdeActionsComponent implements OnInit {
    readonly UNINITIALIZED = ParticipationStatus.UNINITIALIZED;
    readonly INITIALIZED = ParticipationStatus.INITIALIZED;
    readonly INACTIVE = ParticipationStatus.INACTIVE;
    readonly participationStatus = participationStatus;
    ideState: OrionState;
    FeatureToggle = FeatureToggle;

    @Input() @HostBinding('class.col') equalColumns = true;
    @Input() @HostBinding('class.col-auto') smallColumns = false;

    @Input() exercise: Exercise;
    @Input() courseId: number;

    @Input() smallButtons: boolean;

    constructor(
        private jhiAlertService: AlertService,
        private courseExerciseService: CourseExerciseService,
        private javaBridge: OrionConnectorService,
        private ideBuildAndTestService: OrionBuildAndTestService,
        private route: ActivatedRoute,
    ) {}

    ngOnInit(): void {
        this.javaBridge.state().subscribe((ideState: OrionState) => (this.ideState = ideState));
        this.route.queryParams.subscribe(params => {
            if (params['withIdeSubmit']) {
                this.submitChanges();
            }
        });
    }

    /**
     * see exercise-utils -> isStartExerciseAvailable
     */
    isStartExerciseAvailable(): boolean {
        return isStartExerciseAvailable(this.exercise as ProgrammingExercise);
    }

    /**
     * Get the repo URL of a participation. Can be used to clone from the repo or push to it.
     *
     * @param participation The participation for which to get the repository URL
     * @return The URL of the remote repository in which the user's code referring the the current exercise is stored.
     */
    repositoryUrl(participation: Participation) {
        return (participation as ProgrammingExerciseStudentParticipation).repositoryUrl;
    }

    /**
     * Starts the exercise by initializing a new participation and creating a new personal repository.
     */
    startExercise() {
        this.exercise.loading = true;

        this.courseExerciseService
            .startExercise(this.courseId, this.exercise.id)
            .finally(() => (this.exercise.loading = false))
            .subscribe(
                participation => {
                    if (participation) {
                        this.exercise.studentParticipations = [participation];
                        this.exercise.participationStatus = this.participationStatus(this.exercise);
                    }
                    this.jhiAlertService.success('artemisApp.exercise.personalRepository');
                },
                error => {
                    console.log('Error: ' + error);
                    this.jhiAlertService.warning('artemisApp.exercise.startError');
                },
            );
    }

    /**
     * Imports the current exercise in the user's IDE and triggers the opening of the new project in the IDE
     */
    importIntoIDE() {
        const repo = this.repositoryUrl(this.exercise.studentParticipations[0]);
        this.javaBridge.importParticipation(repo, this.exercise as ProgrammingExercise);
    }

    /**
     * Submits the changes made in the IDE by staging everything, committing the changes and pushing them to master.
     */
    submitChanges() {
        this.javaBridge.submitChanges();
        this.ideBuildAndTestService.listenOnBuildOutputAndForwardChanges(this.exercise as ProgrammingExercise);
    }

    get canImport(): boolean {
        const notOpenedOrInstructor = this.ideState.inInstructorView || this.ideState.opened !== this.exercise.id;

        return this.hasInitializedParticipation() && notOpenedOrInstructor;
    }

    get canSubmit(): boolean {
        const openedAndNotInstructor = !this.ideState.inInstructorView && this.ideState.opened === this.exercise.id;

        return this.hasInitializedParticipation() && openedAndNotInstructor;
    }

    private hasInitializedParticipation(): boolean {
        return this.exercise.studentParticipations && this.participationStatus(this.exercise) === this.INITIALIZED && this.exercise.studentParticipations.length > 0;
    }

    resumeProgrammingExercise() {
        this.exercise.loading = true;
        this.courseExerciseService
            .resumeProgrammingExercise(this.courseId, this.exercise.id)
            .pipe(
                filter(Boolean),
                tap((participation: StudentParticipation) => {
                    participation.results = this.exercise.studentParticipations[0] ? this.exercise.studentParticipations[0].results : [];
                    this.exercise.studentParticipations = [participation];
                    this.exercise.participationStatus = participationStatus(this.exercise);
                }),
                catchError(error => {
                    console.log('Error: ' + error);
                    this.jhiAlertService.error('artemisApp.exerciseActions.resumeExercise', { error });
                    return error;
                }),
            )
            .finally(() => (this.exercise.loading = false))
            .subscribe();
    }
}
