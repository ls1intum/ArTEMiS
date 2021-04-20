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
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { Subject } from 'rxjs';
import { ButtonSize } from 'app/shared/components/button.component';
import { CourseManagementDetailViewDto } from 'app/course/manage/course-management-detail-view-dto.model';
import { ARTEMIS_DEFAULT_COLOR } from 'app/app.constants';

@Component({
    selector: 'jhi-course-detail',
    templateUrl: './course-detail.component.html',
    styleUrls: ['./course-detail.component.scss'],
})
export class CourseDetailComponent implements OnInit, OnDestroy {
    readonly ARTEMIS_DEFAULT_COLOR = ARTEMIS_DEFAULT_COLOR;

    ButtonSize = ButtonSize;
    ActionType = ActionType;
    readonly isOrion = isOrion;
    public refreshingCourse = false;

    CachingStrategy = CachingStrategy;
    courseDTO: CourseManagementDetailViewDto;
    course: Course;
    private eventSubscriber: Subscription;

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();
    paramSub: Subscription;

    constructor(
        private eventManager: JhiEventManager,
        private courseService: CourseManagementService,
        private route: ActivatedRoute,
        private router: Router,
        private jhiAlertService: JhiAlertService,
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
            this.courseDTO = courseResponse.body!;
            this.course = this.courseDTO.course;
            this.registerChangeInCourses();
        });
    }

    /**
     * Subscribe to changes in courses and reload the course after a change.
     */
    registerChangeInCourses() {
        this.eventSubscriber = this.eventManager.subscribe('courseListModification', () => {
            this.courseService.find(this.courseDTO.course.id!).subscribe((courseResponse: HttpResponse<Course>) => {
                this.course = courseResponse.body!;
            });
        });
    }

    /**
     * On destroy unsubscribe all subscriptions.
     */
    ngOnDestroy() {
        if (this.paramSub) {
            this.paramSub.unsubscribe();
        }
        this.eventManager.destroy(this.eventSubscriber);
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
