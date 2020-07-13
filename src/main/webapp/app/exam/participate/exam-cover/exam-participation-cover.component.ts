import { Component, Input, OnInit, OnDestroy, EventEmitter, Output } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';

import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { TranslateService } from '@ngx-translate/core';

import { Exam } from 'app/entities/exam.model';
import { Course } from 'app/entities/course.model';
import { AccountService } from 'app/core/auth/account.service';
import { ExamParticipationService } from 'app/exam/participate/exam-participation.service';
import { StudentExam } from 'app/entities/student-exam.model';
import { ArtemisServerDateService } from 'app/shared/server-date.service';
import * as moment from 'moment';

@Component({
    selector: 'jhi-exam-participation-cover',
    templateUrl: './exam-participation-cover.component.html',
    styleUrls: ['./exam-participation-cover.scss'],
})
export class ExamParticipationCoverComponent implements OnInit, OnDestroy {
    /**
     * if startView is set to true: startText and confirmationStartText will be displayed
     * if startView is set to false: endText and confirmationEndText will be displayed
     */
    @Input() startView: boolean;
    @Input() exam: Exam;
    @Input() studentExam: StudentExam;
    @Output() onExamStarted: EventEmitter<StudentExam> = new EventEmitter<StudentExam>();
    @Output() onExamEnded: EventEmitter<StudentExam> = new EventEmitter<StudentExam>();
    course: Course | null;
    startEnabled: boolean;
    endEnabled: boolean;
    confirmed: boolean;

    formattedGeneralInformation: SafeHtml | null;
    formattedConfirmationText: SafeHtml | null;

    interval: number;
    waitingForExamStart = false;
    timeUntilStart = '0';
    formattedStartDate = '';

    accountName = '';
    enteredName = '';

    graceEndDate: moment.Moment;
    criticalTime = moment.duration(30, 'seconds');

    constructor(
        private courseService: CourseManagementService,
        private artemisMarkdown: ArtemisMarkdownService,
        private translateService: TranslateService,
        private accountService: AccountService,
        private examParticipationService: ExamParticipationService,
        private serverDateService: ArtemisServerDateService,
    ) {}

    /**
     * on init uses the correct information to display in either start or final view
     * changes in the exam and subscription is handled in the exam-participation.component
     */
    ngOnInit(): void {
        this.confirmed = false;
        this.startEnabled = false;
        if (this.startView) {
            this.formattedGeneralInformation = this.artemisMarkdown.safeHtmlForMarkdown(this.exam.startText);
            this.formattedConfirmationText = this.artemisMarkdown.safeHtmlForMarkdown(this.exam.confirmationStartText);
            this.formattedStartDate = this.exam.startDate ? this.exam.startDate.format('LT') : '';
        } else {
            this.formattedGeneralInformation = this.artemisMarkdown.safeHtmlForMarkdown(this.exam.endText);
            this.formattedConfirmationText = this.artemisMarkdown.safeHtmlForMarkdown(this.exam.confirmationEndText);
            // this should be the individual working end + the grace period
            this.graceEndDate = moment(this.exam.startDate).add(this.studentExam.workingTime, 'seconds').add(this.exam.gracePeriod, 'seconds');
        }

        this.accountService.identity().then((user) => {
            if (user && user.name) {
                this.accountName = user.name;
            }
        });
    }

    ngOnDestroy() {
        if (this.interval) {
            clearInterval(this.interval);
        }
    }

    /**
     * checks whether confirmation checkbox has been checked
     * if startView true:
     * if confirmed, we further check whether exam has started yet regularly
     */
    updateConfirmation() {
        if (this.startView) {
            this.startEnabled = this.confirmed;
        } else {
            this.endEnabled = this.confirmed;
        }
    }

    /**
     * check if exam already started
     */
    hasStarted(): boolean {
        return this.exam?.startDate ? this.exam.startDate.isBefore(this.serverDateService.now()) : false;
    }

    /**
     * displays popup or start exam participation immediately
     */
    startExam() {
        this.examParticipationService.loadStudentExamWithExercises(this.exam.course.id, this.exam.id).subscribe((studentExam: StudentExam) => {
            this.studentExam = studentExam;
            this.examParticipationService.saveStudentExamToLocalStorage(this.exam.course.id, this.exam.id, studentExam);
            if (this.hasStarted()) {
                this.onExamStarted.emit(studentExam);
            } else {
                this.waitingForExamStart = true;
                this.interval = window.setInterval(() => {
                    this.updateDisplayedTimes(studentExam);
                }, 100);
            }
        });
    }

    /**
     * updates all displayed (relative) times in the UI
     */
    updateDisplayedTimes(studentExam: StudentExam) {
        const translationBasePath = 'showStatistic.';
        // update time until start
        if (this.exam && this.exam.startDate) {
            if (this.hasStarted()) {
                this.timeUntilStart = this.translateService.instant(translationBasePath + 'now');
                this.onExamStarted.emit(studentExam);
            } else {
                this.timeUntilStart = this.relativeTimeText(this.exam.startDate.diff(this.serverDateService.now(), 'seconds'));
            }
        } else {
            this.timeUntilStart = '';
        }
    }

    /**
     * Express the given timespan as humanized text
     *
     * @param remainingTimeSeconds {number} the amount of seconds to display
     * @return {string} humanized text for the given amount of seconds
     */
    relativeTimeText(remainingTimeSeconds: number): string {
        if (remainingTimeSeconds > 210) {
            return Math.ceil(remainingTimeSeconds / 60) + ' min';
        } else if (remainingTimeSeconds > 59) {
            return Math.floor(remainingTimeSeconds / 60) + ' min ' + (remainingTimeSeconds % 60) + ' s';
        } else {
            return remainingTimeSeconds + ' s';
        }
    }

    /**
     * Submits the exam if user has valid token
     */
    submitExam() {
        // TODO: refactor following code
        // this.examSessionService.getCurrentExamSession(this.courseId, this.examId).subscribe((response) => {
        //     const localSessionToken = this.sessionStorage.retrieve('ExamSessionToken');
        //     const validSessionToken = response.body?.sessionToken ?? '';
        //     if (validSessionToken && localSessionToken === validSessionToken) {
        //         console.log(validSessionToken + ' is the same as ' + localSessionToken);
        //         // TODO: submit exam
        //     } else {
        //         console.log('Something went wrong');
        //         // error message
        //     }
        // });
        this.onExamEnded.emit();
    }

    get startButtonEnabled(): boolean {
        return !!(this.nameIsCorrect && this.confirmed && this.exam && this.exam.visibleDate && this.exam.visibleDate.isBefore(this.serverDateService.now()));
    }

    get endButtonEnabled(): boolean {
        // TODO: add logic when confirm can be clicked
        return !!(this.nameIsCorrect && this.confirmed && this.exam);
    }

    get nameIsCorrect(): boolean {
        return this.enteredName.trim() === this.accountName.trim();
    }

    get inserted(): boolean {
        return this.enteredName.trim() !== '';
    }
}
