import { Component, Input, OnInit } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService } from 'ng-jhipster';
import { Exercise, ExerciseService } from '../../entities/exercise';
import { WindowRef } from 'app/core/websocket/window.service';
import { ProgrammingAssessmentRepoExportService, RepositoryExportOptions } from 'app/programming-assessment/repo-export/programming-assessment-repo-export.service';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
    selector: 'jhi-exercise-scores-repo-export-dialog',
    templateUrl: './programming-assessment-repo-export-dialog.component.html',
    styles: ['textarea { width: 100%; }'],
})
export class ProgrammingAssessmentRepoExportDialogComponent implements OnInit {
    @Input() exerciseId: number;
    @Input() studentIdList: string;
    @Input() singleStudentMode = false;
    exercise: Exercise;
    exportInProgress: boolean;
    repositoryExportOptions: RepositoryExportOptions;
    isLoading = false;

    constructor(
        private $window: WindowRef,
        private exerciseService: ExerciseService,
        private repoExportService: ProgrammingAssessmentRepoExportService,
        public activeModal: NgbActiveModal,
        private jhiAlertService: JhiAlertService,
    ) {}

    ngOnInit() {
        this.isLoading = true;
        this.exportInProgress = false;
        this.repositoryExportOptions = {
            exportAllStudents: false,
            filterLateSubmissions: false,
            filterLateSubmissionsDate: null,
            addStudentName: true,
            squashAfterInstructor: true,
            normalizeCodeStyle: true,
        };
        this.exerciseService
            .find(this.exerciseId)
            .pipe(
                tap(({ body: exercise }) => {
                    this.exercise = exercise!;
                }),
                catchError(err => {
                    this.jhiAlertService.error(err);
                    this.clear();
                    return of(null);
                }),
            )
            .subscribe(() => {
                this.isLoading = false;
            });
    }

    clear() {
        this.activeModal.dismiss('cancel');
    }

    exportRepos(exerciseId: number) {
        this.exportInProgress = true;
        const studentIdList = this.studentIdList !== undefined && this.studentIdList !== '' ? this.studentIdList.split(',').map(e => e.trim()) : ['ALL'];
        this.repoExportService.exportRepos(exerciseId, studentIdList, this.repositoryExportOptions).subscribe(
            response => {
                this.jhiAlertService.success('Export of repos was successful. The exported zip file with all repositories is currently being downloaded');
                this.activeModal.dismiss(true);
                this.exportInProgress = false;
                if (response.body) {
                    const zipFile = new Blob([response.body], { type: 'application/zip' });
                    const url = this.$window.nativeWindow.URL.createObjectURL(zipFile);
                    const link = document.createElement('a');
                    link.setAttribute('href', url);
                    link.setAttribute('download', response.headers.get('filename')!);
                    document.body.appendChild(link); // Required for FF
                    link.click();
                    window.URL.revokeObjectURL(url);
                }
            },
            err => {
                this.exportInProgress = false;
            },
        );
    }
}
