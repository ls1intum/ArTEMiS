import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, shareReplay, filter } from 'rxjs/operators';
import * as moment from 'moment';

import { SERVER_API_URL } from 'app/app.constants';
import { Team, TeamAssignmentPayload, TeamImportStrategyType } from 'app/entities/team.model';
import { Exercise } from 'app/entities/exercise.model';
import { Course } from 'app/entities/course.model';
import { TeamSearchUser } from 'app/entities/team-search-user.model';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { User } from 'app/core/user/user.model';
import { AccountService } from 'app/core/auth/account.service';

export type TeamResponse = HttpResponse<Team>;
export type TeamArrayResponse = HttpResponse<Team[]>;

const teamAssignmentUpdatesWebsocketTopic = '/user/topic/team-assignments';

export interface ITeamService {
    create(exercise: Exercise, team: Team): Observable<TeamResponse>;

    update(exercise: Exercise, team: Team): Observable<TeamResponse>;

    find(exercise: Exercise, teamId: number): Observable<TeamResponse>;

    findAllByExerciseId(exerciseId: number): Observable<HttpResponse<Team[]>>;

    delete(exercise: Exercise, teamId: number): Observable<HttpResponse<any>>;

    existsByShortName(exercise: Exercise, shortName: string): Observable<HttpResponse<boolean>>;

    searchInCourseForExerciseTeam(course: Course, exercise: Exercise, loginOrName: string): Observable<HttpResponse<TeamSearchUser[]>>;

    importTeamsFromSourceExercise(exercise: Exercise, sourceExercise: Exercise, importStrategy: TeamImportStrategyType): Observable<HttpResponse<Team[]>>;
}

@Injectable({ providedIn: 'root' })
export class TeamService implements ITeamService {
    private teamAssignmentUpdates$: Observable<TeamAssignmentPayload>;

    constructor(protected http: HttpClient, private websocketService: JhiWebsocketService, private accountService: AccountService) {}

    private static resourceUrl(exerciseId: number) {
        return `${SERVER_API_URL}api/exercises/${exerciseId}/teams`;
    }

    create(exercise: Exercise, team: Team): Observable<TeamResponse> {
        const copy = TeamService.convertDateFromClient(team);
        return this.http
            .post<Team>(TeamService.resourceUrl(exercise.id), copy, { observe: 'response' })
            .pipe(map((res: TeamResponse) => TeamService.convertDateFromServer(res)));
    }

    update(exercise: Exercise, team: Team): Observable<TeamResponse> {
        const copy = TeamService.convertDateFromClient(team);
        return this.http
            .put<Team>(`${TeamService.resourceUrl(exercise.id)}/${team.id}`, copy, { observe: 'response' })
            .pipe(map((res: TeamResponse) => TeamService.convertDateFromServer(res)));
    }

    find(exercise: Exercise, teamId: number): Observable<TeamResponse> {
        return this.http
            .get<Team>(`${TeamService.resourceUrl(exercise.id)}/${teamId}`, { observe: 'response' })
            .pipe(map((res: TeamResponse) => TeamService.convertDateFromServer(res)));
    }

    findAllByExerciseId(exerciseId: number): Observable<TeamArrayResponse> {
        return this.http
            .get<Team[]>(TeamService.resourceUrl(exerciseId), { observe: 'response' })
            .pipe(map((res: TeamArrayResponse) => TeamService.convertDateArrayFromServer(res)));
    }

    delete(exercise: Exercise, teamId: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${TeamService.resourceUrl(exercise.id)}/${teamId}`, { observe: 'response' });
    }

    existsByShortName(exercise: Exercise, shortName: string): Observable<HttpResponse<boolean>> {
        return this.http.get<boolean>(`${TeamService.resourceUrl(exercise.id)}/exists?shortName=${shortName}`, { observe: 'response' });
    }

    searchInCourseForExerciseTeam(course: Course, exercise: Exercise, loginOrName: string): Observable<HttpResponse<TeamSearchUser[]>> {
        const url = `${SERVER_API_URL}api/courses/${course.id}/exercises/${exercise.id}/team-search-users?loginOrName=${loginOrName}`;
        return this.http.get<TeamSearchUser[]>(url, { observe: 'response' });
    }

    importTeamsFromSourceExercise(exercise: Exercise, sourceExercise: Exercise, importStrategyType: TeamImportStrategyType) {
        return this.http.put<Team[]>(
            `${TeamService.resourceUrl(exercise.id)}/import-from-exercise/${sourceExercise.id}?importStrategyType=${importStrategyType}`,
            {},
            { observe: 'response' },
        );
    }

    /**
     * Returns a promise of an observable that emits team assignment updates
     *
     * 1. If there is already an update stream, return it
     * 2. If there is no update stream yet but the websocket client is connected, return a new stream
     * 3. If the websocket client is not yet connected, wait for the user to log in and for the websocket client
     *    to connect, then return a new stream
     */
    get teamAssignmentUpdates(): Promise<Observable<TeamAssignmentPayload>> {
        return new Promise((resolve) => {
            if (this.teamAssignmentUpdates$) {
                resolve(this.teamAssignmentUpdates$);
            } else if (this.websocketService.stompClient?.connected) {
                resolve(this.newTeamAssignmentUpdates$);
            } else {
                this.accountService
                    .getAuthenticationState()
                    .pipe(filter((user: User | null) => Boolean(user)))
                    .subscribe(() => {
                        setTimeout(() => {
                            this.websocketService.bind('connect', () => {
                                resolve(this.newTeamAssignmentUpdates$);
                            });
                        }, 500);
                    });
            }
        });
    }

    /**
     * Subscribes to the team assignment updates websocket topic and stores the stream in teamAssignmentUpdates$
     */
    private get newTeamAssignmentUpdates$(): Observable<TeamAssignmentPayload> {
        if (this.teamAssignmentUpdates$) {
            this.websocketService.unsubscribe(teamAssignmentUpdatesWebsocketTopic);
        }
        this.websocketService.subscribe(teamAssignmentUpdatesWebsocketTopic);
        this.teamAssignmentUpdates$ = this.websocketService.receive(teamAssignmentUpdatesWebsocketTopic).pipe(shareReplay(16));
        return this.teamAssignmentUpdates$;
    }

    /**
     * Helper methods for date conversion from server and client
     */
    private static convertDateArrayFromServer(res: TeamArrayResponse): TeamArrayResponse {
        if (res.body) {
            res.body.map((team: Team) => this.convertDatesForTeamFromServer(team));
        }
        return res;
    }

    private static convertDateFromServer(res: TeamResponse): TeamResponse {
        if (res.body) {
            res.body.createdDate = moment(res.body.createdDate);
            res.body.lastModifiedDate = res.body.lastModifiedDate != null ? moment(res.body.lastModifiedDate) : null;
        }
        return res;
    }

    private static convertDatesForTeamFromServer(team: Team): Team {
        team.createdDate = moment(team.createdDate);
        team.lastModifiedDate = team.lastModifiedDate != null ? moment(team.lastModifiedDate) : null;
        return team;
    }

    private static convertDateFromClient(team: Team): Team {
        return Object.assign({}, team, {
            createdDate: moment(team.createdDate).isValid() ? moment(team.createdDate).toJSON() : null,
            lastModifiedDate: team.lastModifiedDate != null && moment(team.lastModifiedDate).isValid() ? moment(team.lastModifiedDate).toJSON() : null,
        });
    }
}
