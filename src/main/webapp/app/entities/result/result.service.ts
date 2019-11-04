import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { SERVER_API_URL } from 'app/app.constants';

import * as moment from 'moment';

import { Result } from './result.model';
import { createRequestOption } from 'app/shared';
import { Feedback } from 'app/entities/feedback';
import { StudentParticipation } from 'app/entities/participation';
import { Exercise, ExerciseService } from 'app/entities/exercise';
import { isMoment } from 'moment';

export type EntityResponseType = HttpResponse<Result>;
export type EntityArrayResponseType = HttpResponse<Result[]>;

export interface IResultService {
    find: (id: number) => Observable<EntityResponseType>;
    findBySubmissionId: (submissionId: number) => Observable<EntityResponseType>;
    findResultsForParticipation: (courseId: number, exerciseId: number, participationId: number, req?: any) => Observable<EntityArrayResponseType>;
    getResultsForExercise: (courseId: number, exerciseId: number, req?: any) => Observable<EntityArrayResponseType>;
    getLatestResultWithFeedbacks: (particpationId: number) => Observable<HttpResponse<Result>>;
    getFeedbackDetailsForResult: (resultId: number) => Observable<HttpResponse<Feedback[]>>;
    delete: (id: number) => Observable<HttpResponse<void>>;
}

@Injectable({ providedIn: 'root' })
export class ResultService implements IResultService {
    private courseResourceUrl = SERVER_API_URL + 'api/courses';
    private resultResourceUrl = SERVER_API_URL + 'api/results';

    constructor(private http: HttpClient, private exerciseService: ExerciseService) {}

    find(resultId: number): Observable<EntityResponseType> {
        return this.http.get<Result>(`${this.resultResourceUrl}/${resultId}`, { observe: 'response' }).map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    findBySubmissionId(submissionId: number): Observable<EntityResponseType> {
        return this.http
            .get<Result>(`${this.resultResourceUrl}/submission/${submissionId}`, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    findResultsForParticipation(courseId: number, exerciseId: number, participationId: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get(`${this.courseResourceUrl}/${courseId}/exercises/${exerciseId}/participations/${participationId}/results`, {
                params: options,
                observe: 'response',
            })
            .map((res: EntityArrayResponseType) => this.convertArrayResponse(res));
    }

    getResultsForExercise(courseId: number, exerciseId: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<Result[]>(`${this.courseResourceUrl}/${courseId}/exercises/${exerciseId}/results`, {
                params: options,
                observe: 'response',
            })
            .map((res: EntityArrayResponseType) => this.convertArrayResponse(res));
    }

    getFeedbackDetailsForResult(resultId: number): Observable<HttpResponse<Feedback[]>> {
        return this.http.get<Feedback[]>(`${this.resultResourceUrl}/${resultId}/details`, { observe: 'response' });
    }

    getLatestResultWithFeedbacks(particpationId: number): Observable<HttpResponse<Result>> {
        return this.http.get<Result>(`${this.resultResourceUrl}/${particpationId}/latest-result`, { observe: 'response' });
    }

    delete(resultId: number): Observable<HttpResponse<void>> {
        return this.http.delete<void>(`${this.resultResourceUrl}/${resultId}`, { observe: 'response' });
    }

    public convertDateFromClient(result: Result): Result {
        const copy: Result = Object.assign({}, result, {
            completionDate:
                // Result completionDate is a moment object -> toJSON.
                result.completionDate != null && isMoment(result.completionDate)
                    ? result.completionDate.toJSON()
                    : // Result completionDate would be a valid date -> keep string.
                    result.completionDate && moment(result.completionDate).isValid()
                    ? result.completionDate
                    : // No valid date -> remove date.
                      null,
        });
        return copy;
    }

    protected convertArrayResponse(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((result: Result) => {
                result.completionDate = result.completionDate != null ? moment(result.completionDate) : null;
                result.participation = this.convertParticipationDateFromServer(result.participation! as StudentParticipation);
            });
        }
        return res;
    }

    public convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.completionDate = res.body.completionDate != null ? moment(res.body.completionDate) : null;
            res.body.participation = this.convertParticipationDateFromServer(res.body.participation! as StudentParticipation);
        }
        return res;
    }

    convertParticipationDateFromServer(participation: StudentParticipation) {
        if (participation) {
            participation.initializationDate = participation.initializationDate != null ? moment(participation.initializationDate) : null;
            if (participation.exercise) {
                participation.exercise = this.exerciseService.convertExerciseDateFromServer(participation.exercise);
            }
        }
        return participation;
    }

    /**
     * This function is used to check whether the student is allowed to submit a complaint or not. Submitting a complaint is allowed within one week after the student received the
     * result. If the result was submitted after the assessment due date or the assessment due date is not set, the completion date of the result is checked. If the result was
     * submitted before the assessment due date, the assessment due date is checked, as the student can only see the result after the assessment due date.
     */
    isTimeOfComplaintValid(result: Result, exercise: Exercise): boolean {
        const resultCompletionDate = moment(result.completionDate!);
        if (!exercise.assessmentDueDate || resultCompletionDate.isAfter(exercise.assessmentDueDate)) {
            return resultCompletionDate.isAfter(moment().subtract(1, 'week'));
        }
        return moment(exercise.assessmentDueDate).isAfter(moment().subtract(1, 'week'));
    }
}
