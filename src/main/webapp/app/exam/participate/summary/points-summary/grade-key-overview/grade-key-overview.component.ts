import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GradingSystemService } from 'app/grading-system/grading-system.service';
import { GradeStep, GradeStepsDTO } from 'app/entities/grade-step.model';
import { HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { GradeType } from 'app/entities/grading-scale.model';

@Component({
    selector: 'jhi-grade-key-overview',
    templateUrl: './grade-key-overview.component.html',
    styleUrls: ['./grade-key-overview.scss'],
})
export class GradeKeyOverviewComponent implements OnInit {
    constructor(private route: ActivatedRoute, private router: Router, private gradingSystemService: GradingSystemService) {}

    courseId?: number;
    examId?: number;

    examTitle?: string;
    gradeSteps: GradeStep[] = [];
    studentGrade?: string;
    isBonus = false;

    ngOnInit(): void {
        this.route.params.subscribe((params) => {
            this.courseId = Number(params['courseId']);
            this.examId = Number(params['examId']);
            this.gradingSystemService
                .findGradeStepsForExam(this.courseId, this.examId)
                .pipe(catchError(() => of(new HttpResponse<GradeStepsDTO>({ body: { examTitle: '', gradeType: GradeType.GRADE, gradeSteps: [] }, status: 404 }))))
                .subscribe((gradeSteps) => {
                    if (gradeSteps.body) {
                        this.examTitle = gradeSteps.body.examTitle;
                        this.isBonus = gradeSteps.body.gradeType === GradeType.BONUS;
                        this.gradeSteps = this.gradingSystemService.sortGradeSteps(gradeSteps.body.gradeSteps);
                    }
                });
        });
        this.route.queryParams.subscribe((queryParams) => {
            this.studentGrade = queryParams['grade'];
        });
    }

    previousState() {
        this.router.navigate(['courses', this.courseId!.toString(), 'exams', this.examId!.toString()]);
    }

    printPDF() {
        setTimeout(() => window.print());
    }
}
