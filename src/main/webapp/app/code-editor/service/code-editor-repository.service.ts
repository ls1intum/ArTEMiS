import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { pipe, Subject, throwError, UnaryFunction } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/Observable';

import { BuildLogEntry } from 'app/entities/build-log';
import { FileType } from 'app/entities/ace-editor/file-change.model';
import { JhiWebsocketService } from 'app/core';
import { DomainChange, DomainDependentEndpoint, DomainService } from 'app/code-editor/service';
import { CommitState } from 'app/code-editor';
import { CodeEditorConflictStateService, GitConflictState } from 'app/code-editor/service/code-editor-conflict-state.service';

export enum DomainType {
    PARTICIPATION = 'PARTICIPATION',
    TEST_REPOSITORY = 'TEST_REPOSITORY',
}

export interface ICodeEditorRepositoryFileService {
    getRepositoryContent: () => Observable<{ [fileName: string]: FileType }>;
    getFile: (fileName: string) => Observable<{ fileContent: string }>;
    createFile: (fileName: string) => Observable<void>;
    createFolder: (folderName: string) => Observable<void>;
    updateFileContent: (fileName: string, fileContent: string) => Observable<Object>;
    updateFiles: (fileUpdates: Array<{ fileName: string; fileContent: string }>) => Observable<{ [fileName: string]: string | null }>;
    renameFile: (filePath: string, newFileName: string) => Observable<void>;
    deleteFile: (filePath: string) => Observable<void>;
}

export interface ICodeEditorRepositoryService {
    getStatus: () => Observable<{ repositoryStatus: string }>;
    commit: () => Observable<void>;
    pull: () => Observable<void>;
    resetRepository: () => Observable<void>;
}

export interface IBuildLogService {
    getBuildLogs: () => Observable<BuildLogEntry[]>;
}

@Injectable({ providedIn: 'root' })
export class CodeEditorRepositoryService extends DomainDependentEndpoint implements ICodeEditorRepositoryService {
    constructor(http: HttpClient, jhiWebsocketService: JhiWebsocketService, domainService: DomainService, private conflictService: CodeEditorConflictStateService) {
        super(http, jhiWebsocketService, domainService);
    }

    getStatus = () => {
        return this.http.get<any>(this.restResourceUrl!).pipe(
            this.handleErrorResponse<{ repositoryStatus: string }>(),
            tap(({ repositoryStatus }) => {
                if (repositoryStatus !== CommitState.CONFLICT) {
                    this.conflictService.notifyConflictState(GitConflictState.OK);
                }
            }),
        );
    };

    commit = () => {
        return this.http.post<void>(`${this.restResourceUrl}/commit`, {}).pipe(this.handleErrorResponse());
    };

    pull = () => {
        return this.http.get<void>(`${this.restResourceUrl}/pull`, {}).pipe(this.handleErrorResponse());
    };

    // TODO: Maybe we don't have to check for the error here?
    resetRepository = () => {
        return this.http.post<void>(`${this.restResourceUrl}/reset`, {});
    };

    private handleErrorResponse = <T>(): UnaryFunction<Observable<T>, Observable<T>> =>
        pipe(
            catchError((err: HttpErrorResponse) => {
                if (err.status === 409) {
                    this.conflictService.notifyConflictState(GitConflictState.CHECKOUT_CONFLICT);
                }
                return throwError(err);
            }),
        );
}

@Injectable({ providedIn: 'root' })
export class CodeEditorBuildLogService extends DomainDependentEndpoint implements IBuildLogService {
    constructor(http: HttpClient, jhiWebsocketService: JhiWebsocketService, domainService: DomainService) {
        super(http, jhiWebsocketService, domainService);
    }

    getBuildLogs = () => {
        return this.http.get<BuildLogEntry[]>(`${this.restResourceUrl}/buildlogs`, {});
    };
}

@Injectable({ providedIn: 'root' })
export class CodeEditorRepositoryFileService extends DomainDependentEndpoint implements ICodeEditorRepositoryFileService, OnDestroy {
    private fileUpdateSubject = new Subject<{ [fileName: string]: string | null }>();
    private fileUpdateUrl: string;

