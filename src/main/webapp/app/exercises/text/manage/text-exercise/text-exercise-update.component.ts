import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';
import { TextExercise } from 'app/entities/text-exercise.model';
import { TextExerciseService } from './text-exercise.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { ExampleSubmissionService } from 'app/exercises/shared/example-submission/example-submission.service';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { AssessmentType } from 'app/entities/assessment-type.model';
import { Exercise, ExerciseMode, IncludedInOverallScore } from 'app/entities/exercise.model';
import { EditorMode } from 'app/shared/markdown-editor/markdown-editor.component';
import { KatexCommand } from 'app/shared/markdown-editor/commands/katex.command';
import { switchMap, tap } from 'rxjs/operators';
import { ExerciseGroupService } from 'app/exam/manage/exercise-groups/exercise-group.service';
import { NgForm } from '@angular/forms';
import { navigateBackFromExerciseUpdate, navigateToExampleSubmissions } from 'app/utils/navigation.utils';
import { ExerciseCategory } from 'app/entities/exercise-category.model';
import { cloneDeep } from 'lodash';
import { ExerciseUpdateWarningService } from 'app/exercises/shared/exercise-update-warning/exercise-update-warning.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { EditType } from 'app/exercises/shared/exercise/exercise-utils';

@Component({
    selector: 'jhi-text-exercise-update',
    templateUrl: './text-exercise-update.component.html',
    styleUrls: ['./text-exercise-update.scss'],
})
export class TextExerciseUpdateComponent implements OnInit {
    readonly IncludedInOverallScore = IncludedInOverallScore;

    @ViewChild('editForm') editForm: NgForm;

    examCourseId?: number;
    checkedFlag: boolean;
    isExamMode: boolean;
    isImport = false;
    EditorMode = EditorMode;
    AssessmentType = AssessmentType;

    textExercise: TextExercise;
    backupExercise: TextExercise;
    isSaving: boolean;
    exerciseCategories: ExerciseCategory[];
    existingCategories: ExerciseCategory[];
    notificationText?: string;

    domainCommandsProblemStatement = [new KatexCommand()];
    domainCommandsSampleSolution = [new KatexCommand()];
    domainCommandsGradingInstructions = [new KatexCommand()];

    constructor(
        private jhiAlertService: JhiAlertService,
        private textExerciseService: TextExerciseService,
        private modalService: NgbModal,
        private popupService: ExerciseUpdateWarningService,
        private exerciseService: ExerciseService,
        private exerciseGroupService: ExerciseGroupService,
        private courseService: CourseManagementService,
        private eventManager: JhiEventManager,
        private exampleSubmissionService: ExampleSubmissionService,
        private activatedRoute: ActivatedRoute,
        private router: Router,
    ) {}

    get editType(): EditType {
        if (this.isImport) {
            return EditType.IMPORT;
        }

        return this.textExercise.id == undefined ? EditType.CREATE : EditType.UPDATE;
    }

    /**
     * Initializes all relevant data for creating or editing text exercise
     */
    ngOnInit() {
        this.checkedFlag = false; // default value of grading instructions toggle

        // This is used to scroll page to the top of the page, because the routing keeps the position for the
        // new page from previous page.
        window.scroll(0, 0);

        // Get the textExercise
        this.activatedRoute.data.subscribe(({ textExercise }) => {
            this.textExercise = textExercise;
            this.backupExercise = cloneDeep(this.textExercise);
            this.examCourseId = this.textExercise.course?.id || this.textExercise.exerciseGroup?.exam?.course?.id;
        });

        this.activatedRoute.url
            .pipe(
                tap(
                    (segments) =>
                        (this.isImport = segments.some((segment) => segment.path === 'import', (this.isExamMode = segments.some((segment) => segment.path === 'exercise-groups')))),
                ),
                switchMap(() => this.activatedRoute.params),
                tap((params) => {
                    if (!this.isExamMode) {
                        this.exerciseCategories = this.textExercise.categories || [];
                        if (this.examCourseId) {
                            this.courseService.findAllCategoriesOfCourse(this.examCourseId).subscribe(
                                (categoryRes: HttpResponse<string[]>) => {
                                    this.existingCategories = this.exerciseService.convertExerciseCategoriesAsStringFromServer(categoryRes.body!);
                                },
                                (categoryRes: HttpErrorResponse) => this.onError(categoryRes),
                            );
                        }
                    } else {
                        // Lock individual mode for exam exercises
                        this.textExercise.mode = ExerciseMode.INDIVIDUAL;
                        this.textExercise.teamAssignmentConfig = undefined;
                        this.textExercise.teamMode = false;
                    }
                    if (this.isImport) {
                        if (this.isExamMode) {
                            // The target exerciseId where we want to import into
                            const exerciseGroupId = params['exerciseGroupId'];
                            const courseId = params['courseId'];
                            const examId = params['examId'];

                            this.exerciseGroupService.find(courseId, examId, exerciseGroupId).subscribe((res) => (this.textExercise.exerciseGroup = res.body!));
                            // We reference exam exercises by their exercise group, not their course. Having both would lead to conflicts on the server
                            this.textExercise.course = undefined;
                        } else {
                            // The target course where we want to import into
                            const targetCourseId = params['courseId'];
                            this.courseService.find(targetCourseId).subscribe((res) => (this.textExercise.course = res.body!));
                            // We reference normal exercises by their course, having both would lead to conflicts on the server
                            this.textExercise.exerciseGroup = undefined;
                        }
                        // Reset the due dates
                        this.textExercise.dueDate = undefined;
                        this.textExercise.releaseDate = undefined;
                        this.textExercise.assessmentDueDate = undefined;
                    }
                }),
            )
            .subscribe();
        this.isSaving = false;
        this.notificationText = undefined;
    }

