import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { BuildLogEntry } from 'app/entities/build-log';
import { SERVER_API_URL } from 'app/app.constants';

export interface IBuildLogService {
    getBuildLogs: (participationId: number) => Observable<BuildLogEntry[]>;
    getTestRepositoryBuildLogs: (participationId: number) => Observable<BuildLogEntry[]>;
}

@Injectable({ providedIn: 'root' })
export class BuildLogService implements IBuildLogService {
    private restResourceUrlBase = `${SERVER_API_URL}/api`;
    private assignmentResourceUrl = `${this.restResourceUrlBase}/repository`;
    private testRepositoryResourceUrl = `${this.restResourceUrlBase}/test-repository`;

    constructor(private http: HttpClient) {}

    getBuildLogs(participationId: number): Observable<BuildLogEntry[]> {
        return this.http.get<BuildLogEntry[]>(`${this.assignmentResourceUrl}/${participationId}/buildlogs`);
    }

    getTestRepositoryBuildLogs(participationId: number) {
        return this.http.get<BuildLogEntry[]>(`${this.testRepositoryResourceUrl}/${participationId}/buildlogs`);
    }
}
