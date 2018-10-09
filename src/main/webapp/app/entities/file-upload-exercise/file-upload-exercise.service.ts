import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { SERVER_API_URL } from '../../app.constants';

import * as moment from 'moment';

import { FileUploadExercise } from './file-upload-exercise.model';
import { createRequestOption } from '../../shared';
import { Exercise } from 'app/entities/exercise';

export type EntityResponseType = HttpResponse<FileUploadExercise>;
export type EntityArrayResponseType = HttpResponse<FileUploadExercise[]>;

@Injectable()
export class FileUploadExerciseService {
    private resourceUrl = SERVER_API_URL + 'api/file-upload-exercises';

    constructor(private http: HttpClient) {}

    create(fileUploadExercise: FileUploadExercise): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(fileUploadExercise);
        return this.http
            .post<FileUploadExercise>(this.resourceUrl, copy, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    update(fileUploadExercise: FileUploadExercise): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(fileUploadExercise);
        return this.http
            .put<FileUploadExercise>(this.resourceUrl, copy, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<FileUploadExercise>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .map((res: EntityResponseType) => this.convertDateFromServer(res));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<FileUploadExercise[]>(this.resourceUrl, { params: options, observe: 'response' })
            .map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res));
    }

    delete(id: number): Observable<HttpResponse<void>> {
        return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    private convertDateFromClient(exercise: FileUploadExercise): FileUploadExercise {
        const copy: FileUploadExercise = Object.assign({}, exercise, {
            releaseDate: exercise.releaseDate != null && exercise.releaseDate.isValid() ? exercise.releaseDate.toJSON() : null,
            dueDate: exercise.dueDate != null && exercise.dueDate.isValid() ? exercise.dueDate.toJSON() : null
        });
        return copy;
    }

    private convertDateFromServer(res: EntityResponseType): EntityResponseType {
        res.body.releaseDate = res.body.releaseDate != null ? moment(res.body.releaseDate) : null;
        res.body.dueDate = res.body.dueDate != null ? moment(res.body.dueDate) : null;
        return res;
    }

    private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        res.body.forEach((exercise: Exercise) => {
            exercise.releaseDate = exercise.releaseDate != null ? moment(exercise.releaseDate) : null;
            exercise.dueDate = exercise.dueDate != null ? moment(exercise.dueDate) : null;
        });
        return res;
    }
}
