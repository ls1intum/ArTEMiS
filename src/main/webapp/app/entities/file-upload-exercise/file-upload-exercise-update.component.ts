import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertService } from 'app/core/alert/alert.service';
import { FileUploadExerciseService } from './file-upload-exercise.service';
import { FileUploadExercise } from 'app/entities/file-upload-exercise/file-upload-exercise.model';
import { CourseService } from 'app/entities/course/course.service';
import { MAX_SCORE_PATTERN } from 'app/app.constants';
import { ExerciseService } from 'app/entities/exercise/exercise.service';
import { ExerciseCategory } from 'app/entities/exercise/exercise.model';
import { EditorMode } from 'app/markdown-editor/markdown-editor.component';
import { Course } from 'app/entities/course/course.model';
import { KatexCommand } from 'app/markdown-editor/commands/katex.command';

@Component({
    selector: 'jhi-file-upload-exercise-update',
    templateUrl: './file-upload-exercise-update.component.html',
    styleUrls: ['./file-upload-exercise-update.component.scss'],
})
export class FileUploadExerciseUpdateComponent implements OnInit {
    fileUploadExercise: FileUploadExercise;
    isSaving: boolean;
    maxScorePattern = MAX_SCORE_PATTERN;
    exerciseCategories: ExerciseCategory[];
    existingCategories: ExerciseCategory[];
    EditorMode = EditorMode;
    domainCommandsProblemStatement = [new KatexCommand()];
    domainCommandsSampleSolution = [new KatexCommand()];
    domainCommandsGradingInstructions = [new KatexCommand()];

    constructor(
        private fileUploadExerciseService: FileUploadExerciseService,
        private activatedRoute: ActivatedRoute,
        private courseService: CourseService,
        private exerciseService: ExerciseService,
        private jhiAlertService: AlertService,
        private router: Router,
    ) {}

    /**
     * Initializes information relevant to file upload exercise
     */
    ngOnInit() {
        // This is used to scroll page to the top of the page, because the routing keeps the position for the
        // new page from previous page.
        window.scroll(0, 0);

        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ fileUploadExercise }) => {
            this.fileUploadExercise = fileUploadExercise;
            this.exerciseCategories = this.exerciseService.convertExerciseCategoriesFromServer(this.fileUploadExercise);
            this.courseService.findAllCategoriesOfCourse(this.fileUploadExercise.course!.id).subscribe(
                (categoryRes: HttpResponse<string[]>) => {
                    this.existingCategories = this.exerciseService.convertExerciseCategoriesAsStringFromServer(categoryRes.body!);
                },
                (categoryRes: HttpErrorResponse) => this.onError(categoryRes),
            );
        });
    }

    /**
     * Returns to previous state, which should be always the page of selected course
     */
    previousState() {
        if (this.fileUploadExercise.course) {
            this.router.navigate(['/course', this.fileUploadExercise.course!.id]);
        } else {
            window.history.back();
        }
    }

    /**
     * Creates or updates file upload exercise
     */
    save() {
        this.isSaving = true;
        if (this.fileUploadExercise.id !== undefined) {
            this.subscribeToSaveResponse(this.fileUploadExerciseService.update(this.fileUploadExercise, this.fileUploadExercise.id));
        } else {
            this.subscribeToSaveResponse(this.fileUploadExerciseService.create(this.fileUploadExercise));
        }
    }
    /**
     * Validates if the date is correct
     */
    validateDate() {
        this.exerciseService.validateDate(this.fileUploadExercise);
    }
    /**
     * Updates categories for file upload exercise
     * @param categories list of exercies categories
     */
    updateCategories(categories: ExerciseCategory[]) {
        this.fileUploadExercise.categories = categories.map(el => JSON.stringify(el));
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<FileUploadExercise>>) {
        result.subscribe(
            (res: HttpResponse<FileUploadExercise>) => this.onSaveSuccess(),
            (res: HttpErrorResponse) => this.onSaveError(res),
        );
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError(error: HttpErrorResponse) {
        const errorMessage = error.headers.get('X-artemisApp-alert')!;
        // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
        const jhiAlert = this.jhiAlertService.error(errorMessage);
        jhiAlert.msg = errorMessage;
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }
}
