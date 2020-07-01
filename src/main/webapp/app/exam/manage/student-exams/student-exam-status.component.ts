import { Component, Input } from '@angular/core';

/**
 * Status indicator for student exams
 * Number of student exams should match the number of registered users
 */
@Component({
    selector: 'jhi-student-exam-status',
    template: `
        <div class="d-flex">
            <div *ngIf="hasStudentsWithoutExam; else allStudentsHaveExams" class="d-flex align-items-center badge badge-warning mb-1">
                <fa-icon class="ml-2 text-white" icon="exclamation-triangle" [ngbTooltip]="'artemisApp.studentExams.studentExamStatusWarningTooltip' | translate"> </fa-icon>
                <span class="ml-1" jhiTranslate="artemisApp.studentExams.studentExamStatusWarning"></span>
            </div>
            <ng-template #allStudentsHaveExams>
                <div class="d-flex align-items-center badge badge-success mb-1">
                    <fa-icon class="ml-2 text-white" icon="check-circle"></fa-icon>
                    <span class="ml-1" jhiTranslate="artemisApp.studentExams.studentExamStatusSuccess"></span>
                </div>
            </ng-template>
        </div>
    `,
})
export class StudentExamStatusComponent {
    @Input() hasStudentsWithoutExam: boolean;
}
