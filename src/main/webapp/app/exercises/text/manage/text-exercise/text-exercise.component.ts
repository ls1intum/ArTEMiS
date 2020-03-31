import { Component, Input } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiEventManager } from 'ng-jhipster';
import { TextExercise } from 'app/entities/text-exercise.model';
import { TextExerciseService } from './text-exercise.service';
import { CourseExerciseService, CourseManagementService } from '../../../../course/manage/course-management.service';
import { ActivatedRoute } from '@angular/router';
import { ExerciseComponent } from 'app/exercises/shared/exercise/exercise.component';
import { TranslateService } from '@ngx-translate/core';
import { onError } from 'app/shared/util/global.utils';
import { AccountService } from 'app/core/auth/account.service';
import { AlertService } from 'app/core/alert/alert.service';

@Component({
    selector: 'jhi-text-exercise',
    templateUrl: './text-exercise.component.html',
})
export class TextExerciseComponent extends ExerciseComponent {
    @Input() textExercises: TextExercise[];

    constructor(
        private textExerciseService: TextExerciseService,
        private courseExerciseService: CourseExerciseService,
        courseService: CourseManagementService,
        translateService: TranslateService,
        private jhiAlertService: AlertService,
        eventManager: JhiEventManager,
        route: ActivatedRoute,
        private accountService: AccountService,
    ) {
        super(courseService, translateService, route, eventManager);
        this.textExercises = [];
    }

    protected loadExercises(): void {
        this.courseExerciseService.findAllTextExercisesForCourse(this.courseId).subscribe(
            (res: HttpResponse<TextExercise[]>) => {
                this.textExercises = res.body!;

                // reconnect exercise with course
                this.textExercises.forEach((exercise) => {
                    exercise.course = this.course;
                    exercise.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(exercise.course);
                    exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(exercise.course);
                });
                this.emitExerciseCount(this.textExercises.length);
            },
            (res: HttpErrorResponse) => onError(this.jhiAlertService, res),
        );
    }

    /**
     * Returns the unique identifier for items in the collection
     * @param index of a text exercise in the collection
     * @param item current text exercise
     */
    trackId(index: number, item: TextExercise) {
        return item.id;
    }

    /**
     * Deletes text exercise
     * @param textExerciseId id of the exercise that will be deleted
     */
    deleteTextExercise(textExerciseId: number) {
        this.textExerciseService.delete(textExerciseId).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'textExerciseListModification',
                    content: 'Deleted an textExercise',
                });
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
    }

    protected getChangeEventName(): string {
        return 'textExerciseListModification';
    }

    /**
     * Used in the template for jhiSort
     */
    callback() {}
}
