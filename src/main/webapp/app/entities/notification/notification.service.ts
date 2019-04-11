import { Injectable, EventEmitter } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';
import { Notification } from 'app/entities/notification';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { AccountService, JhiWebsocketService, User } from 'app/core';
import { Router } from '@angular/router';
import { Course } from 'app/entities/course';
import { GroupNotification, GroupNotificationType } from 'app/entities/group-notification';

type EntityResponseType = HttpResponse<Notification>;
type EntityArrayResponseType = HttpResponse<Notification[]>;

@Injectable({ providedIn: 'root' })
export class NotificationService {
    public resourceUrl = SERVER_API_URL + 'api/notifications';
    notificationObserver: BehaviorSubject<Notification>;
    subscribedTopics: string[] = [];

    constructor(private jhiWebsocketService: JhiWebsocketService, private router: Router, private http: HttpClient, private accountService: AccountService) {}

    create(notification: Notification): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(notification);
        return this.http.post<Notification>(this.resourceUrl, copy, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(notification: Notification): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(notification);
        return this.http.put<Notification>(this.resourceUrl, copy, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<Notification>(`${this.resourceUrl}/${id}`, { observe: 'response' }).pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<Notification[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    getRecentNotificationsForUser(): Observable<EntityArrayResponseType> {
        return this.http
            .get<Notification[]>(`${this.resourceUrl}/for-user`, { observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    protected convertDateFromClient(notification: Notification): Notification {
        const copy: Notification = Object.assign({}, notification, {
            notificationDate: notification.notificationDate != null && notification.notificationDate.isValid() ? notification.notificationDate.toJSON() : null,
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.notificationDate = res.body.notificationDate != null ? moment(res.body.notificationDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((notification: Notification) => {
                notification.notificationDate = notification.notificationDate != null ? moment(notification.notificationDate) : null;
            });
        }
        return res;
    }

    subscribeUserNotifications(): Promise<any> {
        return new Promise((resolve, reject) => {
            this.accountService
                .identity()
                .then((user: User) => {
                    if (!this.notificationObserver) {
                        this.notificationObserver = new BehaviorSubject<Notification>(null);
                    }
                    const userTopic = `/topic/user/${user.id}/notifications`;
                    if (!this.subscribedTopics.includes(userTopic)) {
                        this.subscribedTopics.push(userTopic);
                        this.jhiWebsocketService.subscribe(userTopic);
                        this.jhiWebsocketService.receive(userTopic).subscribe((notification: Notification) => {
                            this.notificationObserver.next(notification);
                        });
                        resolve();
                    }
                })
                .catch(error => reject(error));
        });
    }

    public handleCourseNotifications(course: Course): void {
        if (!this.notificationObserver) {
            this.notificationObserver = new BehaviorSubject<Notification>(null);
        }
        let courseTopic = `/topic/course/${course.id}/${GroupNotificationType.STUDENT}`;
        if (this.accountService.isAtLeastInstructorInCourse(course)) {
            courseTopic = `/topic/course/${course.id}/${GroupNotificationType.INSTRUCTOR}`;
        } else if (this.accountService.isAtLeastTutorInCourse(course)) {
            courseTopic = `/topic/course/${course.id}/${GroupNotificationType.TA}`;
        }
        if (!this.subscribedTopics.includes(courseTopic)) {
            this.subscribedTopics.push(courseTopic);
            this.jhiWebsocketService.subscribe(courseTopic);
            this.jhiWebsocketService.receive(courseTopic).subscribe((notification: Notification) => {
                this.notificationObserver.next(notification);
            });
        }
    }

    public handleCoursesNotifications(courses: Course[]): void {
        courses.forEach((course: Course) => {
            this.handleCourseNotifications(course);
        });
    }

    subscribeToSocketMessages(): BehaviorSubject<Notification> {
        if (!this.notificationObserver) {
            this.notificationObserver = new BehaviorSubject<Notification>(null);
        }
        return this.notificationObserver;
    }

    interpretNotification(notification: GroupNotification): void {
        const target = JSON.parse(notification.target);
        const courseId = target.course || notification.course.id;
        this.router.navigate([target.mainPage, courseId, target.entity, target.id]);
    }
}
