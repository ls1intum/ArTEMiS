import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { Observable, of, Subscription, throwError } from 'rxjs';
import { isEmpty as _isEmpty } from 'lodash';
import { CodeEditorSubmissionService } from 'app/exercises/programming/shared/code-editor/service/code-editor-submission.service';
import { CodeEditorConflictStateService } from 'app/exercises/programming/shared/code-editor/service/code-editor-conflict-state.service';
import { CodeEditorResolveConflictModalComponent } from 'app/exercises/programming/shared/code-editor/actions/code-editor-resolve-conflict-modal.component';
import { FeatureToggle } from 'app/shared/feature-toggle/feature-toggle.service';
import { CodeEditorRepositoryFileService, CodeEditorRepositoryService } from 'app/exercises/programming/shared/code-editor/service/code-editor-repository.service';
import {
    CommitState,
    EditorState,
    FileSubmission,
    FileSubmissionError,
    GitConflictState,
    RepositoryError,
} from 'app/exercises/programming/shared/code-editor/model/code-editor.model';
import { CodeEditorConfirmRefreshModalComponent } from './code-editor-confirm-refresh-modal.component';

/**
 * Type guard for checking if the file submission received through the websocket is an error object.
 * @param toBeDetermined either a FileSubmission or a FileSubmissionError.
 */
const checkIfSubmissionIsError = (toBeDetermined: FileSubmission | FileSubmissionError): toBeDetermined is FileSubmissionError => {
    return !!(toBeDetermined as FileSubmissionError).error;
};

@Component({
    selector: 'jhi-code-editor-actions',
    templateUrl: './code-editor-actions.component.html',
})
export class CodeEditorActionsComponent implements OnInit, OnDestroy {
    CommitState = CommitState;
    EditorState = EditorState;
    FeatureToggle = FeatureToggle;

    @Input()
    buildable = true;
    @Input()
    unsavedFiles: { [fileName: string]: string };
    @Input() disableActions = false;
    @Input()
    get editorState() {
        return this.editorStateValue;
    }
    @Input()
    get commitState() {
        return this.commitStateValue;
    }

    @Output()
    commitStateChange = new EventEmitter<CommitState>();
    @Output()
    editorStateChange = new EventEmitter<EditorState>();
    @Output()
    isBuildingChange = new EventEmitter<boolean>();
    @Output()
    onSavedFiles = new EventEmitter<{ [fileName: string]: string | null }>();
    @Output()
    onError = new EventEmitter<string>();

    isBuilding: boolean;
    editorStateValue: EditorState;
    commitStateValue: CommitState;
    isResolvingConflict = false;

    conflictStateSubscription: Subscription;
    submissionSubscription: Subscription;

    set commitState(commitState: CommitState) {
        this.commitStateValue = commitState;
        this.commitStateChange.emit(commitState);
    }

    set editorState(editorState: EditorState) {
        this.editorStateValue = editorState;
        this.editorStateChange.emit(editorState);
    }

    constructor(
        private repositoryService: CodeEditorRepositoryService,
        private repositoryFileService: CodeEditorRepositoryFileService,
        private conflictService: CodeEditorConflictStateService,
        private modalService: NgbModal,
        private submissionService: CodeEditorSubmissionService,
    ) {}

    ngOnInit(): void {
        this.conflictStateSubscription = this.conflictService.subscribeConflictState().subscribe((gitConflictState: GitConflictState) => {
            // When the conflict is encountered when opening the code-editor, setting the commitState here could cause an uncheckedException.
            // This is why a timeout of 0 is set to make sure the template is rendered before setting the commitState.
            if (this.commitState === CommitState.CONFLICT && gitConflictState === GitConflictState.OK) {
                // Case a: Conflict was resolved.
                setTimeout(() => (this.commitState = CommitState.UNDEFINED), 0);
            } else if (this.commitState !== CommitState.CONFLICT && gitConflictState === GitConflictState.CHECKOUT_CONFLICT) {
                // Case b: Conflict has occurred.
                setTimeout(() => (this.commitState = CommitState.CONFLICT), 0);
            }
        });
        this.submissionSubscription = this.submissionService
            .getBuildingState()
            .pipe(tap((isBuilding: boolean) => (this.isBuilding = isBuilding)))
            .subscribe();
    }

