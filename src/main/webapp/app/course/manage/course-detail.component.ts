import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs/Subscription';
import { JhiEventManager } from 'ng-jhipster';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from './course-management.service';
import { CachingStrategy } from 'app/shared/image/secured-image.component';
import { JhiAlertService } from 'ng-jhipster';
import { isOrion } from 'app/shared/orion/orion';
import * as moment from 'moment';
import { downloadZipFileFromResponse } from 'app/shared/util/download.util';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { tap } from 'rxjs/operators';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { Subject } from 'rxjs';
import { ButtonSize } from 'app/shared/components/button.component';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CourseManagementDetailViewDto } from 'app/course/manage/course-management-detail-view-dto.model';

type CourseArchiveState = {
    exportState: 'COMPLETED' | 'RUNNING';
    progress: string;
};

@Component({
    selector: 'jhi-course-detail',
    templateUrl: './course-detail.component.html',
    styleUrls: ['./course-detail.component.scss'],
})
export class CourseDetailComponent implements OnInit, OnDestroy {
    ButtonSize = ButtonSize;
    ActionType = ActionType;
    readonly isOrion = isOrion;
    public refreshingCourse = false;

    CachingStrategy = CachingStrategy;
    course: CourseManagementDetailViewDto;
    oldCourse: Course; // TODO Delete when handling the course archival
    private eventSubscriber: Subscription;

    courseIsBeingArchived = false;
    archiveCourseButtonText = this.getArchiveCourseText();

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();
    paramSub: Subscription;

    firstTitle = 'Total Assessments';
    secondTitle = 'Total Complaints';
    thirdTitle = 'More Feedback Requests';
    fourthTitle = 'Average Student Score';

    constructor(
        private eventManager: JhiEventManager,
        private courseService: CourseManagementService,
        private route: ActivatedRoute,
        private router: Router,
        private jhiAlertService: JhiAlertService,
        private websocketService: JhiWebsocketService,
        private translateService: TranslateService,
        private modalService: NgbModal,
    ) {}

    /**
     * On init load the course information and subscribe to listen for changes in courses.
     */
    ngOnInit() {
        // There is no course 0 -> will get no course
        let courseId = 0;
        this.paramSub = this.route.params.subscribe((params) => {
            courseId = params['courseId'];
        });
        this.courseService.getCourseForDetailView(courseId).subscribe((courseResponse: HttpResponse<CourseManagementDetailViewDto>) => {
            this.course = courseResponse.body!;
            // this.registerChangeInCourses();
            this.registerCourseArchiveWebsocket();
        });

        // update the span title on each language change
        this.translateService.onLangChange.subscribe(() => {
            if (!this.courseIsBeingArchived) {
                this.archiveCourseButtonText = this.getArchiveCourseText();
            }
        });
    }

    /**
     * Subscribe to changes in courses and reload the course after a change.
     */
    registerChangeInCourses() {
        this.eventSubscriber = this.eventManager.subscribe('courseListModification', () => {
            this.courseService.find(this.course.id).subscribe((courseResponse: HttpResponse<Course>) => {
                this.oldCourse = courseResponse.body!;
            });
        });
    }

    private registerCourseArchiveWebsocket() {
        const topic = CourseDetailComponent.getCourseArchiveStateTopic(this.course.id);
        this.websocketService.subscribe(topic);
        this.websocketService
            .receive(topic)
            .pipe(tap((courseArchiveState: CourseArchiveState) => this.handleCourseArchiveStateChanges(courseArchiveState)))
            .subscribe();
    }

    private handleCourseArchiveStateChanges(courseArchiveState: CourseArchiveState) {
        const { exportState, progress } = courseArchiveState;
        this.courseIsBeingArchived = exportState === 'RUNNING';
        this.archiveCourseButtonText = exportState === 'RUNNING' ? progress : this.getArchiveCourseText();

        if (exportState === 'COMPLETED') {
            this.jhiAlertService.success('artemisApp.course.archive.archiveCourseSuccess');
            this.eventManager.broadcast('courseListModification');
        }
    }

    private getArchiveCourseText() {
        return this.translateService.instant('artemisApp.course.archive.archiveCourse');
    }

    /**
     * Register for the currently loaded course.
     */
    registerForCourse() {
        this.courseService.registerForCourse(this.course.id).subscribe(
            (userResponse) => {
                if (userResponse.body != undefined) {
                    const message = 'Registered user for course ' + this.course.title;
                    const jhiAlert = this.jhiAlertService.info(message);
                    jhiAlert.msg = message;
                }
            },
            (error: HttpErrorResponse) => {
                const errorMessage = error.headers.get('X-artemisApp-message')!;
                // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
                const jhiAlert = this.jhiAlertService.error(errorMessage);
                jhiAlert.msg = errorMessage;
            },
        );
    }

    /**
     * On destroy unsubscribe all subscriptions.
     */
    ngOnDestroy() {
        if (this.paramSub) {
            this.paramSub.unsubscribe();
        }
        // this.eventManager.destroy(this.eventSubscriber);
        this.dialogErrorSource.unsubscribe();
        this.websocketService.unsubscribe(CourseDetailComponent.getCourseArchiveStateTopic(this.course.id));
    }

    private static getCourseArchiveStateTopic(courseId: number) {
        return '/topic/courses/' + courseId + '/export-course';
    }

    canArchiveCourse() {
        // A course can only be archived if it's over.
        const isCourseOver = this.course.endDate?.isBefore(moment()) ?? false;
        return this.course.isAtLeastInstructor && isCourseOver;
    }

    /**
     * Open a modal that requires the user's confirmation.
     * @param content the modal content
     */
    openArchieCourseModal(content: any) {
        this.modalService.open(content).result.then(
            (result: string) => {
                if (result === 'archive') {
                    this.archiveCourse();
                }
            },
            () => {},
        );
    }

    archiveCourse() {
        this.courseService.archiveCourse(this.course.id).subscribe();
    }

    downloadCourseArchive() {
        this.courseService.downloadCourseArchive(this.course.id).subscribe(
            (response) => downloadZipFileFromResponse(response),
            () => this.jhiAlertService.error('artemisApp.course.archive.archiveDownloadError'),
        );
    }

    canDownloadArchive() {
        const hasArchive = !!this.course.courseArchivePath && this.course.courseArchivePath.length > 0;
        // You can only download one if the path to the archive is present
        return this.course.isAtLeastInstructor && hasArchive;
    }

    canCleanupCourse() {
        // A course can only be cleaned up if the course has been archived.
        const canCleanup = !!this.course.courseArchivePath && this.course.courseArchivePath.length > 0;
        return this.course.isAtLeastInstructor && canCleanup;
    }

    cleanupCourse() {
        this.courseService.cleanupCourse(this.course.id).subscribe(
            () => {
                this.jhiAlertService.success('artemisApp.programmingExercise.cleanup.successMessage');
                this.dialogErrorSource.next('');
            },
            (error) => {
                this.dialogErrorSource.next(error.error.title);
            },
        );
    }

    /**
     * Deletes the course
     * @param courseId id the course that will be deleted
     */
    deleteCourse(courseId: number) {
        this.courseService.delete(courseId).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'courseListModification',
                    content: 'Deleted an course',
                });
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
        this.router.navigate(['/course-management']);
    }
}
