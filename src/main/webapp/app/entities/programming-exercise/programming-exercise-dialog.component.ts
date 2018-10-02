import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { ProgrammingExercise } from './programming-exercise.model';
import { ProgrammingExercisePopupService } from './programming-exercise-popup.service';
import { ProgrammingExerciseService } from './programming-exercise.service';
import { Course, CourseService } from '../course';

import { Subscription } from 'rxjs/Subscription';

@Component({
    selector: 'jhi-programming-exercise-dialog',
    templateUrl: './programming-exercise-dialog.component.html'
})
export class ProgrammingExerciseDialogComponent implements OnInit {

    programmingExercise: ProgrammingExercise;
    isSaving: boolean;

    courses: Course[];

    constructor(
        public activeModal: NgbActiveModal,
        private jhiAlertService: JhiAlertService,
        private programmingExerciseService: ProgrammingExerciseService,
        private courseService: CourseService,
        private eventManager: JhiEventManager
    ) {
    }

    ngOnInit() {
        this.isSaving = false;
        this.courseService.query()
            .subscribe((res: HttpResponse<Course[]>) => { this.courses = res.body; }, (res: HttpErrorResponse) => this.onError(res));
    }

    clear() {
        this.activeModal.dismiss('cancel');
    }

    save() {
        this.isSaving = true;
        if (this.programmingExercise.id !== undefined) {
            this.subscribeToSaveResponse(
                this.programmingExerciseService.update(this.programmingExercise));
        } else {
            this.subscribeToSaveResponse(
                this.programmingExerciseService.create(this.programmingExercise));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<ProgrammingExercise>>) {
        result.subscribe((res: HttpResponse<ProgrammingExercise>) =>
            this.onSaveSuccess(res.body), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess(result: ProgrammingExercise) {
        this.eventManager.broadcast({ name: 'programmingExerciseListModification', content: 'OK'});
        this.isSaving = false;
        this.activeModal.dismiss(result);
    }

    private onSaveError() {
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message, null, null);
    }

    trackCourseById(index: number, item: Course) {
        return item.id;
    }
}

@Component({
    selector: 'jhi-programming-exercise-popup',
    template: ''
})
export class ProgrammingExercisePopupComponent implements OnInit, OnDestroy {

    routeSub: Subscription;

    constructor(
        private route: ActivatedRoute,
        private programmingExercisePopupService: ProgrammingExercisePopupService
    ) {}

    ngOnInit() {
        this.routeSub = this.route.params.subscribe(params => {
            if ( params['id'] ) {
                this.programmingExercisePopupService
                    .open(ProgrammingExerciseDialogComponent as Component, params['id']);
            } else {
                if ( params['courseId'] ) {
                    this.programmingExercisePopupService
                        .open(ProgrammingExerciseDialogComponent as Component, undefined, params['courseId']);
                } else {
                    this.programmingExercisePopupService
                        .open(ProgrammingExerciseDialogComponent as Component);
                }
            }
        });
    }

    ngOnDestroy() {
        this.routeSub.unsubscribe();
    }
}
