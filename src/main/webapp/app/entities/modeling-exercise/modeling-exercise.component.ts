import { Component, Input } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { ModelingExercise } from './modeling-exercise.model';
import { ModelingExerciseService } from './modeling-exercise.service';
import { AccountService } from '../../core';
import { CourseExerciseService } from '../course/course.service';
import { ActivatedRoute } from '@angular/router';
import { CourseService } from '../course';
import { ExerciseComponent } from 'app/entities/exercise/exercise.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-modeling-exercise',
    templateUrl: './modeling-exercise.component.html',
})
export class ModelingExerciseComponent extends ExerciseComponent {
    @Input() modelingExercises: ModelingExercise[];

    constructor(
        private modelingExerciseService: ModelingExerciseService,
        private courseExerciseService: CourseExerciseService,
        private jhiAlertService: JhiAlertService,
        private accountService: AccountService,
        courseService: CourseService,
        translateService: TranslateService,
        eventManager: JhiEventManager,
        route: ActivatedRoute,
    ) {
        super(courseService, translateService, route, eventManager);
        this.modelingExercises = [];
    }

    protected loadExercises(): void {
        this.courseExerciseService.findAllModelingExercisesForCourse(this.courseId).subscribe(
            (res: HttpResponse<ModelingExercise[]>) => {
                this.modelingExercises = res.body!;
                // reconnect exercise with course
                this.modelingExercises.forEach(exercise => {
                    exercise.course = this.course;
                    exercise.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(exercise.course);
                    exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(exercise.course);
                });
                this.emitExerciseCount(this.modelingExercises.length);
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
    }

    trackId(index: number, item: ModelingExercise) {
        return item.id;
    }

    protected getChangeEventName(): string {
        return 'modelingExerciseListModification';
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
        console.log('Error: ' + error);
    }

    callback() {}
}
