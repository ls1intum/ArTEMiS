import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { LayoutService } from 'app/shared/breakpoints/layout.service';
import { CustomBreakpointNames } from 'app/shared/breakpoints/breakpoints.service';
import * as moment from 'moment';
import { Moment } from 'moment';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

@Component({
    selector: 'jhi-exam-navigation-bar',
    templateUrl: './exam-navigation-bar.component.html',
    styleUrls: ['./exam-navigation-bar.component.scss'],
})
export class ExamNavigationBarComponent implements OnInit {
    @Input() exercises: Exercise[] = [];
    @Input() pageIndex = 0;
    @Input() endDate: Moment;
    @Input() overviewPageOpen: boolean;

    @Output() onPageChanged = new EventEmitter<{ overViewChange: boolean; exercise: Exercise; forceSave: boolean }>();
    @Output() examAboutToEnd = new EventEmitter<void>();
    @Output() onExamHandInEarly = new EventEmitter<void>();

    static itemsVisiblePerSideDefault = 4;
    itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;

    criticalTime = moment.duration(5, 'minutes');

    icon: IconProp;

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
        console.log(this.pageIndex);
    }

    triggerExamAboutToEnd() {
        this.saveExercise(false);
        this.examAboutToEnd.emit();
    }

    openExerciseOverview() {
        // set index and emit event
        this.pageIndex = 0;
        // save current exercise
        this.onPageChanged.emit({ overViewChange: true, exercise: this.exercises[this.pageIndex], forceSave: false });
        this.setExerciseButtonStatus(this.pageIndex);
    }

    changeExercise(exerciseIndex: number, forceSave: boolean) {
        // out of index -> do nothing
        if (exerciseIndex > this.exercises.length - 1 || exerciseIndex < 0) {
            return;
        }
        // set index and emit event
        this.pageIndex = exerciseIndex;
        this.onPageChanged.emit({ overViewChange: false, exercise: this.exercises[exerciseIndex], forceSave });
        this.setExerciseButtonStatus(exerciseIndex);
    }

    /**
     * Save the currently active exercise and go to the next exercise.
     * @param changeExercise whether to go to the next exercise {boolean}
     */
    saveExercise(changeExercise = true) {
        const newIndex = this.pageIndex + 1;
        const submission = this.getSubmissionForExercise(this.exercises[this.pageIndex]);
        // we do not submit programming exercises on a save
        if (submission && this.exercises[this.pageIndex].type !== ExerciseType.PROGRAMMING) {
            submission.submitted = true;
        }
        if (changeExercise) {
            if (newIndex > this.exercises.length - 1) {
                // we are in the last exercise, if out of range "change" active exercise to current in order to trigger a save
                this.changeExercise(this.pageIndex, true);
            } else {
                this.changeExercise(newIndex, true);
            }
        }
    }

    isProgrammingExercise() {
        return this.exercises[this.pageIndex].type === ExerciseType.PROGRAMMING;
    }

    isFileUploadExercise() {
        return this.exercises[this.pageIndex].type === ExerciseType.FILE_UPLOAD;
    }

    setOverviewStatus(): 'active' | '' {
        return this.overviewPageOpen ? 'active' : '';
    }

    setExerciseButtonStatus(exerciseIndex: number): 'synced' | 'synced active' | 'notSynced' {
        this.icon = 'edit';
        const exercise = this.exercises[exerciseIndex];
        const submission = this.getSubmissionForExercise(exercise);
        if (submission) {
            if (submission.submitted) {
                this.icon = 'check';
            }
            if (submission.isSynced) {
                // make button blue
                if (exerciseIndex === this.pageIndex && !this.overviewPageOpen) {
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

    getExerciseButtonTooltip(exerciseIndex: number): 'submitted' | 'notSubmitted' | 'synced' | 'notSynced' | 'notSavedOrSubmitted' {
        const submission = this.getSubmissionForExercise(this.exercises[exerciseIndex]);
        if (submission) {
            if (this.exercises[exerciseIndex].type === ExerciseType.PROGRAMMING) {
                if (submission.submitted && submission.isSynced) {
                    return 'submitted'; // You have submitted an exercise. You can submit again
                } else if (submission.submitted && !submission.isSynced) {
                    return 'notSavedOrSubmitted'; // You have unsaved and/or unsubmitted changes
                } else if (!submission.submitted && submission.isSynced) {
                    return 'notSubmitted'; // starting point
                } else {
                    return 'notSavedOrSubmitted';
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
        this.saveExercise(false);
        this.onExamHandInEarly.emit();
    }

    // TODO: find usages of similar logic -> put into utils method
    getSubmissionForExercise(exercise: Exercise) {
        if (
            exercise &&
            exercise.studentParticipations &&
            exercise.studentParticipations.length > 0 &&
            exercise.studentParticipations[0].submissions &&
            exercise.studentParticipations[0].submissions.length > 0
        ) {
            return exercise.studentParticipations[0].submissions[0];
        } else {
            return undefined;
        }
    }
}
