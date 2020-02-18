import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { createRequestOption } from 'app/shared/util/request-util';
import { User } from 'app/core/user/user.model';
import { SERVER_API_URL } from 'app/app.constants';
import { Course } from 'app/entities/course';
import { Exercise } from 'app/entities/exercise';
import { TeamSearchUser } from 'app/entities/team-search-student/team-search-student.model';

@Injectable({ providedIn: 'root' })
export class UserService {
    public resourceUrl = SERVER_API_URL + 'api/users';

    constructor(private http: HttpClient) {}

    create(user: User): Observable<HttpResponse<User>> {
        return this.http.post<User>(this.resourceUrl, user, { observe: 'response' });
    }

    update(user: User): Observable<HttpResponse<User>> {
        return this.http.put<User>(this.resourceUrl, user, { observe: 'response' });
    }

    find(login: string): Observable<HttpResponse<User>> {
        return this.http.get<User>(`${this.resourceUrl}/${login}`, { observe: 'response' });
    }

    query(req?: any): Observable<HttpResponse<User[]>> {
        const options = createRequestOption(req);
        return this.http.get<User[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(login: string): Observable<HttpResponse<void>> {
        return this.http.delete<void>(`${this.resourceUrl}/${login}`, { observe: 'response' });
    }

    updateUserNotificationDate(): Observable<HttpResponse<User>> {
        return this.http.put<User>(`${this.resourceUrl}/notification-date`, null, { observe: 'response' });
    }

    authorities(): Observable<string[]> {
        return this.http.get<string[]>(SERVER_API_URL + 'api/users/authorities');
    }

    searchInCourseForExerciseTeam(course: Course, exercise: Exercise, loginOrName: string): Observable<HttpResponse<TeamSearchUser[]>> {
        return this.http.get<TeamSearchUser[]>(`${SERVER_API_URL}api/courses/${course.id}/exercises/${exercise.id}/team-search-users?loginOrName=${loginOrName}`, {
            observe: 'response',
        });
    }
}
