import { AfterViewInit, Component, Directive, ElementRef, Input, OnInit, QueryList, SimpleChanges, ViewChildren } from '@angular/core';
// @ts-ignore
import Split from 'split.js';
import { Subject } from 'rxjs';
import { PlagiarismComparison } from 'app/exercises/shared/plagiarism/types/PlagiarismComparison';
import { TextSubmissionElement } from 'app/exercises/shared/plagiarism/types/text/TextSubmissionElement';
import { ModelingSubmissionElement } from 'app/exercises/shared/plagiarism/types/modeling/ModelingSubmissionElement';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { PlagiarismSubmission } from 'app/exercises/shared/plagiarism/types/PlagiarismSubmission';

@Directive({ selector: '[jhiPane]' })
export class SplitPaneDirective {
    constructor(public el: ElementRef) {}
}

@Component({
    selector: 'jhi-plagiarism-split-view',
    styleUrls: ['./plagiarism-split-view.component.scss'],
    templateUrl: './plagiarism-split-view.component.html',
})
export class PlagiarismSplitViewComponent implements AfterViewInit, OnChanges, OnInit {
    @Input() comparison: PlagiarismComparison<TextSubmissionElement | ModelingSubmissionElement>;
    @Input() exercise: Exercise;
    @Input() splitControlSubject: Subject<string>;

    @ViewChildren(SplitPaneDirective) panes!: QueryList<SplitPaneDirective>;

    public split: Split.Instance;

    public isModelingExercise: boolean;
    public isProgrammingExercise: boolean;
    public isTextExercise: boolean;

    /**
     * Initialize third party libs inside this lifecycle hook.
     */
    ngAfterViewInit(): void {
        const paneElements = this.panes.map((pane: SplitPaneDirective) => pane.el.nativeElement);

        this.split = Split(paneElements, {
            minSize: 100,
            sizes: [50, 50],
            gutterSize: 8,
        });
    }

    ngOnInit(): void {
        this.splitControlSubject.subscribe((pane: string) => this.handleSplitControl(pane));
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.exercise) {
            const exercise = changes.exercise.currentValue;

            this.isModelingExercise = exercise.type === ExerciseType.MODELING;
            this.isProgrammingExercise = exercise.type === ExerciseType.PROGRAMMING;
            this.isTextExercise = exercise.type === ExerciseType.TEXT;
        }
    }

    getModelingSubmissionA() {
        return this.comparison.submissionA as PlagiarismSubmission<ModelingSubmissionElement>;
    }

    getModelingSubmissionB() {
        return this.comparison.submissionB as PlagiarismSubmission<ModelingSubmissionElement>;
    }

    getTextSubmissionA() {
        return this.comparison.submissionA as PlagiarismSubmission<TextSubmissionElement>;
    }

    getTextSubmissionB() {
        return this.comparison.submissionB as PlagiarismSubmission<TextSubmissionElement>;
    }

    handleSplitControl(pane: string) {
        switch (pane) {
            case 'left': {
                this.split.collapse(1);
                return;
            }
            case 'right': {
                this.split.collapse(0);
                return;
            }
            case 'even': {
                this.split.setSizes([50, 50]);
            }
        }
    }
}
