import { Component, Input, OnInit } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { ModelingExercise } from 'app/entities/modeling-exercise';
import { ArtemisMarkdown } from 'app/components/util/markdown.service';
import { UMLModel } from '@ls1intum/apollon';
import { ExerciseType } from 'app/entities/exercise';

@Component({
    selector: 'jhi-expandable-sample-solution',
    templateUrl: './expandable-sample-solution.component.html',
    styleUrls: ['../assessment-instructions.scss'],
})
export class ExpandableSampleSolutionComponent implements OnInit {
    @Input() exercise: ModelingExercise;
    @Input() isCollapsed = false;

    readonly ExerciseType_MODELING = ExerciseType.MODELING;
    formattedSampleSolutionExplanation: SafeHtml | null;
    sampleSolution: UMLModel;

    constructor(private artemisMarkdown: ArtemisMarkdown) {}

    ngOnInit() {
        if (this.exercise) {
            if (this.exercise.sampleSolutionModel) {
                this.sampleSolution = JSON.parse(this.exercise.sampleSolutionModel);
            }
            if (this.exercise.sampleSolutionExplanation) {
                this.formattedSampleSolutionExplanation = this.artemisMarkdown.safeHtmlForMarkdown(this.exercise.sampleSolutionExplanation);
            }
        }
    }
}
