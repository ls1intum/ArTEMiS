import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ComplaintResponse } from 'app/entities/complaint-response.model';
import { Feedback } from 'app/entities/feedback.model';
import { EntityResponseType, ResultService } from 'app/exercises/shared/result/result.service';
import { Result } from 'app/entities/result.model';

@Injectable({ providedIn: 'root' })
export class ProgrammingAssessmentManualResultService {
    private resourceUrl = SERVER_API_URL + 'api';
    // TODO: It would be good to refactor the convertDate methods into a separate service, so that we don't have to import the result service here.
    constructor(private http: HttpClient, private resultService: ResultService) {}

    /**
     * Saves a new manual result and stores it in the server
     * @param {number} participationId - Id of the participation
     * @param {Result} result - The result to be created and sent to the server
     * @param {submit} submit - Indicates whether submit or save is called
     */
    // TODO: make consistent with other *.assessment.service.ts file
    saveAssessment(participationId: number, result: Result, submit = false, correctionRound = 0): Observable<EntityResponseType> {
        let params = new HttpParams();
        if (submit) {
            params = params.set('submit', 'true');
        }
        if (correctionRound > 0) {
            params = params.set('correction-round', correctionRound.toString());
        }

        const url = `${this.resourceUrl}/participations/${participationId}/manual-results`;
        const copy = this.resultService.convertDateFromClient(result);
        return this.http
            .put<Result>(url, copy, { params, observe: 'response' })
            .map((res: EntityResponseType) => this.resultService.convertDateFromServer(res));
    }

    /**
     * Send the request to update the assessment after the complaint (only done once per complaint). The result score, string
     * and feedbacks will be updated. Original result will be stored as a string on complaint.
     * @param feedbacks list of feedback items (the score is not evaluated from them, as we pass score directly from the result)
     * @param complaintResponse contains main information about the complaint response (time, responseText, reviewer)
     * @param submissionId the id of the submission
     * @return updated result with updated feedbacks and score
     */
    updateAfterComplaint(feedbacks: Feedback[], complaintResponse: ComplaintResponse, submissionId: number): Observable<Result> {
        const url = `${this.resourceUrl}/programming-submissions/${submissionId}/assessment-after-complaint`;
        const assessmentUpdate = {
            feedbacks,
            complaintResponse,
        };
        return this.http.put<Result>(url, assessmentUpdate);
    }

    cancelAssessment(submissionId: number): Observable<void> {
        return this.http.put<void>(`${this.resourceUrl}/programming-submissions/${submissionId}/cancel-assessment`, null);
    }
}
