import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { ModelingExercise } from './modeling-exercise.model';
import { ModelingExercisePopupService } from './modeling-exercise-popup.service';
import { ModelingExerciseService } from './modeling-exercise.service';
import { Course, CourseService } from '../course';
import * as moment from 'moment';

@Component({
    selector: 'jhi-modeling-exercise-dialog',
    templateUrl: './modeling-exercise-dialog.component.html'
})
export class ModelingExerciseDialogComponent implements OnInit {

    modelingExercise: ModelingExercise;
    isSaving: boolean;
    releaseDate: Date;
    dueDate: Date;
    releaseClockToggled: boolean;
    dueClockToggled: boolean;

    courses: Course[];

    constructor(
        public activeModal: NgbActiveModal,
        private jhiAlertService: JhiAlertService,
        private modelingExerciseService: ModelingExerciseService,
        private courseService: CourseService,
        private eventManager: JhiEventManager
    ) {
    }

    ngOnInit() {
        this.isSaving = false;
        this.courseService.query()
            .subscribe((res: HttpResponse<Course[]>) => { this.courses = res.body; }, (res: HttpResponse<Course[]>) => this.onError(res.body));
        this.releaseDate = new Date(this.modelingExercise.releaseDate || undefined);
        this.dueDate = new Date(this.modelingExercise.dueDate || undefined);
        this.releaseClockToggled = false;
        this.dueClockToggled = false;
    }

    clear() {
        this.activeModal.dismiss('cancel');
    }

    toggleClock(input: string) {
      switch ( input ) {
        case 'releaseDate':
          this.releaseClockToggled = !this.releaseClockToggled;
          break;
        case 'dueDate':
          this.dueClockToggled = !this.dueClockToggled;
          break;
      }
    }

    save() {
        this.isSaving = true;
        this.modelingExercise.type = 'modeling-exercise';
        this.modelingExercise.releaseDate = moment(this.releaseDate).format();
        this.modelingExercise.dueDate = moment(this.dueDate).format();
        if (this.modelingExercise.id !== undefined) {
            this.subscribeToSaveResponse(
                this.modelingExerciseService.update(this.modelingExercise));
        } else {
            this.subscribeToSaveResponse(
                this.modelingExerciseService.create(this.modelingExercise));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<ModelingExercise>>) {
        result.subscribe((res: HttpResponse<ModelingExercise>) =>
            this.onSaveSuccess(res.body), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess(result: ModelingExercise) {
        this.eventManager.broadcast({ name: 'modelingExerciseListModification', content: 'OK'});
        this.isSaving = false;
        this.activeModal.dismiss(result);
    }

    private onSaveError() {
        this.isSaving = false;
    }

    private onError(error: any) {
        this.jhiAlertService.error(error.message, null, null);
    }

    trackCourseById(index: number, item: Course) {
        return item.id;
    }
}

@Component({
    selector: 'jhi-modeling-exercise-popup',
    template: ''
})
export class ModelingExercisePopupComponent implements OnInit, OnDestroy {

    routeSub: any;

    constructor(
        private route: ActivatedRoute,
        private modelingExercisePopupService: ModelingExercisePopupService
    ) {}

    ngOnInit() {
        this.routeSub = this.route.params.subscribe(params => {
            if ( params['id'] ) {
                this.modelingExercisePopupService
                    .open(ModelingExerciseDialogComponent as Component, params['id']);
            } else {
                if ( params['courseId'] ) {
                    this.modelingExercisePopupService
                        .open(ModelingExerciseDialogComponent as Component, undefined, params['courseId']);
                } else {
                    this.modelingExercisePopupService
                        .open(ModelingExerciseDialogComponent as Component);
                }
            }
        });
    }

    ngOnDestroy() {
        this.routeSub.unsubscribe();
    }
}
