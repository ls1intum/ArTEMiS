import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService } from 'ng-jhipster';
import { Observable } from 'rxjs';

import { Course, CourseService } from 'app/entities/course';
import { ExerciseCategory, ExerciseService } from 'app/entities/exercise';

import { ProgrammingExercise, ProgrammingLanguage } from './programming-exercise.model';
import { ProgrammingExerciseService } from './programming-exercise.service';
import { FileService } from 'app/shared/http/file.service';

@Component({
    selector: 'jhi-programming-exercise-update',
    templateUrl: './programming-exercise-update.component.html',
})
export class ProgrammingExerciseUpdateComponent implements OnInit {
    readonly JAVA = ProgrammingLanguage.JAVA;
    readonly PYTHON = ProgrammingLanguage.PYTHON;

    programmingExercise: ProgrammingExercise;
    isSaving: boolean;
    problemStatementLoaded = false;

    maxScorePattern = '^[1-9]{1}[0-9]{0,4}$'; // make sure max score is a positive natural integer and not too large
    packageNamePattern = '^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$'; // package name must have at least 1 dot and must not start with a number
    shortNamePattern = '^[a-zA-Z][a-zA-Z0-9]*'; // must start with a letter and cannot contain special characters
    exerciseCategories: ExerciseCategory[];
    existingCategories: ExerciseCategory[];
    courses: Course[];

    constructor(
        private programmingExerciseService: ProgrammingExerciseService,
        private courseService: CourseService,
        private jhiAlertService: JhiAlertService,
        private exerciseService: ExerciseService,
        private fileService: FileService,
        private activatedRoute: ActivatedRoute,
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ programmingExercise }) => {
            this.programmingExercise = programmingExercise;
        });
        this.activatedRoute.params.subscribe(params => {
            if (params['courseId']) {
                const courseId = params['courseId'];
                this.courseService.find(courseId).subscribe(res => {
                    const course = res.body;
                    this.programmingExercise.course = course;
                    this.exerciseCategories = this.exerciseService.convertExerciseCategoriesFromServer(this.programmingExercise);
                    this.courseService.findAllCategoriesOfCourse(this.programmingExercise.course.id).subscribe(
                        (categoryRes: HttpResponse<string[]>) => {
                            this.existingCategories = this.exerciseService.convertExerciseCategoriesAsStringFromServer(categoryRes.body);
                        },
                        (categoryRes: HttpErrorResponse) => this.onError(categoryRes),
                    );
                });
            }
        });
        this.courseService.query().subscribe(
            (res: HttpResponse<Course[]>) => {
                this.courses = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
        // If an exercise is created, load our readme template so the problemStatement is not empty
        if (this.programmingExercise.id === undefined) {
            this.fileService.getTemplateFile('readme').subscribe(
                file => {
                    this.programmingExercise.problemStatement = file;
                    this.problemStatementLoaded = true;
                },
                err => {
                    this.programmingExercise.problemStatement = '';
                    this.problemStatementLoaded = true;
                    console.log('Error while getting template instruction file!', err);
                },
            );
        } else {
            this.problemStatementLoaded = true;
        }
    }

    /**
     * Update the problemStatement of the exercise with the data emitted by the markdown editor.
     * @param problemStatement
     */
    updateProblemStatement(problemStatement: string) {
        this.programmingExercise = { ...this.programmingExercise, problemStatement };
    }

    previousState() {
        window.history.back();
    }

    updateCategories(categories: ExerciseCategory[]) {
        this.programmingExercise.categories = categories.map(el => JSON.stringify(el));
    }

    save() {
        this.isSaving = true;
        if (this.programmingExercise.id !== undefined) {
            this.subscribeToSaveResponse(this.programmingExerciseService.update(this.programmingExercise));
        } else {
            this.subscribeToSaveResponse(this.programmingExerciseService.automaticSetup(this.programmingExercise));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<ProgrammingExercise>>) {
        result.subscribe((res: HttpResponse<ProgrammingExercise>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError(res));
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError(error: HttpErrorResponse) {
        const errorMessage = error.headers.get('X-arTeMiSApp-alert');
        // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
        const jhiAlert = this.jhiAlertService.error(errorMessage);
        jhiAlert.msg = errorMessage;
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    trackCourseById(index: number, item: Course) {
        return item.id;
    }
}
