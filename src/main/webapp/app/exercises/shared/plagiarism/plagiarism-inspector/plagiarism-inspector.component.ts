import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ModelingExerciseService } from 'app/exercises/modeling/manage/modeling-exercise.service';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { TextExerciseService } from 'app/exercises/text/manage/text-exercise/text-exercise.service';
import { ModelingPlagiarismResult } from 'app/exercises/shared/plagiarism/types/modeling/ModelingPlagiarismResult';
import { downloadFile } from 'app/shared/util/download.util';
import { TextPlagiarismResult } from 'app/exercises/shared/plagiarism/types/text/TextPlagiarismResult';
import { PlagiarismResult } from 'app/exercises/shared/plagiarism/types/PlagiarismResult';
import { ExportToCsv } from 'export-to-csv';
import { PlagiarismComparison } from 'app/exercises/shared/plagiarism/types/PlagiarismComparison';
import { ModelingSubmissionElement } from 'app/exercises/shared/plagiarism/types/modeling/ModelingSubmissionElement';
import { TextSubmissionElement } from 'app/exercises/shared/plagiarism/types/text/TextSubmissionElement';

@Component({
    selector: 'jhi-plagiarism-inspector',
    styleUrls: ['./plagiarism-inspector.component.scss'],
    templateUrl: './plagiarism-inspector.component.html',
})
export class PlagiarismInspectorComponent implements OnInit {
    /**
     * The modeling exercise for which plagiarism is to be detected.
     */
    exercise: Exercise;

    /**
     * Result of the automated plagiarism detection
     */
    plagiarismResult?: TextPlagiarismResult | ModelingPlagiarismResult;

    /**
     * True, if an automated plagiarism detection is running; false otherwise.
     */
    detectionInProgress: boolean;

    /**
     * Index of the currently selected comparison.
     */
    selectedComparisonIndex: number;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private modelingExerciseService: ModelingExerciseService,
        private textExerciseService: TextExerciseService,
    ) {}

    ngOnInit() {
        this.route.data.subscribe(({ exercise }) => {
            this.exercise = exercise;
        });
    }

    checkPlagiarism() {
        if (this.exercise.type === ExerciseType.MODELING) {
            this.checkPlagiarismModeling();
        } else {
            this.checkPlagiarismJPlag();
        }
    }

    selectComparisonAtIndex(index: number) {
        this.selectedComparisonIndex = index;
    }

    /**
     * Trigger the server-side plagiarism detection and fetch its result.
     */
    checkPlagiarismJPlag() {
        this.detectionInProgress = true;

        this.textExerciseService.checkPlagiarismJPlag(this.exercise.id!).subscribe(
            (result: TextPlagiarismResult) => {
                this.detectionInProgress = false;

                this.sortComparisonsForResult(result);

                this.plagiarismResult = result;
                this.selectedComparisonIndex = 0;
            },
            () => (this.detectionInProgress = false),
        );
    }

    /**
     * Trigger the server-side plagiarism detection and fetch its result.
     */
    checkPlagiarismModeling() {
        this.detectionInProgress = true;

        this.modelingExerciseService.checkPlagiarism(this.exercise.id!).subscribe(
            (result: ModelingPlagiarismResult) => {
                this.detectionInProgress = false;

                this.sortComparisonsForResult(result);

                this.plagiarismResult = result;
                this.selectedComparisonIndex = 0;
            },
            () => (this.detectionInProgress = false),
        );
    }

    sortComparisonsForResult(result: PlagiarismResult<any>) {
        result.comparisons = result.comparisons.sort((a, b) => b.similarity - a.similarity);
    }

    /**
     * Download plagiarism detection results as JSON document.
     */
    downloadPlagiarismResultsJson() {
        const json = JSON.stringify(this.plagiarismResult);
        const blob = new Blob([json], { type: 'application/json' });

        downloadFile(blob, `plagiarism-result_${this.exercise.type}-exercise-${this.exercise.id}.json`);
    }

    /**
     * Download plagiarism detection results as CSV document.
     */
    downloadPlagiarismResultsCsv() {
        if (this.plagiarismResult && this.plagiarismResult.comparisons.length > 0) {
            const csvExporter = new ExportToCsv({
                fieldSeparator: ';',
                quoteStrings: '"',
                decimalSeparator: 'locale',
                showLabels: true,
                title: `Plagiarism Check for Exercise ${this.exercise.id}: ${this.exercise.title}`,
                filename: `plagiarism-result_${this.exercise.type}-exercise-${this.exercise.id}`,
                useTextFile: false,
                useBom: true,
                headers: ['Similarity', 'Status', 'Participant 1', 'Submission 1', 'Score 1', 'Size 1', 'Participant 2', 'Submission 2', 'Score 2', 'Size 2'],
            });

            const csvData = (this.plagiarismResult.comparisons as PlagiarismComparison<ModelingSubmissionElement | TextSubmissionElement>[]).map((comparison) => {
                return Object.assign({
                    Similarity: comparison.similarity,
                    Status: comparison.status,
                    'Participant 1': comparison.submissionA.studentLogin,
                    'Submission 1': comparison.submissionA.submissionId,
                    'Score 1': comparison.submissionA.score,
                    'Size 1': comparison.submissionA.size,
                    'Participant 2': comparison.submissionB.studentLogin,
                    'Submission 2': comparison.submissionB.submissionId,
                    'Score 2': comparison.submissionB.score,
                    'Size 2': comparison.submissionB.size,
                });
            });

            csvExporter.generateCsv(csvData);
        }
    }
}
