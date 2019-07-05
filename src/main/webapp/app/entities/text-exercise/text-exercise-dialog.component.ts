import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { TextExercise } from './text-exercise.model';
import { TextExercisePopupService } from './text-exercise-popup.service';
import { TextExerciseService } from './text-exercise.service';
import { Course, CourseService } from '../course';

import { Subscription } from 'rxjs/Subscription';
import { ExerciseCategory, ExerciseService } from 'app/entities/exercise';
import { ExampleSubmissionService } from 'app/entities/example-submission/example-submission.service';

@Component({
    selector: 'jhi-text-exercise-dialog',
    templateUrl: './text-exercise-dialog.component.html',
    styleUrls: ['./text-exercise-dialog.scss'],
})
export class TextExerciseDialogComponent implements OnInit {
    textExercise: TextExercise;
    isSaving: boolean;
    maxScorePattern = '^[1-9]{1}[0-9]{0,4}$'; // make sure max score is a positive natural integer and not too large
    exerciseCategories: ExerciseCategory[];
    existingCategories: ExerciseCategory[];
    notificationText: string | null;

    courses: Course[];

    constructor(
        public activeModal: NgbActiveModal,
        private jhiAlertService: JhiAlertService,
        private textExerciseService: TextExerciseService,
        private exerciseService: ExerciseService,
        private courseService: CourseService,
        private eventManager: JhiEventManager,
        private exampleSubmissionService: ExampleSubmissionService,
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.notificationText = null;
        this.courseService.query().subscribe(
            (res: HttpResponse<Course[]>) => {
                this.courses = res.body!;
            },
            (res: HttpErrorResponse) => this.onError(res),
        );

        this.exerciseCategories = this.exerciseService.convertExerciseCategoriesFromServer(this.textExercise);
        this.courseService.findAllCategoriesOfCourse(this.textExercise.course!.id).subscribe(
            (res: HttpResponse<string[]>) => {
                this.existingCategories = this.exerciseService.convertExerciseCategoriesAsStringFromServer(res.body!);
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
    }

    clear() {
        this.activeModal.dismiss('cancel');
    }

    updateCategories(categories: ExerciseCategory[]) {
        this.textExercise.categories = categories.map(el => JSON.stringify(el));
    }

    save() {
        this.isSaving = true;
        if (this.textExercise.id !== undefined) {
            const requestOptions = {} as any;
            if (this.notificationText) {
                requestOptions.notificationText = this.notificationText;
            }
            this.subscribeToSaveResponse(this.textExerciseService.update(this.textExercise, requestOptions));
        } else {
            this.subscribeToSaveResponse(this.textExerciseService.create(this.textExercise));
        }
    }

    deleteExampleSubmission(id: number, index: number) {
        this.exampleSubmissionService.delete(id).subscribe(
            () => {
                this.textExercise.exampleSubmissions.splice(index, 1);
            },
            (error: HttpErrorResponse) => {
                this.jhiAlertService.error(error.message);
            },
        );
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<TextExercise>>) {
        result.subscribe((res: HttpResponse<TextExercise>) => this.onSaveSuccess(res.body!), (res: HttpErrorResponse) => this.onSaveError(res));
    }

    private onSaveSuccess(result: TextExercise) {
        this.eventManager.broadcast({ name: 'textExerciseListModification', content: 'OK' });
        this.isSaving = false;
        this.activeModal.dismiss(result);
    }

    private onSaveError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message, null, undefined);
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    trackCourseById(index: number, item: Course) {
        return item.id;
    }
}

@Component({
    selector: 'jhi-text-exercise-popup',
    template: '',
})
export class TextExercisePopupComponent implements OnInit, OnDestroy {
    routeSub: Subscription;

    constructor(private route: ActivatedRoute, private textExercisePopupService: TextExercisePopupService) {}

    ngOnInit() {
        this.routeSub = this.route.params.subscribe(params => {
            if (params['id']) {
                this.textExercisePopupService.open(TextExerciseDialogComponent as Component, params['id']);
            } else if (params['courseId']) {
                this.textExercisePopupService.open(TextExerciseDialogComponent as Component, undefined, params['courseId']);
            } else {
                this.textExercisePopupService.open(TextExerciseDialogComponent as Component);
            }
        });
    }

    ngOnDestroy() {
        this.routeSub.unsubscribe();
    }
}
