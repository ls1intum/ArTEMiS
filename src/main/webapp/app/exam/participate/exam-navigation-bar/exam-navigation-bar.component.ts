import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { LayoutService } from 'app/shared/breakpoints/layout.service';
import { CustomBreakpointNames } from 'app/shared/breakpoints/breakpoints.service';
import * as moment from 'moment';
import { Moment } from 'moment';
import { ArtemisServerDateService } from 'app/shared/server-date.service';
import { timer } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

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

    @Output() onExerciseChanged = new EventEmitter<Exercise>();

    static itemsVisiblePerSideDefault = 4;

    exerciseIndex = 0;
    itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;

    displayTime$ = timer(0, 100).pipe(
        map(() => this.updateDisplayTime()),
        distinctUntilChanged(),
    );

    criticalTime = false;
    icon: string;

    constructor(private layoutService: LayoutService, private serverDateService: ArtemisServerDateService) {}

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

    changeExercise(i: number) {
        // out of index -> do nothing
        if (i > this.exercises.length - 1 || i < 0) {
            return;
        }
        // set index and emit event
        this.exerciseIndex = i;
        this.onExerciseChanged.emit(this.exercises[i]);
        this.setExerciseButtonStatus(i);
    }

    saveExercise() {
        const newIndex = this.exerciseIndex + 1;
        this.exercises[this.exerciseIndex].studentParticipations[0].submissions[0].submitted = true;
        if (newIndex > this.exercises.length - 1) {
            // if out of range "change" active exercise to current in order to trigger a save
            this.changeExercise(this.exerciseIndex);
        } else {
            this.changeExercise(newIndex);
        }
    }

    updateDisplayTime() {
        const timeDiff = moment.duration(this.endDate.diff(this.serverDateService.now()));
        if (!this.criticalTime && timeDiff.asMinutes() < 5) {
            this.criticalTime = true;
        }
        return timeDiff.asMinutes() > 10
            ? Math.round(timeDiff.asMinutes()) + ' min'
            : timeDiff.minutes().toString().padStart(2, '0') + ' : ' + timeDiff.seconds().toString().padStart(2, '0') + ' min';
    }

    isProgrammingExercise() {
        return this.exercises[this.exerciseIndex].type === ExerciseType.PROGRAMMING;
    }

    setExerciseButtonStatus(i: number): string {
        let status: string;
        this.icon = 'edit';
        if (this.exercises[i].studentParticipations[0].submissions[0].submitted) {
            this.icon = 'check';
        }
        if (this.exercises[i].studentParticipations[0].submissions[0].isSynced) {
            // make button blue
            status = 'synced';
            if (i === this.exerciseIndex) {
                status = status + ' active';
                return status;
            }
            return status;
        } else {
            // make button yellow
            status = 'notSynced';
            return status;
        }
    }
}
