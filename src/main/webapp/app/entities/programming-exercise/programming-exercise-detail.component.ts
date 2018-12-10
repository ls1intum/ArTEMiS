import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ProgrammingExercise } from './programming-exercise.model';

import { NgxSpinnerService } from 'ngx-spinner';
import { ProgrammingExerciseService } from 'app/entities/programming-exercise/programming-exercise.service';

@Component({
    selector: 'jhi-programming-exercise-detail',
    templateUrl: './programming-exercise-detail.component.html'
})
export class ProgrammingExerciseDetailComponent implements OnInit {
    programmingExercise: ProgrammingExercise;

    constructor(
        private activatedRoute: ActivatedRoute,
        private programmingExerciseService: ProgrammingExerciseService,
        private spinner: NgxSpinnerService
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ programmingExercise }) => {
            this.programmingExercise = programmingExercise;
        });
    }

    previousState() {
        window.history.back();
    }

    generateStructureDiff() {
        this.spinner.show();
        setTimeout(() => {
            /** spinner ends after 8 seconds */
            this.spinner.hide();
        }, 8000);

        this.programmingExerciseService.generateTestDiff(this.programmingExercise.id);
        console.log(
            'Programming Exercise Detail Component: Called generate test diff method on the programming exercise ' +
                this.programmingExercise.id
        );
    }
}
