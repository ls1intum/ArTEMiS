import { Component, Input } from '@angular/core';

/**
 * Two status indicators for the test case table:
 * - Are there unsaved changes?
 * - Have test cases been changed but the student submissions were not triggered?
 */
@Component({
    selector: 'jhi-programming-exercise-manage-test-cases-status',
    template: `
        <div class="d-flex flex-column justify-content-between">
            <div *ngIf="hasUnsavedChanges; else noUnsavedChanges" class="d-flex align-items-center badge badge-warning mb-1">
                <fa-icon class="ml-2 text-white" icon="exclamation-triangle"></fa-icon>
                <span class="ml-1" jhiTranslate="artemisApp.programmingExercise.manageTestCases.unsavedChanges"></span>
            </div>
            <ng-template #noUnsavedChanges>
                <div class="d-flex align-items-center badge badge-success mb-1">
                    <fa-icon class="ml-2 text-white" icon="check-circle"></fa-icon>
                    <span class="ml-1" jhiTranslate="artemisApp.programmingExercise.manageTestCases.noUnsavedChanges"></span>
                </div>
            </ng-template>
            <div class="d-flex align-items-center badge badge-warning" *ngIf="hasUpdatedTestCases; else noUpdatedTestCases">
                <fa-icon
                    class="ml-2 text-white"
                    icon="exclamation-triangle"
                    [ngbTooltip]="'artemisApp.programmingExercise.manageTestCases.updatedTestCasesTooltip' | translate"
                ></fa-icon>
                <span class="ml-1" jhiTranslate="artemisApp.programmingExercise.manageTestCases.updatedTestCasesShort"></span>
            </div>
            <ng-template #noUpdatedTestCases>
                <div class="d-flex align-items-center badge badge-success">
                    <fa-icon class="ml-2 text-white" icon="check-circle"></fa-icon>
                    <span class="ml-1" jhiTranslate="artemisApp.programmingExercise.manageTestCases.noUpdatedTestCases"></span>
                </div>
            </ng-template>
        </div>
    `,
})
export class ProgrammingExerciseManageTestCasesStatusComponent {
    @Input() hasUnsavedChanges: boolean;
    @Input() hasUpdatedTestCases: boolean;
}
