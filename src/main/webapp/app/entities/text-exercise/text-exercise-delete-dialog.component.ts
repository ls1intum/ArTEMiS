import { Component } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { TextExercise } from './text-exercise.model';
import { TextExerciseService } from './text-exercise.service';

@Component({
    selector: 'jhi-text-exercise-delete-dialog',
    templateUrl: './text-exercise-delete-dialog.component.html',
})
export class TextExerciseDeleteDialogComponent {
    textExercise: TextExercise;
    confirmExerciseName: string;

    constructor(private textExerciseService: TextExerciseService, public activeModal: NgbActiveModal, private eventManager: JhiEventManager) {}

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
    confirmDelete(id: number) {
        this.textExerciseService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'textExerciseListModification',
                content: 'Deleted an textExercise',
            });
            this.activeModal.dismiss(true);
        });
    }
}
