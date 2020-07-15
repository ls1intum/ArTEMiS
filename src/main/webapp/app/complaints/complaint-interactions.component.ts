import { Component, Input, OnInit } from '@angular/core';
import { Exercise } from 'app/entities/exercise.model';
import * as moment from 'moment';
import { ComplaintType } from 'app/entities/complaint.model';
import { ComplaintService } from 'app/complaints/complaint.service';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { Result } from 'app/entities/result.model';
import { ActivatedRoute } from '@angular/router';
import { Course } from 'app/entities/course.model';

@Component({
    selector: 'jhi-complaint-interactions',
    templateUrl: './complaint-interactions.component.html',
})
export class ComplaintInteractionsComponent implements OnInit {
    @Input() exercise: Exercise;
    @Input() participation: StudentParticipation;
    @Input() result: Result;
    @Input()
    examId: number;

    // Refers to the student review period of an exam.
    @Input() isWithinStudentReviewPeriod: boolean;

    get isExamMode() {
        return this.isWithinStudentReviewPeriod != null;
    }

    showRequestMoreFeedbackForm = false;
    // indicates if there is a complaint for the result of the submission
    hasComplaint = false;
    // indicates if more feedback was requested already
    hasRequestMoreFeedback = false;
    // the number of complaints that the student is still allowed to submit in the course. this is used for disabling the complain button.
    numberOfAllowedComplaints: number;
    showComplaintForm = false;
    ComplaintType = ComplaintType;

    constructor(private complaintService: ComplaintService, private activatedRoute: ActivatedRoute) {}

    /**
     * Loads the number of allowed complaints and feedback requests
     */
    ngOnInit(): void {
        if (this.isExamMode) {
            this.activatedRoute.params.subscribe((params) => {
                this.examId = params['examId'];
            });
            if (this.participation && this.participation.id && this.exercise) {
                if (this.participation.results && this.participation.results.length > 0) {
                    // Make sure result and participation are connected
                    this.result = this.participation.results[0];
                    this.result.participation = this.participation;
                }
            }
        } else {
            if (this.course) {
                // for normal exercises we track the number of allowed complaints
                if (this.course.complaintsEnabled) {
                    this.complaintService.getNumberOfAllowedComplaintsInCourse(this.course.id, this.exercise.teamMode).subscribe((allowedComplaints: number) => {
                        this.numberOfAllowedComplaints = allowedComplaints;
                    });
                } else {
                    this.numberOfAllowedComplaints = 0;
                }
            }
        }
        if (this.participation.submissions && this.participation.submissions.length > 0) {
            if (this.result && this.result.completionDate) {
                this.complaintService.findByResultId(this.result.id).subscribe((res) => {
                    if (res.body) {
                        if (res.body.complaintType == null || res.body.complaintType === ComplaintType.COMPLAINT) {
                            this.hasComplaint = true;
                        } else {
                            this.hasRequestMoreFeedback = true;
                        }
                    }
                });
            }
        }
    }

    get course(): Course | null {
        return this.exercise.course;
    }

    /**
     * This function is used to check whether the student is allowed to submit a complaint or not.
     * For exams, submitting a complaint is allowed within the Student Review Period, see {@link isWithinStudentReviewPeriod}.
     *
     * For normal course exercises, submitting a complaint is allowed within one week after the student received the
     * result. If the result was submitted after the assessment due date or the assessment due date is not set, the completion date of the result is checked. If the result was
     * submitted before the assessment due date, the assessment due date is checked, as the student can only see the result after the assessment due date.
     */
    get isTimeOfComplaintValid(): boolean {
        if (this.isExamMode) {
            return this.isWithinStudentReviewPeriod && !!this.result.completionDate;
        } else if (this.result && this.result.completionDate) {
            const resultCompletionDate = moment(this.result.completionDate!);
            if (!this.exercise.assessmentDueDate || resultCompletionDate.isAfter(this.exercise.assessmentDueDate)) {
                return resultCompletionDate.isAfter(moment().subtract(this.course?.maxComplaintTimeDays, 'day'));
            }
            return moment(this.exercise.assessmentDueDate).isAfter(moment().subtract(this.course?.maxComplaintTimeDays, 'day'));
        } else {
            return false;
        }
    }
    /**
     * toggles between showing the complaint form
     */
    toggleComplaintForm() {
        this.showRequestMoreFeedbackForm = false;
        this.showComplaintForm = !this.showComplaintForm;
    }
    /**
     * toggles between showing the feedback request form
     */
    toggleRequestMoreFeedbackForm() {
        this.showComplaintForm = false;
        this.showRequestMoreFeedbackForm = !this.showRequestMoreFeedbackForm;
    }
}
