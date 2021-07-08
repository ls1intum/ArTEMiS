import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { LayoutService } from 'app/shared/breakpoints/layout.service';
import { CustomBreakpointNames } from 'app/shared/breakpoints/breakpoints.service';
import * as moment from 'moment';
import { Moment } from 'moment';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { ExamExerciseUpdateService } from 'app/exam/manage/exam-exercise-update.service';
import { Subscription } from 'rxjs';
import { ExamParticipationService } from 'app/exam/participate/exam-participation.service';

@Component({
    selector: 'jhi-exam-navigation-bar',
    templateUrl: './exam-navigation-bar.component.html',
    styleUrls: ['./exam-navigation-bar.component.scss'],
})
export class ExamNavigationBarComponent implements OnInit {
    @Input() exercises: Exercise[] = [];
    @Input() exerciseIndex = 0;
    @Input() endDate: Moment;
    @Input() overviewPageOpen: boolean;

    @Output() onPageChanged = new EventEmitter<{ overViewChange: boolean; exercise?: Exercise; forceSave: boolean }>();
    @Output() examAboutToEnd = new EventEmitter<void>();
    @Output() onExamHandInEarly = new EventEmitter<void>();

    static itemsVisiblePerSideDefault = 4;
    itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;

    criticalTime = moment.duration(5, 'minutes');

    icon: IconProp;
    getExerciseButtonTooltip = this.examParticipationService.getExerciseButtonTooltip;

    subscriptionToLiveExamExerciseUpdates: Subscription;

    constructor(private layoutService: LayoutService, private examExerciseUpdateService: ExamExerciseUpdateService, private examParticipationService: ExamParticipationService) {}

    ngOnInit(): void {
        this.subscriptionToLiveExamExerciseUpdates = this.examExerciseUpdateService.currentExerciseIdAndProblemStatement.subscribe((update) => {
            if (update.problemStatement === '') {
                // another exercise will only be displayed if the student clicks on the corresponding pop-up notification
                this.changeExerciseById(update.exerciseId);
            } else {
                this.updateExerciseProblemStatementById(update.exerciseId, update.problemStatement);
            }
        });

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
        this.saveExercise(false);
        this.examAboutToEnd.emit();
    }

    /*
        @param exerciseIndex: exercise to switch to
        @param overviewPage: user wants to switch to the overview page
        @param forceSave: true if forceSave shall be used.
     */
    changePage(overviewPage: boolean, exerciseIndex?: number, forceSave?: boolean) {
        if (!overviewPage) {
            // out of index -> do nothing
            if (exerciseIndex! > this.exercises.length - 1 || exerciseIndex! < 0) {
                return;
            }
            // set index and emit event
            this.exerciseIndex = exerciseIndex!;
            this.onPageChanged.emit({ overViewChange: false, exercise: this.exercises[this.exerciseIndex], forceSave: !!forceSave });
        } else if (overviewPage) {
            // set index and emit event
            this.exerciseIndex = -1;
            // save current exercise
            this.onPageChanged.emit({ overViewChange: true, exercise: undefined, forceSave: false });
        }
        this.setExerciseButtonStatus(this.exerciseIndex);
    }

    /**
     * Auxiliary method to call changeExerciseByIndex based on the unique id of the exercise
     * @param exerciseId the unique identifier of an exercise that stays the same regardless of student exam ordering
     */
    changeExerciseById(exerciseId: number) {
        const foundIndex = this.exercises.findIndex((ex) => ex.id === exerciseId);
        this.changePage(false, foundIndex, true);
    }

    /**
     * Updates the problem statement of an exam exercise during an ongoing exam in real time,
     * i.e. the student will see the change immediately without the need to reload the page
     * @param exerciseId the unique exercise that needs to be updated
     * @param problemStatement the updated problem statement
     */
    updateExerciseProblemStatementById(exerciseId: number, problemStatement: string) {
        if (exerciseId !== -1 && problemStatement != undefined) {
            const foundIndex = this.exercises.findIndex((ex) => ex.id === exerciseId);
            this.exercises[foundIndex].problemStatement = problemStatement;
        }
    }

    /**
     * Save the currently active exercise and go to the next exercise.
     * @param changeExercise whether to go to the next exercise {boolean}
     */
    saveExercise(changeExercise = true) {
        const newIndex = this.exerciseIndex + 1;
        const submission = ExamParticipationService.getSubmissionForExercise(this.exercises[this.exerciseIndex]);
        // we do not submit programming exercises on a save
        if (submission && this.exercises[this.exerciseIndex].type !== ExerciseType.PROGRAMMING) {
            submission.submitted = true;
        }
        if (changeExercise) {
            if (newIndex > this.exercises.length - 1) {
                // we are in the last exercise, if out of range "change" active exercise to current in order to trigger a save
                this.changePage(false, this.exerciseIndex, true);
            } else {
                this.changePage(false, newIndex, true);
            }
        }
    }

    isProgrammingExercise() {
        return this.exercises[this.exerciseIndex].type === ExerciseType.PROGRAMMING;
    }

    isFileUploadExercise() {
        return this.exercises[this.exerciseIndex].type === ExerciseType.FILE_UPLOAD;
    }

    getOverviewStatus(): 'active' | '' {
        return this.overviewPageOpen ? 'active' : '';
    }

    setExerciseButtonStatus(exerciseIndex: number): 'synced' | 'synced active' | 'notSynced' {
        this.icon = 'edit';
        const exercise = this.exercises[exerciseIndex];
        const submission = ExamParticipationService.getSubmissionForExercise(exercise);
        if (submission) {
            if (submission.submitted) {
                this.icon = 'check';
            }
            if (submission.isSynced) {
                // make button blue
                if (exerciseIndex === this.exerciseIndex && !this.overviewPageOpen) {
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

    /**
     * Notify parent component when user wants to hand in early
     */
    handInEarly() {
        this.saveExercise(false);
        this.onExamHandInEarly.emit();
    }
}
