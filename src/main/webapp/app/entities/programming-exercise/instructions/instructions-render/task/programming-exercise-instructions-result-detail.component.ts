import { Component, Input, OnInit } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BuildLogEntryArray } from 'app/entities/build-log';
import { Result, ResultService } from 'app/entities/result';
import { Feedback } from 'app/entities/feedback';
import { CourseService } from 'app/entities/course';
import { AccountService } from 'app/core';

// Modal -> Result details view
@Component({
    selector: 'jhi-programming-exercise-instructions-result-detail',
    // This popup references the result detail html template, so make sure the constant names match
    templateUrl: '../../../../result/result-detail.component.html',
})
export class ProgrammingExerciseInstructionResultDetailComponent implements OnInit {
    @Input() result: Result;
    @Input() tests: string[];
    isLoading: boolean;
    feedbackList: Feedback[];
    isAtLeastTutor: boolean;
    buildLogs: BuildLogEntryArray;

    constructor(public activeModal: NgbActiveModal, private resultService: ResultService, private courseService: CourseService, private accountService: AccountService) {}

    ngOnInit(): void {
        of(this.result.feedbacks)
            .pipe(
                switchMap(feedbacks => (feedbacks ? of(feedbacks) : this.loadResultDetails(this.result))),
                map(feedbacks =>
                    this.tests.map(test => {
                        const matchingFeedback = feedbacks.find(({ text }) => text === test);
                        return matchingFeedback || ({ text: test, detailText: 'No result information available', type: 'AUTOMATIC' } as Feedback);
                    }),
                ),
                tap(feedbacks => (this.feedbackList = feedbacks)),
            )
            .subscribe();

        this.courseService.findWithBasicInformation(this.result.id).subscribe(resp => (this.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(resp.body!)));
    }

    loadResultDetails(result: Result): Observable<Feedback[]> {
        this.isLoading = true;
        return this.resultService.getFeedbackDetailsForResult(result.id).pipe(
            filter(res => !!res && !!res.body),
            map(res => res.body as Feedback[]),
            catchError(() => of([] as Feedback[])),
            tap(() => (this.isLoading = false)),
        );
    }
}
