import { Component, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { mapValues } from 'lodash';
import { DialogType } from 'app/shared/delete-dialog/delete-button.directive';

@Component({
    selector: 'jhi-delete-dialog',
    templateUrl: './delete-dialog.component.html',
})
export class DeleteDialogComponent implements OnInit {
    DialogType = DialogType;
    confirmEntityName: string;
    entityTitle: string;
    deleteQuestion: string;
    deleteConfirmationText: string;
    additionalChecks?: { [key: string]: string };
    additionalChecksValues: { [key: string]: boolean } = {};
    dialogType: DialogType = DialogType.Delete;

    // used by *ngFor in the template
    objectKeys = Object.keys;

    constructor(public activeModal: NgbActiveModal) {}

    ngOnInit(): void {
        if (this.additionalChecks) {
            this.additionalChecksValues = mapValues(this.additionalChecks, () => false);
        }
    }

    /**
     * Closes the dialog
     */
    clear() {
        this.activeModal.dismiss();
    }

    /**
     * Closes the dialog with a 'confirm' message, so the user of the service can use this message to delete the entity
     */
    confirmDelete() {
        this.activeModal.close(this.additionalChecksValues);
    }
}
