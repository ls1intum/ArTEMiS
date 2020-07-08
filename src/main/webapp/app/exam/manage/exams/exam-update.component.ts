import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Exam } from 'app/entities/exam.model';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AlertService } from 'app/core/alert/alert.service';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import * as moment from 'moment';

@Component({
    selector: 'jhi-exam-update',
    templateUrl: './exam-update.component.html',
})
export class ExamUpdateComponent implements OnInit {
    exam: Exam;
    course: Course;
    isSaving: boolean;

    constructor(
        private route: ActivatedRoute,
        private examManagementService: ExamManagementService,
        private jhiAlertService: AlertService,
        private courseManagementService: CourseManagementService,
    ) {}

    ngOnInit(): void {
        this.route.data.subscribe(({ exam }) => {
            this.exam = exam;
            this.courseManagementService.find(Number(this.route.snapshot.paramMap.get('courseId'))).subscribe(
                (response: HttpResponse<Course>) => {
                    this.exam.course = response.body!;
                    this.course = response.body!;
                },
                (err: HttpErrorResponse) => this.onError(err),
            );
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.exam.id !== undefined) {
            this.subscribeToSaveResponse(this.examManagementService.update(this.course.id, this.exam));
        } else {
            this.subscribeToSaveResponse(this.examManagementService.create(this.course.id, this.exam));
        }
    }

    subscribeToSaveResponse(result: Observable<HttpResponse<Exam>>) {
        result.subscribe(
            () => this.onSaveSuccess(),
            (err: HttpErrorResponse) => this.onSaveError(err),
        );
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message, null, undefined);
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    get isValidPublishResultsDate() {
        return this.exam.publishResultsDate;
    }

    get isValidExamStudentReviewStart() {
        return this.exam.examStudentReviewStart;
    }

    get isValidExamStudentReviewEnd() {
        return this.exam.examStudentReviewEnd;
    }

    get isValidVisibleDate() {
        return this.exam.visibleDate && moment(this.exam.visibleDate).isAfter(moment());
    }

    get isValidStartDate() {
        return this.isValidVisibleDate && this.exam.startDate && moment(this.exam.startDate).isAfter(this.exam.visibleDate);
    }

    get isValidEndDate() {
        return this.isValidStartDate && this.exam.endDate && moment(this.exam.endDate).isAfter(this.exam.startDate);
    }
}
