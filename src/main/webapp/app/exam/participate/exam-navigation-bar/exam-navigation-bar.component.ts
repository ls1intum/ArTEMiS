import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { LayoutService } from 'app/shared/breakpoints/layout.service';
import { CustomBreakpointNames } from 'app/shared/breakpoints/breakpoints.service';
import * as moment from 'moment';
import { Moment } from 'moment';
import { Submission } from 'app/entities/submission.model';

@Component({
    selector: 'jhi-exam-navigation-bar',
    templateUrl: './exam-navigation-bar.component.html',
    styleUrls: ['./exam-navigation-bar.component.scss'],
})
export class ExamNavigationBarComponent implements OnInit {
    @Input()
    exercises: Exercise[] = [];

    @Input()
    endDate: Moment;

    @Output() onExerciseChanged = new EventEmitter<{ exercise: Exercise; force: boolean }>();
    @Output() examAboutToEnd = new EventEmitter<void>();
    @Output() onExamHandInEarly = new EventEmitter<void>();

    static itemsVisiblePerSideDefault = 4;

    exerciseIndex = 0;
    itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;

    criticalTime = moment.duration(5, 'minutes');

    icon: string;

    constructor(private layoutService: LayoutService) {}

    ngOnInit(): void {
        this.layoutService.subscribeToLayoutChanges().subscribe(() => {
            // You will have all matched breakpoints in observerResponse
            if (this.layoutService.isBreakpointActive(CustomBreakpointNames.extraLarge)) {
                this.itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;
            } else if (this.layoutService.isBreakpointActive(CustomBreakpointNames.large)) {
                this.itemsVisiblePerSide = 3;
            } else if (this.layoutService.isBreakpointActive(CustomBreakpointNames.medium)) {
                this.itemsVisiblePerSide = 1;
            } else {
                this.itemsVisiblePerSide = 0;
            }
        });
    }

    triggerExamAboutToEnd() {
        this.saveExercise();
        this.examAboutToEnd.emit();
    }

    changeExercise(exerciseIndex: number, force: boolean) {
        // out of index -> do nothing
        if (exerciseIndex > this.exercises.length - 1 || exerciseIndex < 0) {
            return;
        }
        // set index and emit event
        this.exerciseIndex = exerciseIndex;
        this.onExerciseChanged.emit({ exercise: this.exercises[exerciseIndex], force });
        this.setExerciseButtonStatus(exerciseIndex);
    }

    saveExercise() {
        const newIndex = this.exerciseIndex + 1;
        const submission = this.getSubmissionForExercise(this.exercises[this.exerciseIndex]);
        // we do not submit programming exercises on a save
        if (submission && this.exercises[this.exerciseIndex].type !== ExerciseType.PROGRAMMING) {
            submission.submitted = true;
        }
        if (newIndex > this.exercises.length - 1) {
            // we are in the last exercise, if out of range "change" active exercise to current in order to trigger a save
            this.changeExercise(this.exerciseIndex, true);
        } else {
            this.changeExercise(newIndex, true);
        }
    }

    isProgrammingExercise() {
        return this.exercises[this.exerciseIndex].type === ExerciseType.PROGRAMMING;
    }

    setExerciseButtonStatus(exerciseIndex: number): 'synced' | 'synced active' | 'notSynced' {
        this.icon = 'edit';
        if (this.getSubmissionForExercise(this.exercises[exerciseIndex])) {
            const submission = this.exercises[exerciseIndex].studentParticipations[0].submissions[0];
            if (submission.submitted) {
                this.icon = 'check';
            }
            if (submission.isSynced) {
                // make button blue
                if (exerciseIndex === this.exerciseIndex) {
                    return 'synced active';
                } else {
                    return 'synced';
                }
            } else {
                // make button yellow
                this.icon = 'edit';
                return 'notSynced';
            }
        } else {
            // in case no participation yet exists -> display synced
            return 'synced';
        }
    }

    getExerciseButtonTooltip(exerciseIndex: number): 'submitted' | 'notSubmitted' | 'synced' | 'notSynced' {
        const submission = this.getSubmissionForExercise(this.exercises[exerciseIndex]);
        if (submission) {
            if (this.exercises[exerciseIndex].type === ExerciseType.PROGRAMMING) {
                if (submission.isSynced) {
                    return 'submitted';
                } else {
                    return 'notSubmitted';
                }
            } else {
                if (submission.isSynced) {
                    return 'synced';
                } else {
                    return 'notSynced';
                }
            }
        } else {
            // submission does not yet exist for this exercise.
            // When the participant navigates to the exercise the submissions are created.
            // Until then show, that the exercise is synced
            return 'synced';
        }
    }

    /**
     * Notify parent component when user wants to hand in early
     */
    handInEarly() {
        this.saveExercise();
        this.onExamHandInEarly.emit();
    }

    // TODO: find usages of similar logic -> put into utils method
    private getSubmissionForExercise(exercise: Exercise): Submission | null {
        if (
            exercise &&
            exercise.studentParticipations &&
            exercise.studentParticipations.length > 0 &&
            exercise.studentParticipations[0].submissions &&
            exercise.studentParticipations[0].submissions.length > 0
        ) {
            return exercise.studentParticipations[0].submissions[0];
        } else {
            return null;
        }
    }
}
