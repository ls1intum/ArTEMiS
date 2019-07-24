import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { SERVER_API_URL } from '../../app.constants';

import { ModelingExercise } from './modeling-exercise.model';
import { ExerciseService } from 'app/entities/exercise';
import { ModelingStatistic } from 'app/entities/modeling-statistic';
import { createRequestOption } from 'app/shared';

export type EntityResponseType = HttpResponse<ModelingExercise>;
export type EntityArrayResponseType = HttpResponse<ModelingExercise[]>;

@Injectable({ providedIn: 'root' })
export class ModelingExerciseService {
    public resourceUrl = SERVER_API_URL + 'api/modeling-exercises';

    constructor(private http: HttpClient, private exerciseService: ExerciseService) {
        this.exerciseService = exerciseService;
    }

    create(modelingExercise: ModelingExercise): Observable<EntityResponseType> {
        const copy = this.exerciseService.convertDateFromClient(modelingExercise);
        return this.http.post<ModelingExercise>(this.resourceUrl, copy, { observe: 'response' }).map((res: EntityResponseType) => this.exerciseService.convertDateFromServer(res));
    }

    update(modelingExercise: ModelingExercise, req?: any): Observable<EntityResponseType> {
        const options = createRequestOption(req);
        const copy = this.exerciseService.convertDateFromClient(modelingExercise);
        return this.http
            .put<ModelingExercise>(this.resourceUrl, copy, { params: options, observe: 'response' })
            .map((res: EntityResponseType) => this.exerciseService.convertDateFromServer(res));
    }

    find(modelingExerciseId: number): Observable<EntityResponseType> {
        return this.http
            .get<ModelingExercise>(`${this.resourceUrl}/${modelingExerciseId}`, { observe: 'response' })
            .map((res: EntityResponseType) => this.exerciseService.convertDateFromServer(res));
    }

    getStatistics(modelingExerciseId: number): Observable<HttpResponse<ModelingStatistic>> {
        return this.http.get<ModelingStatistic>(`${this.resourceUrl}/${modelingExerciseId}/statistics`, { observe: 'response' });
    }

    delete(modelingExerciseId: number): Observable<HttpResponse<void>> {
        return this.http.delete<void>(`${this.resourceUrl}/${modelingExerciseId}`, { observe: 'response' });
    }
}