    ngOnDestroy(): void {
        if (this.conflictStateSubscription) {
            this.conflictStateSubscription.unsubscribe();
        }
    }

    onRefresh() {
        if (this.editorState !== EditorState.CLEAN) {
            const modal = this.modalService.open(CodeEditorConfirmRefreshModalComponent, { keyboard: true, size: 'lg' });
            modal.componentInstance.shouldRefresh.subscribe(() => {
                this.executeRefresh();
            });
        } else {
            this.executeRefresh();
        }
    }

    executeRefresh() {
        this.editorState = EditorState.REFRESHING;
        setTimeout(() => {
            if (this.editorState === EditorState.REFRESHING) {
                this.editorState = EditorState.UNSAVED_CHANGES;
                const error = new Error('connectionTimeoutRefresh');
                this.onError.emit(error.message);
            }
        }, 8000);
        this.repositoryService.pull().subscribe(() => {
            this.unsavedFiles = {};
            this.editorState = EditorState.CLEAN;
        });
    }

    onSave() {
        this.saveChangedFilesWithTimeout()
            .pipe(catchError(() => of()))
            .subscribe();
    }

    /**
     * @function saveFiles
     * @desc Saves all files that have unsaved changes in the editor.
     */
    saveChangedFilesWithTimeout(): Observable<any> {
        if (!_isEmpty(this.unsavedFiles)) {
            setTimeout(() => {
                if (this.editorState === EditorState.SAVING) {
                    this.editorState = EditorState.UNSAVED_CHANGES;
                    const error = new Error('connectionTimeoutSave');
                    this.onError.emit(error.message);
                }
            }, 8000);
            this.editorState = EditorState.SAVING;
            const unsavedFiles = Object.entries(this.unsavedFiles).map(([fileName, fileContent]) => ({ fileName, fileContent }));
            this.saveFiles(unsavedFiles);
            return Observable.of(null);
        } else {
            return Observable.of(null);
        }
    }

    private saveFiles(fileUpdates: Array<{ fileName: string; fileContent: string }>) {
        this.repositoryFileService.updateFiles(fileUpdates).subscribe(
            (fileSubmission: FileSubmission | FileSubmissionError) => {
                if (checkIfSubmissionIsError(fileSubmission)) {
                    this.repositoryFileService.fileUpdateSubject.error(fileSubmission);
                    if (checkIfSubmissionIsError(fileSubmission) && fileSubmission.error === RepositoryError.CHECKOUT_CONFLICT) {
                        this.conflictService.notifyConflictState(GitConflictState.CHECKOUT_CONFLICT);
                    }
                    return;
                }
                this.onSavedFiles.emit(fileSubmission);
            },
            (err) => {
                this.onError.emit(err.error);
                this.editorState = EditorState.UNSAVED_CHANGES;
                throwError('savingFailed');
            },
        );
    }

    /**
     * @function commit
     * @desc Commits the current repository files.
     * If there are unsaved changes, save them first before trying to commit again.
     */
    commit() {
        // Avoid multiple commits at the same time.
        if (this.commitState === CommitState.COMMITTING) {
            return;
        }
        // If there are unsaved changes, save them before trying to commit again.
        Observable.of(null)
            .pipe(
                switchMap(() => (this.editorState === EditorState.UNSAVED_CHANGES ? this.saveChangedFilesWithTimeout() : Observable.of(null))),
                tap(() => (this.commitState = CommitState.COMMITTING)),
                switchMap(() => this.repositoryService.commit()),
                tap(() => {
                    this.commitState = CommitState.CLEAN;
                    // Note: this is not 100% clean, but not setting it here would complicate the state model.
                    // We just assume that after the commit a build happens if the repo is buildable.
                    if (this.buildable) {
                        this.isBuilding = true;
                    }
                }),
            )
            .subscribe(
                () => {},
                () => {
                    this.commitState = CommitState.UNCOMMITTED_CHANGES;
                    this.onError.emit('commitFailed');
                },
            );
    }

    resetRepository() {
        this.modalService.open(CodeEditorResolveConflictModalComponent, { keyboard: true, size: 'lg' });
    }
}
