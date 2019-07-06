import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CodeEditorRepositoryFileService, CodeEditorRepositoryService, CodeEditorConflictStateService, GitConflictState } from 'app/code-editor/service';

@Component({
    selector: 'jhi-code-editor-resolve-conflict-modal',
    templateUrl: './code-editor-resolve-conflict-modal.component.html',
    styleUrls: ['./code-editor-resolve-conflict-modal.scss'],
    providers: [CodeEditorRepositoryFileService],
})
export class CodeEditorResolveConflictModalComponent {
    constructor(public activeModal: NgbActiveModal, private repositoryService: CodeEditorRepositoryService, private conflictService: CodeEditorConflictStateService) {}

    /**
     * @function deleteFile
     * @desc Reads the provided fileName and deletes the matching file in the repository
     */
    deleteFile() {
        this.repositoryService.resetRepository().subscribe(() => {
            this.conflictService.notifyConflictState(GitConflictState.OK);
            this.closeModal();
        });
    }

    closeModal() {
        this.activeModal.dismiss('cancel');
    }
}
