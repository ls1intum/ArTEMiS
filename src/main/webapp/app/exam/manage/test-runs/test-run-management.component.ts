import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Course } from 'app/entities/course.model';
import { StudentExam } from 'app/entities/student-exam.model';
import { SortService } from 'app/shared/service/sort.service';
import { Exam } from 'app/entities/exam.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService } from 'ng-jhipster';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { CreateTestRunModalComponent } from 'app/exam/manage/test-runs/create-test-run-modal.component';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { AccountService } from 'app/core/auth/account.service';
import { Subject } from 'rxjs';
import { User } from 'app/core/user/user.model';

@Component({
    selector: 'jhi-test-run-management',
    templateUrl: './test-run-management.component.html',
})
export class TestRunManagementComponent implements OnInit {
    course: Course;
    exam: Exam;
    isLoading: boolean;
    isExamStarted: boolean;
    testRuns: StudentExam[] = [];
    instructor: User;
    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();
    predicate: string;
    ascending: boolean;

    constructor(
        private route: ActivatedRoute,
        private jhiAlertService: JhiAlertService,
        private examManagementService: ExamManagementService,
        private accountService: AccountService,
        private sortService: SortService,
        private modalService: NgbModal,
    ) {
        this.predicate = 'id';
        this.ascending = true;
    }

    ngOnInit(): void {
        this.examManagementService.find(Number(this.route.snapshot.paramMap.get('courseId')), Number(this.route.snapshot.paramMap.get('examId')), false, true).subscribe(
            (response: HttpResponse<Exam>) => {
                this.exam = response.body!;
                this.isExamStarted = this.exam.started;
                this.course = this.exam.course;
                this.course.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(this.course);
                this.examManagementService.findAllTestRunsForExam(this.course.id, this.exam.id).subscribe(
                    (res) => {
                        this.testRuns = res.body!;
                    },
                    (err) => this.onError(err),
                );
            },
            (error) => this.onError(error),
        );
        this.accountService.fetch().subscribe((res) => {
            if (res.body != null) {
                this.instructor = res.body;
            }
        });
    }

    /**
     * Open modal to configure a new test run
     */
    openCreateTestRunModal() {
        const modalRef: NgbModalRef = this.modalService.open(CreateTestRunModalComponent as Component, { size: 'lg', backdrop: 'static' });
        modalRef.componentInstance.exam = this.exam;
        modalRef.result
            .then((testRunConfiguration: StudentExam) => {
                this.examManagementService.createTestRun(this.course.id, this.exam.id, testRunConfiguration).subscribe(
                    (res) => {
                        if (res.body != null) {
                            this.testRuns.push(res.body!);
                        }
                    },
                    (error) => {
                        this.onError(error);
                    },
                );
            })
            .catch(() => {});
    }

    /**
     * Delete the test run with the given id.
     * @param testRunId {number}
     */
    deleteTestRun(testRunId: number) {
        this.examManagementService.deleteTestRun(this.course.id, this.exam.id, testRunId).subscribe(
            () => {
                this.testRuns = this.testRuns!.filter((testRun) => testRun.id !== testRunId);
                this.dialogErrorSource.next('');
            },
            (error) => this.dialogErrorSource.next(error.message),
        );
    }

    /**
     * Track the items on the testruns Table
     * @param index {number}
     * @param item {StudentExam}
     */
    trackId(index: number, item: StudentExam) {
        return item.id;
    }

    sortRows() {
        this.sortService.sortByProperty(this.testRuns, this.predicate, this.ascending);
    }

    /**
     * Get function to determine if a test run has been submitted.
     * Used to enable the assess test run button.
     */
    get testRunCanBeAssessed(): boolean {
        if (!!this.testRuns && this.testRuns.length > 0) {
            for (const testRun of this.testRuns) {
                if (testRun.user && this.instructor && testRun.submitted) {
                    return true;
                }
            }
        }
        return false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }
}
