import { Component, OnInit } from '@angular/core';
import { FormControl, Validators, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { StudentExam } from 'app/entities/student-exam.model';
import { StudentExamService } from 'app/exam/manage/student-exams/student-exam.service';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { User } from 'app/core/user/user.model';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { ArtemisDurationFromSecondsPipe } from 'app/shared/pipes/artemis-duration-from-seconds.pipe';
import { AlertService } from 'app/core/alert/alert.service';
import { AccountService } from 'app/core/auth/account.service';

@Component({
    selector: 'jhi-student-exam-detail',
    templateUrl: './student-exam-detail.component.html',
    providers: [ArtemisDurationFromSecondsPipe],
})
export class StudentExamDetailComponent implements OnInit {
    courseId: number;
    studentExam: StudentExam;
    course: Course;
    student: User;
    workingTimeForm: FormGroup;
    isSavingWorkingTime = false;
    isAtLeastInstructor = false;

    constructor(
        private route: ActivatedRoute,
        private studentExamService: StudentExamService,
        private courseService: CourseManagementService,
        private artemisDurationFromSecondsPipe: ArtemisDurationFromSecondsPipe,
        private alertService: AlertService,
        private accountService: AccountService,
    ) {}

    /**
     * Initialize the courseId and studentExam
     */
    ngOnInit(): void {
        this.loadAll();
    }

    /**
     * Load the course and the student exam
     */
    loadAll() {
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        this.route.data.subscribe(({ studentExam }) => this.setStudentExam(studentExam));

        this.courseService.find(this.courseId).subscribe((courseResponse) => {
            this.course = courseResponse.body!;
            this.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(this.course);
        });
        this.student = this.studentExam.user;
    }

    /**
     * Link to download the exported PDF of the students participation
     */
    downloadPdf() {
        // TODO
    }

    /**
     * Get an icon for the type of the given exercise.
     * @param exercise {Exercise}
     */
    exerciseIcon(exercise: Exercise): string {
        switch (exercise.type) {
            case ExerciseType.QUIZ:
                return 'check-double';
            case ExerciseType.FILE_UPLOAD:
                return 'file-upload';
            case ExerciseType.MODELING:
                return 'project-diagram';
            case ExerciseType.PROGRAMMING:
                return 'keyboard';
            default:
                return 'font';
        }
    }

    /**
     * Save the defined working time
     */
    saveWorkingTime() {
        this.isSavingWorkingTime = true;
        const seconds = this.workingTimeForm.controls.minutes.value * 60 + this.workingTimeForm.controls.seconds.value;
        this.studentExamService.updateWorkingTime(this.courseId, this.studentExam.exam.id, this.studentExam.id, seconds).subscribe(
            (res) => {
                if (res.body) {
                    this.setStudentExam(res.body);
                }
                this.isSavingWorkingTime = false;
                this.alertService.success('artemisApp.studentExamDetail.saveWorkingTimeSuccessful');
            },
            () => {
                this.alertService.error('artemisApp.studentExamDetail.workingTimeCouldNotBeSaved');
                this.isSavingWorkingTime = false;
            },
        );
    }

    private setStudentExam(studentExam: StudentExam) {
        this.studentExam = studentExam;
        this.initWorkingTimeForm();
    }

    private initWorkingTimeForm() {
        const workingTime = this.artemisDurationFromSecondsPipe.transform(this.studentExam.workingTime);
        const workingTimeParts = workingTime.split(':');
        this.workingTimeForm = new FormGroup({
            minutes: new FormControl(parseInt(workingTimeParts[0] ? workingTimeParts[0] : '0', 10), [Validators.min(0), Validators.required]),
            seconds: new FormControl(parseInt(workingTimeParts[1] ? workingTimeParts[1] : '0', 10), [Validators.min(0), Validators.max(59), Validators.required]),
        });
    }
}
