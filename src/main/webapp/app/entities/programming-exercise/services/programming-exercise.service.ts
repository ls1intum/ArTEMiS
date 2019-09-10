import { Injectable } from '@angular/core';
import * as moment from 'moment';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { omit as _omit } from 'lodash';
import { SERVER_API_URL } from 'app/app.constants';

import { ProgrammingExercise } from '../programming-exercise.model';
import { createRequestOption } from 'app/shared';
import { ExerciseService } from 'app/entities/exercise';
import { Participation, SolutionProgrammingExerciseParticipation, TemplateProgrammingExerciseParticipation } from 'app/entities/participation';

export type EntityResponseType = HttpResponse<ProgrammingExercise>;
export type EntityArrayResponseType = HttpResponse<ProgrammingExercise[]>;

@Injectable({ providedIn: 'root' })
export class ProgrammingExerciseService {
    public resourceUrl = SERVER_API_URL + 'api/programming-exercises';

    constructor(private http: HttpClient, private exerciseService: ExerciseService) {}

    create(programmingExercise: ProgrammingExercise): Observable<EntityResponseType> {
        const copy = this.convertDataFromClient(programmingExercise);
        return this.http.post<ProgrammingExercise>(this.resourceUrl, copy, { observe: 'response' }).map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    automaticSetup(programmingExercise: ProgrammingExercise): Observable<EntityResponseType> {
        const copy = this.convertDataFromClient(programmingExercise);
        return this.http.post<ProgrammingExercise>(this.resourceUrl + '/setup', copy, { observe: 'response' }).map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    generateStructureOracle(exerciseId: number) {
        return this.http.get(this.resourceUrl + '/' + exerciseId + '/generate-tests', { responseType: 'text' });
    }

    squashTemplateRepositoryCommits(exerciseId: number) {
        return this.http.put(this.resourceUrl + '/' + exerciseId + '/squash-template-commits', { responseType: 'text' });
    }

    update(programmingExercise: ProgrammingExercise, req?: any): Observable<EntityResponseType> {
        const options = createRequestOption(req);
        const copy = this.convertDataFromClient(programmingExercise);
        return this.http
            .put<ProgrammingExercise>(this.resourceUrl, copy, { params: options, observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    updateProblemStatement(programmingExerciseId: number, problemStatement: string, req?: any) {
        const options = createRequestOption(req);
        return this.http
            .patch<ProgrammingExercise>(`${this.resourceUrl}-problem`, { exerciseId: programmingExerciseId, problemStatement }, { params: options, observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    find(programmingExerciseId: number): Observable<EntityResponseType> {
        return this.http
            .get<ProgrammingExercise>(`${this.resourceUrl}/${programmingExerciseId}`, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    findWithTemplateAndSolutionParticipation(programmingExerciseId: number): Observable<EntityResponseType> {
        return this.http
            .get<ProgrammingExercise>(`${this.resourceUrl}-with-participations/${programmingExerciseId}`, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<ProgrammingExercise[]>(this.resourceUrl, { params: options, observe: 'response' })
            .map((res: EntityArrayResponseType) => this.exerciseService.convertDateArrayFromServer(res));
    }

    delete(programmingExerciseId: number, deleteStudentReposBuildPlans: boolean, deleteBaseReposBuildPlans: boolean): Observable<HttpResponse<void>> {
        let params = new HttpParams();
        params = params.set('deleteStudentReposBuildPlans', deleteStudentReposBuildPlans.toString());
        params = params.set('deleteBaseReposBuildPlans', deleteBaseReposBuildPlans.toString());
        return this.http.delete<void>(`${this.resourceUrl}/${programmingExerciseId}`, { params, observe: 'response' });
    }

    convertDataFromClient(exercise: ProgrammingExercise) {
        const copy = {
            ...this.exerciseService.convertDateFromClient(exercise),
            buildAndTestStudentSubmissionsAfterDueDate:
                exercise.buildAndTestStudentSubmissionsAfterDueDate != null && moment(exercise.buildAndTestStudentSubmissionsAfterDueDate).isValid()
                    ? moment(exercise.buildAndTestStudentSubmissionsAfterDueDate).toJSON()
                    : null,
        };
        // Remove exercise from template & solution participation to avoid circular dependency issues.
        // Also remove the results, as they can have circular structures as well and don't have to be saved here.
        if (copy.templateParticipation) {
            copy.templateParticipation = _omit(copy.templateParticipation, ['exercise', 'results']) as TemplateProgrammingExerciseParticipation;
        }
        if (copy.solutionParticipation) {
            copy.solutionParticipation = _omit(copy.solutionParticipation, ['exercise', 'results']) as SolutionProgrammingExerciseParticipation;
        }

        return copy;
    }

    /**
     * Convert all date fields of the programming exercise to momentJs date objects.
     * Note: This conversion could produce an invalid date if the date is malformatted.
     *
     * @param entity ProgrammingExercise
     */
    convertDateFromServer(entity: EntityResponseType) {
        const res = this.exerciseService.convertDateFromServer(entity);
        if (!res.body) {
            return res;
        }
        res.body.buildAndTestStudentSubmissionsAfterDueDate =
            res.body.buildAndTestStudentSubmissionsAfterDueDate != null ? moment(res.body.buildAndTestStudentSubmissionsAfterDueDate) : null;
        return res;
    }
}
