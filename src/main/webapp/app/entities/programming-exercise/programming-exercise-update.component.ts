import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService } from 'ng-jhipster';
import { Observable } from 'rxjs';

import { Course, CourseService } from 'app/entities/course';
import { ExerciseCategory, ExerciseService } from 'app/entities/exercise';

import { ProgrammingExercise, ProgrammingLanguage } from './programming-exercise.model';
import { ProgrammingExerciseService } from './services/programming-exercise.service';
import { FileService } from 'app/shared/http/file.service';
import { MAX_SCORE_PATTERN } from 'app/app.constants';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-programming-exercise-update',
    templateUrl: './programming-exercise-update.component.html',
    styleUrls: ['./programming-exercise-form.scss'],
})
export class ProgrammingExerciseUpdateComponent implements OnInit {
    readonly JAVA = ProgrammingLanguage.JAVA;
    readonly PYTHON = ProgrammingLanguage.PYTHON;

    private offeredLanguages = [this.JAVA, this.PYTHON];
    private translationBasePath = 'artemisApp.programmingExercise.';

    hashUnsavedChanges = false;
    programmingExercise: ProgrammingExercise;
    isSaving: boolean;
    problemStatementLoaded = false;
    templateParticipationResultLoaded = true;
    notificationText: string | null;
    selectedLanguage: ProgrammingLanguage;

    maxScorePattern = MAX_SCORE_PATTERN;
    packageNamePattern = '^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$'; // package name must have at least 1 dot and must not start with a number
    shortNamePattern = '^[a-zA-Z][a-zA-Z0-9]*'; // must start with a letter and cannot contain special characters
    titleNamePattern = '^[a-zA-Z0-9-_ ]+'; // must only contain alphanumeric characters, or whitespaces, or '_' or '-'
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
        private translateService: TranslateService,
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.notificationText = null;
        this.activatedRoute.data.subscribe(({ programmingExercise }) => {
            this.programmingExercise = programmingExercise;
        });
        this.activatedRoute.params.subscribe(params => {
            if (params['courseId']) {
                const courseId = params['courseId'];
                this.courseService.find(courseId).subscribe(res => {
                    const course = res.body!;
                    this.programmingExercise.course = course;
                    this.exerciseCategories = this.exerciseService.convertExerciseCategoriesFromServer(this.programmingExercise);
                    this.courseService.findAllCategoriesOfCourse(this.programmingExercise.course.id).subscribe(
                        (categoryRes: HttpResponse<string[]>) => {
                            this.existingCategories = this.exerciseService.convertExerciseCategoriesAsStringFromServer(categoryRes.body!);
                        },
                        (categoryRes: HttpErrorResponse) => this.onError(categoryRes),
                    );
                });
            }
        });
        this.courseService.query().subscribe(
            (res: HttpResponse<Course[]>) => {
                this.courses = res.body!;
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
        // If an exercise is created, load our readme template so the problemStatement is not empty
        if (this.programmingExercise.id === undefined) {
            this.onNewProgrammingLanguage(ProgrammingLanguage.JAVA);
        } else {
            this.problemStatementLoaded = true;
        }

        this.selectedLanguage = ProgrammingLanguage.JAVA;
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
            const requestOptions = {} as any;
            if (this.notificationText) {
                requestOptions.notificationText = this.notificationText;
            }
            this.subscribeToSaveResponse(this.programmingExerciseService.update(this.programmingExercise, requestOptions));
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
        const errorMessage = error.headers.get('X-artemisApp-alert')!;
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

    /**
     * Change the selected programming language for the current exercise. If there are unsaved changes, the user
     * will see a confirmation dialog about switching to a new template
     *
     * @param language The new programming language
     * @param languageSelect The <select> HTML element, which caused the language change
     */
    onNewProgrammingLanguage(language: ProgrammingLanguage, languageSelect?: HTMLSelectElement) {
        // If there are unsaved changes and the user does not confirm, the language doesn't get changed
        if (this.hashUnsavedChanges) {
            const confirmLanguageChangeText = this.translateService.instant(this.translationBasePath + 'unsavedChangesLanguageChange');
            if (!window.confirm(confirmLanguageChangeText)) {
                if (languageSelect) {
                    languageSelect.selectedIndex = this.offeredLanguages.indexOf(this.programmingExercise.programmingLanguage);
                }
                return;
            }
        }

        // Otherwise, just change the language and load the new template
        this.hashUnsavedChanges = false;
        this.problemStatementLoaded = false;
        this.programmingExercise.programmingLanguage = language;
        this.fileService.getTemplateFile('readme', this.programmingExercise.programmingLanguage).subscribe(
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
    }
}
