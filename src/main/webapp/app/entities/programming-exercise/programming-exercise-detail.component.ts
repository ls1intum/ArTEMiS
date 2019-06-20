import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ProgrammingExercise, ProgrammingLanguage } from './programming-exercise.model';
import { ProgrammingExerciseService } from 'app/entities/programming-exercise/programming-exercise.service';
import { ResultService } from 'app/entities/result';
import { JhiAlertService } from 'ng-jhipster';
import { ParticipationType } from './programming-exercise-participation.model';

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
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ programmingExercise }) => {
            this.programmingExercise = programmingExercise;

            this.programmingExercise.solutionParticipation.exercise = this.programmingExercise;
            this.programmingExercise.templateParticipation.exercise = this.programmingExercise;

            const course = this.programmingExercise.course!;
            this.resultService.findResultsForParticipation(course.id, this.programmingExercise.id, this.programmingExercise.solutionParticipation.id).subscribe(results => {
                this.programmingExercise.solutionParticipation.results = results.body!;
            });

            this.resultService.findResultsForParticipation(course.id, this.programmingExercise.id, this.programmingExercise.templateParticipation.id).subscribe(results => {
                this.programmingExercise.templateParticipation.results = results.body!;
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
