import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { ProgrammingExercise } from './programming-exercise.model';
import { ProgrammingExerciseService } from './services/programming-exercise.service';
import { CourseExerciseService, CourseService } from '../course';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseComponent } from 'app/entities/exercise/exercise.component';
import { TranslateService } from '@ngx-translate/core';
import { AccountService } from 'app/core';
import { ExerciseService } from 'app/entities/exercise';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { ProgrammingAssessmentRepoExportDialogComponent } from 'app/programming-assessment/repo-export';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'jhi-programming-exercise',
    templateUrl: './programming-exercise.component.html',
})
export class ProgrammingExerciseComponent extends ExerciseComponent implements OnInit, OnDestroy {
    @Input() programmingExercises: ProgrammingExercise[];
    readonly ActionType = ActionType;
    closeDialogTrigger: boolean;

    constructor(
        private programmingExerciseService: ProgrammingExerciseService,
        private courseExerciseService: CourseExerciseService,
        private exerciseService: ExerciseService,
        private accountService: AccountService,
        protected jhiAlertService: JhiAlertService,
        private router: Router,
        private modalService: NgbModal,
        courseService: CourseService,
        translateService: TranslateService,
        eventManager: JhiEventManager,
        route: ActivatedRoute,
    ) {
        super(courseService, translateService, route, eventManager, jhiAlertService);
        this.programmingExercises = [];
    }

    protected loadExercises(): void {
        this.courseExerciseService.findAllProgrammingExercisesForCourse(this.courseId).subscribe(
            (res: HttpResponse<ProgrammingExercise[]>) => {
                this.programmingExercises = res.body!;
                // reconnect exercise with course
                this.programmingExercises.forEach(exercise => {
                    exercise.course = this.course;
                    exercise.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(exercise.course);
                    exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(exercise.course);
                });
                this.emitExerciseCount(this.programmingExercises.length);
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
    }

    trackId(index: number, item: ProgrammingExercise) {
        return item.id;
    }

    /**
     * Deletes programming exercise
     * @param programmingExerciseId the id of the programming exercise that we want to delete
     * @param $event contains additional checks for deleting exercise
     */
    deleteProgrammingExercise(programmingExerciseId: number, $event: { [key: string]: boolean }) {
        return this.programmingExerciseService.delete(programmingExerciseId, $event.deleteStudentReposBuildPlans, $event.deleteBaseReposBuildPlans).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'programmingExerciseListModification',
                    content: 'Deleted an programmingExercise',
                });
                this.closeDialogTrigger = !this.closeDialogTrigger;
            },
            (error: HttpErrorResponse) => this.onError(error),
        );
    }

    /**
     * Cleans up programming exercise
     * @param programmingExerciseId the id of the programming exercise that we want to delete
     * @param $event true if repositories should be deleted
     */
    cleanupProgrammingExercise(programmingExerciseId: number, $event: { [key: string]: boolean }) {
        this.exerciseService.cleanup(programmingExerciseId, $event.deleteRepositories).subscribe(
            () => {
                if ($event.deleteRepositories) {
                    this.jhiAlertService.success('artemisApp.programmingExercise.cleanup.successMessageWithRepositories');
                } else {
                    this.jhiAlertService.success('artemisApp.programmingExercise.cleanup.successMessage');
                }
                this.closeDialogTrigger = !this.closeDialogTrigger;
            },
            (error: HttpErrorResponse) => this.onError(error),
        );
    }

    /**
     * Resets programming exercise
     * @param programmingExerciseId the id of the programming exercise that we want to delete
     */
    resetProgrammingExercise(programmingExerciseId: number) {
        this.exerciseService.reset(programmingExerciseId).subscribe(() => (this.closeDialogTrigger = !this.closeDialogTrigger), (error: HttpErrorResponse) => this.onError(error));
    }

    openRepoExportDialog(programmingExerciseId: number, $event: MouseEvent) {
        $event.stopPropagation();
        const modalRef = this.modalService.open(ProgrammingAssessmentRepoExportDialogComponent, { keyboard: true, size: 'lg' });
        modalRef.componentInstance.exerciseId = programmingExerciseId;
    }

    protected getChangeEventName(): string {
        return 'programmingExerciseListModification';
    }

    callback() {}
}
