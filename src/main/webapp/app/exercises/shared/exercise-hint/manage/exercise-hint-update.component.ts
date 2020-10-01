import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable, of, Subscription } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { AlertService } from 'app/core/alert/alert.service';
import { ExerciseHint } from 'app/entities/exercise-hint.model';
import { ExerciseHintService } from './exercise-hint.service';
import { EditorMode, MarkdownEditorHeight } from 'app/shared/markdown-editor/markdown-editor.component';
import { Exercise } from 'app/entities/exercise.model';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { KatexCommand } from 'app/shared/markdown-editor/commands/katex.command';

@Component({
    selector: 'jhi-exercise-hint-update',
    templateUrl: './exercise-hint-update.component.html',
    styleUrls: ['./exercise-hint.scss'],
})
export class ExerciseHintUpdateComponent implements OnInit, OnDestroy {
    MarkdownEditorHeight = MarkdownEditorHeight;

    exerciseId: number;
    exerciseHint = new ExerciseHint();

    isSaving: boolean;
    isLoading: boolean;
    exerciseNotFound: boolean;
    paramSub: Subscription;

    domainCommands = [new KatexCommand()];
    editorMode = EditorMode.LATEX;

    constructor(
        private route: ActivatedRoute,
        protected jhiAlertService: AlertService,
        protected exerciseHintService: ExerciseHintService,
        protected exerciseService: ExerciseService,
    ) {}

    /**
     * Fetches the exercise from the server and assigns it on the exercise hint
     */
    ngOnInit() {
        this.isLoading = true;
        this.paramSub = this.route.params.subscribe((params) => {
            this.exerciseId = params['exerciseId'];
            this.isSaving = false;
            this.exerciseNotFound = false;
        });
        this.route.data.subscribe(({ exerciseHint }) => {
            this.exerciseHint = exerciseHint;
            // If the exercise was not yet created, load the exercise from the current route to set it as its exercise.
            if (!this.exerciseHint.id) {
                this.exerciseService
                    .find(this.exerciseId)
                    .pipe(
                        map(({ body }) => body),
                        tap((res: Exercise) => {
                            this.exerciseHint.exercise = res;
                        }),
                        catchError((res: HttpErrorResponse) => {
                            this.exerciseNotFound = true;
                            this.onError(res.message);
                            return of(null);
                        }),
                    )
                    .subscribe(() => {
                        this.isLoading = false;
                    });
            } else {
                this.isLoading = false;
            }
        });
    }

    /**
     * Unsubscribes from the param subscription
     */
    ngOnDestroy(): void {
        if (this.paramSub) {
            this.paramSub.unsubscribe();
        }
    }

    /**
     * Setter to update the exercise hint content
     * @param newContent New value to set
     */
    updateHintContent(newContent: string) {
        this.exerciseHint.content = newContent;
    }

    /**
     * Navigates back one step in the browser history
     */
    previousState() {
        window.history.back();
    }

    /**
     * Saves the exercise hint by creating or updating it on the server
     */
    save() {
        this.isSaving = true;
        if (this.exerciseHint.id !== undefined) {
            this.subscribeToSaveResponse(this.exerciseHintService.update(this.exerciseHint));
        } else {
            this.subscribeToSaveResponse(this.exerciseHintService.create(this.exerciseHint));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ExerciseHint>>) {
        result.subscribe(
            () => this.onSaveSuccess(),
            () => this.onSaveError(),
        );
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }
    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage);
    }
}
