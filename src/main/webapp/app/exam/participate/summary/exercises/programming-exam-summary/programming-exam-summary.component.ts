import { Component, Input } from '@angular/core';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { ProgrammingSubmission } from 'app/entities/programming-submission.model';
import { ProgrammingExerciseStudentParticipation } from 'app/entities/participation/programming-exercise-student-participation.model';

@Component({
    selector: 'jhi-programming-exam-summary',
    templateUrl: './programming-exam-summary.component.html',
    styles: [],
})
export class ProgrammingExamSummaryComponent {
    @Input()
    exercise: ProgrammingExercise;

    @Input()
    participation: ProgrammingExerciseStudentParticipation;

    @Input()
    submission: ProgrammingSubmission;
    constructor() {}
}
