import { Component, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse } from '@angular/common/http';
import { DiagramType, ModelingExercise } from './modeling-exercise.model';
import { ModelingExerciseService } from './modeling-exercise.service';
import { DatePipe } from '@angular/common';
import { CourseService } from '../course';

@Injectable({ providedIn: 'root' })
export class ModelingExercisePopupService {
    private ngbModalRef: NgbModalRef | null;

    constructor(
        private datePipe: DatePipe,
        private modalService: NgbModal,
        private router: Router,
        private modelingExerciseService: ModelingExerciseService,
        private courseService: CourseService,
    ) {
        this.ngbModalRef = null;
    }

    open(component: Component, id?: number | any, courseId?: number): Promise<NgbModalRef> {
        return new Promise<NgbModalRef>((resolve, reject) => {
            if (this.ngbModalRef != null) {
                resolve(this.ngbModalRef);
            }

            if (id) {
                this.modelingExerciseService.find(id).subscribe((modelingExerciseResponse: HttpResponse<ModelingExercise>) => {
                    const modelingExercise: ModelingExercise = modelingExerciseResponse.body!;
                    this.ngbModalRef = this.modelingExerciseModalRef(component, modelingExercise);
                    resolve(this.ngbModalRef);
                });
            } else {
                // setTimeout used as a workaround for getting ExpressionChangedAfterItHasBeenCheckedError
                setTimeout(() => {
                    if (courseId) {
                        this.courseService.find(courseId).subscribe(res => {
                            const course = res.body!;
                            // class diagram is the default value and can be changed by the user in the creation dialog
                            this.ngbModalRef = this.modelingExerciseModalRef(component, new ModelingExercise(DiagramType.ClassDiagram, course));
                            resolve(this.ngbModalRef);
                        });
                    } else {
                        // class diagram is the default value and can be changed by the user in the creation dialog
                        this.ngbModalRef = this.modelingExerciseModalRef(component, new ModelingExercise(DiagramType.ClassDiagram));
                        resolve(this.ngbModalRef);
                    }
                }, 0);
            }
        });
    }

    modelingExerciseModalRef(component: Component, modelingExercise: ModelingExercise): NgbModalRef {
        const modalRef = this.modalService.open(component, { size: 'lg', backdrop: 'static' });
        modalRef.componentInstance.modelingExercise = modelingExercise;
        modalRef.result.then(
            result => {
                this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                this.ngbModalRef = null;
            },
            reason => {
                this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                this.ngbModalRef = null;
            },
        );
        return modalRef;
    }

    close() {
        if (this.ngbModalRef) {
            this.ngbModalRef.close();
        }
    }
}
