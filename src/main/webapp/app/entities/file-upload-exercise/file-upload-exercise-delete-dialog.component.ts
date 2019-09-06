import { Component } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { FileUploadExercise } from './file-upload-exercise.model';
import { FileUploadExerciseService } from './file-upload-exercise.service';

@Component({
    selector: 'jhi-file-upload-exercise-delete-dialog',
    templateUrl: './file-upload-exercise-delete-dialog.component.html',
})
export class FileUploadExerciseDeleteDialogComponent {
    fileUploadExercise: FileUploadExercise;
    confirmExerciseName: string;

    constructor(private fileUploadExerciseService: FileUploadExerciseService, public activeModal: NgbActiveModal, private eventManager: JhiEventManager) {}

    /**
     * Closes the dialog
     */
    clear() {
        this.activeModal.dismiss('cancel');
    }

    /**
     * Deletes specified file upload exercise and closes the dialog
     * @param exerciseId
     */
    confirmDelete(exerciseId: number) {
        this.fileUploadExerciseService.delete(exerciseId).subscribe(response => {
            this.eventManager.broadcast({
                name: 'fileUploadExerciseListModification',
                content: 'Deleted an fileUploadExercise',
            });
            this.activeModal.dismiss(true);
        });
    }
}
