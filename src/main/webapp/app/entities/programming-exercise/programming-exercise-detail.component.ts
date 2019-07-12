import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';

import { ProgrammingExercise, ProgrammingLanguage } from './programming-exercise.model';
import { ProgrammingExerciseService } from 'app/entities/programming-exercise/services/programming-exercise.service';
import { Result, ResultService } from 'app/entities/result';
import { JhiAlertService } from 'ng-jhipster';
import { ParticipationType } from './programming-exercise-participation.model';
import { ProgrammingExerciseParticipationService } from 'app/entities/programming-exercise/services/programming-exercise-participation.service';

@Component({
    selector: 'jhi-programming-exercise-detail',
    templateUrl: './programming-exercise-detail.component.html',
    styleUrls: ['./programming-exercise-detail.component.scss'],
})
export class ProgrammingExerciseDetailComponent implements OnInit {
    ParticipationType = ParticipationType;
    readonly JAVA = ProgrammingLanguage.JAVA;

    programmingExercise: ProgrammingExercise;

    constructor(
        private activatedRoute: ActivatedRoute,
        private programmingExerciseService: ProgrammingExerciseService,
        private resultService: ResultService,
        private jhiAlertService: JhiAlertService,
        private programmingExerciseParticipationService: ProgrammingExerciseParticipationService,
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ programmingExercise }) => {
            this.programmingExercise = programmingExercise;

            this.programmingExercise.solutionParticipation.programmingExercise = this.programmingExercise;
            this.programmingExercise.templateParticipation.programmingExercise = this.programmingExercise;

            this.programmingExerciseParticipationService
                .getLatestResultWithFeedback(this.programmingExercise.solutionParticipation.id)
                .pipe(filter((result: Result) => !!result))
                .subscribe((result: Result) => {
                    this.programmingExercise.solutionParticipation.results = [result];
                });

            this.programmingExerciseParticipationService
                .getLatestResultWithFeedback(this.programmingExercise.templateParticipation.id)
                .pipe(filter((result: Result) => !!result))
                .subscribe((result: Result) => {
                    this.programmingExercise.templateParticipation.results = [result];
                });
        });
    }

    previousState() {
        window.history.back();
    }

    squashTemplateCommits() {
        this.programmingExerciseService.squashTemplateRepositoryCommits(this.programmingExercise.id).subscribe(
            () => {
                this.jhiAlertService.success('artemisApp.programmingExercise.squashTemplateCommitsSuccess');
            },
            () => {
                this.jhiAlertService.error('artemisApp.programmingExercise.squashTemplateCommitsError');
            },
        );
    }

    generateStructureOracle() {
        this.programmingExerciseService.generateStructureOracle(this.programmingExercise.id).subscribe(
            res => {
                const jhiAlert = this.jhiAlertService.success(res);
                jhiAlert.msg = res;
            },
            error => {
                const errorMessage = error.headers.get('X-artemisApp-alert');
                // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
                const jhiAlert = this.jhiAlertService.error(errorMessage);
                jhiAlert.msg = errorMessage;
            },
        );
    }
}