    constructor(http: HttpClient, jhiWebsocketService: JhiWebsocketService, domainService: DomainService, private conflictService: CodeEditorConflictStateService) {
        super(http, jhiWebsocketService, domainService);
    }

    ngOnDestroy() {
        super.ngOnDestroy();
        this.jhiWebsocketService.unsubscribe(this.fileUpdateUrl);
    }

    setDomain(domain: DomainChange) {
        super.setDomain(domain);
        if (this.fileUpdateSubject) {
            this.fileUpdateSubject.complete();
        }
        if (this.fileUpdateUrl) {
            this.jhiWebsocketService.unsubscribe(this.fileUpdateUrl);
        }
        this.fileUpdateUrl = `${this.websocketResourceUrlReceive}/files`;
    }

    getRepositoryContent = () => {
        return this.http.get<{ [fileName: string]: FileType }>(`${this.restResourceUrl}/files`).pipe(this.handleErrorResponse<{ [fileName: string]: FileType }>());
    };

    getFile = (fileName: string) => {
        return this.http.get(`${this.restResourceUrl}/file`, { params: new HttpParams().set('file', fileName), responseType: 'text' }).pipe(
            map(data => ({ fileContent: data })),
            this.handleErrorResponse<{ fileContent: string }>(),
        );
    };

    createFile = (fileName: string) => {
        return this.http.post<void>(`${this.restResourceUrl}/file`, '', { params: new HttpParams().set('file', fileName) }).pipe(this.handleErrorResponse());
    };

    createFolder = (folderName: string) => {
        return this.http.post<void>(`${this.restResourceUrl}/folder`, '', { params: new HttpParams().set('folder', folderName) }).pipe(this.handleErrorResponse());
    };

    updateFileContent = (fileName: string, fileContent: string) => {
        return this.http
            .put(`${this.restResourceUrl}/file`, fileContent, {
                params: new HttpParams().set('file', fileName),
            })
            .pipe(this.handleErrorResponse());
    };

    // TODO: We also have to handle receiving the conflict from the websocket
    updateFiles = (fileUpdates: Array<{ fileName: string; fileContent: string }>) => {
        if (this.fileUpdateSubject) {
            this.fileUpdateSubject.complete();
        }
        if (this.fileUpdateUrl) {
            this.jhiWebsocketService.unsubscribe(this.fileUpdateUrl);
        }
        this.fileUpdateSubject = new Subject<{ [p: string]: string | null }>();

        this.jhiWebsocketService.subscribe(this.fileUpdateUrl);
        this.jhiWebsocketService
            .receive(this.fileUpdateUrl)
            .pipe(this.handleWebsocketErrorResponse())
            .subscribe(res => this.fileUpdateSubject.next(res), err => this.fileUpdateSubject.error(err));
        this.jhiWebsocketService.send(`${this.websocketResourceUrlSend}/files`, fileUpdates);
        return this.fileUpdateSubject as Observable<{ [fileName: string]: string | null }>;
    };

    renameFile = (currentFilePath: string, newFilename: string) => {
        return this.http.post<void>(`${this.restResourceUrl}/rename-file`, { currentFilePath, newFilename }).pipe(this.handleErrorResponse());
    };

    deleteFile = (fileName: string) => {
        return this.http.delete<void>(`${this.restResourceUrl}/file`, { params: new HttpParams().set('file', fileName) }).pipe(this.handleErrorResponse());
    };

    private handleErrorResponse = <T>(): UnaryFunction<Observable<T>, Observable<T>> =>
        pipe(
            catchError((err: HttpErrorResponse) => {
                if (err.status === 409) {
                    this.conflictService.notifyConflictState(GitConflictState.CHECKOUT_CONFLICT);
                }
                return throwError(err);
            }),
        );

    private handleWebsocketErrorResponse = <T>(): UnaryFunction<Observable<T>, Observable<T>> =>
        pipe(
            catchError((err: { error: string }) => {
                if (err.error === 'checkoutConflict') {
                    this.conflictService.notifyConflictState(GitConflictState.CHECKOUT_CONFLICT);
                }
                return throwError(err);
            }),
        );
}
