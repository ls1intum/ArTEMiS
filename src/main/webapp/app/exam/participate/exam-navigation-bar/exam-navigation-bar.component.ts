import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { Exercise } from 'app/entities/exercise.model';
import { LayoutService } from 'app/shared/breakpoints/layout.service';
import { CustomBreakpointNames } from 'app/shared/breakpoints/breakpoints.service';
import * as moment from 'moment';

@Component({
    selector: 'jhi-exam-navigation-bar',
    templateUrl: './exam-navigation-bar.component.html',
    styleUrls: ['./exam-navigation-bar.component.scss'],
})
export class ExamNavigationBarComponent implements OnInit {
    @Input()
    exercises: Exercise[] = [];

    @Input()
    endDate: string;

    @Output() onExerciseChanged = new EventEmitter<Exercise>();

    static itemsVisiblePerSideDefault = 4;

    exerciseIndex = 0;
    itemsVisiblePerSide = ExamNavigationBarComponent.itemsVisiblePerSideDefault;

    criticalTime = false;

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

    changeExercise(i: number) {
        // out of index -> do nothing
        if (i > this.exercises.length - 1 || i < 0) {
            return;
        }
        // set index and emit event
        this.exerciseIndex = i;
        this.onExerciseChanged.emit(this.exercises[i]);
    }

    submitExam() {
        this.changeExercise(this.exerciseIndex + 1);
    }

    get remainingTime(): string {
        const timeDiff = moment.duration(moment(this.endDate).diff(moment()));
        if (!this.criticalTime && timeDiff.asMinutes() < 5) {
            this.criticalTime = true;
        }
        return timeDiff.asMinutes() > 10
            ? Math.round(timeDiff.asMinutes()) + ' min'
            : Math.round(timeDiff.asSeconds() / 60)
                  .toString()
                  .padStart(2, '0') +
                  ' : ' +
                  Math.round(timeDiff.asSeconds() % 60)
                      .toString()
                      .padStart(2, '0');
    }
}