    previousState() {
        navigateBackFromExerciseUpdate(this.router, this.textExercise);
    }

    /**
     * Validates if the date is correct
     */
    validateDate() {
        this.exerciseService.validateDate(this.textExercise);
    }

    /**
     * Updates the exercise categories
     * @param categories list of exercise categories
     */
    updateCategories(categories: ExerciseCategory[]) {
        this.textExercise.categories = categories;
    }

    save() {
        if (this.textExercise.gradingInstructionFeedbackUsed) {
            const ref = this.popupService.checkExerciseBeforeUpdate(this.textExercise, this.backupExercise);
            if (!this.modalService.hasOpenModals()) {
                this.saveExercise();
            } else {
                ref.then((reference) => {
                    reference.componentInstance.confirmed.subscribe(() => {
                        this.saveExercise();
                    });
                    reference.componentInstance.reEvaluated.subscribe(() => {
                        const requestOptions = {} as any;
                        requestOptions.deleteFeedback = reference.componentInstance.deleteFeedback;
                        this.subscribeToSaveResponse(this.textExerciseService.reevaluateAndUpdate(this.textExercise, requestOptions));
                    });
                });
            }
            return;
        }

        this.saveExercise();
    }

    /**
     * Sends a request to either update or create a text exercise
     */
    saveExercise() {
        Exercise.sanitize(this.textExercise);

        this.isSaving = true;

        switch (this.editType) {
            case EditType.IMPORT:
                this.subscribeToSaveResponse(this.textExerciseService.import(this.textExercise));
                break;
            case EditType.CREATE:
                this.subscribeToSaveResponse(this.textExerciseService.create(this.textExercise));
                break;
            case EditType.UPDATE:
                const requestOptions = {} as any;
                if (this.notificationText) {
                    requestOptions.notificationText = this.notificationText;
                }
                this.subscribeToSaveResponse(this.textExerciseService.update(this.textExercise, requestOptions));
                break;
        }
    }

    /**
     * Deletes example submission
     * @param id of the submission that will be deleted
     * @param index in the example submissions array
     */
    deleteExampleSubmission(id: number, index: number) {
        this.exampleSubmissionService.delete(id).subscribe(
            () => {
                this.textExercise.exampleSubmissions!.splice(index, 1);
            },
            (error: HttpErrorResponse) => {
                this.jhiAlertService.error(error.message);
            },
        );
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<TextExercise>>) {
        result.subscribe(
            (exercise: HttpResponse<TextExercise>) => this.onSaveSuccess(exercise.body!.id!),
            (res: HttpErrorResponse) => this.onSaveError(res),
        );
    }

    private onSaveSuccess(exerciseId: number) {
        this.eventManager.broadcast({ name: 'textExerciseListModification', content: 'OK' });
        this.isSaving = false;

        switch (this.editType) {
            case EditType.CREATE:
            case EditType.IMPORT:
                // Passing exerciseId since it is required for navigation to the example submission dashboard.
                navigateToExampleSubmissions(this.router, { ...this.textExercise, id: exerciseId });
                break;
            case EditType.UPDATE:
                this.previousState();
                break;
        }
    }

    private onSaveError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    /**
     * gets the flag of the structured grading instructions slide toggle
     */
    getCheckedFlag(event: boolean) {
        this.checkedFlag = event;
    }
}
