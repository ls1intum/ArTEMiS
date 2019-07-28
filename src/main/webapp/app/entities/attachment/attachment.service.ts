import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { Attachment } from 'app/entities/attachment';

type EntityResponseType = HttpResponse<Attachment>;
type EntityArrayResponseType = HttpResponse<Attachment[]>;

@Injectable({ providedIn: 'root' })
export class AttachmentService {
    public resourceUrl = SERVER_API_URL + 'api/attachments';

    constructor(protected http: HttpClient) {}

    create(attachment: Attachment): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(attachment);
        return this.http.post<Attachment>(this.resourceUrl, copy, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(attachment: Attachment, req?: any): Observable<EntityResponseType> {
        const options = createRequestOption(req);
        const copy = this.convertDateFromClient(attachment);
        return this.http.put<Attachment>(this.resourceUrl, copy, { params: options, observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<Attachment>(`${this.resourceUrl}/${id}`, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<Attachment[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    findAllByLectureId(lectureId: number): Observable<EntityArrayResponseType> {
        return this.http
            .get<Attachment[]>(`api/lectures/${lectureId}/attachments`, { observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    /**
     * Requests an access token from the server to download the file. If the access token was generated successfully, the file is then downloaded.
     *
     * @param downloadUrl url that is stored in the attachment model
     */
    downloadAttachment(downloadUrl: string) {
        return this.http
            .get('api/files/attachments/access-token', { observe: 'response', responseType: 'text' })
            .switchMap(result => this.http.get(`${downloadUrl}?access_token=${result.body}`, { observe: 'response', responseType: 'blob' }))
            .do(response => {
                const blob = new Blob([response.body!], { type: response.headers.get('content-type')! });
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.setAttribute('href', url);
                link.setAttribute('download', response.headers.get('filename')!);
                document.body.appendChild(link); // Required for FF
                link.click();
                window.URL.revokeObjectURL(url);
            });
    }

    protected convertDateFromClient(attachment: Attachment): Attachment {
        const copy: Attachment = Object.assign({}, attachment, {
            releaseDate: attachment.releaseDate != null && attachment.releaseDate.isValid() ? attachment.releaseDate.toJSON() : null,
            uploadDate: attachment.uploadDate != null && attachment.uploadDate.isValid() ? attachment.uploadDate.toJSON() : null,
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.releaseDate = res.body.releaseDate != null ? moment(res.body.releaseDate) : null;
            res.body.uploadDate = res.body.uploadDate != null ? moment(res.body.uploadDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((attachment: Attachment) => {
                attachment.releaseDate = attachment.releaseDate != null ? moment(attachment.releaseDate) : null;
                attachment.uploadDate = attachment.uploadDate != null ? moment(attachment.uploadDate) : null;
            });
        }
        return res;
    }
}
