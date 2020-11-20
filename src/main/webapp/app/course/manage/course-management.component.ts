import { AfterViewInit, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs/Subscription';
import { JhiEventManager } from 'ng-jhipster';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from './course-management.service';
import { ARTEMIS_DEFAULT_COLOR } from 'app/app.constants';
import { onError } from 'app/shared/util/global.utils';
import { Subject } from 'rxjs';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { tutorAssessmentTour } from 'app/guided-tour/tours/tutor-assessment-tour';
import { JhiAlertService } from 'ng-jhipster';
import { SortService } from 'app/shared/service/sort.service';

@Component({
    selector: 'jhi-course',
    templateUrl: './course-management.component.html',
    styles: ['.course-table {padding-bottom: 5rem}'],
    styleUrls: ['./course-management.component.scss'],
})
export class CourseManagementComponent implements OnInit, OnDestroy, AfterViewInit {
    predicate: string;
    reverse: boolean;
    showOnlyActive = true;
    showExamButton = true;

    courses: Course[];
    courseSemesters: string[];
    semesterCollapsed: { [key: string]: boolean };
    eventSubscriber: Subscription;

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    readonly ARTEMIS_DEFAULT_COLOR = ARTEMIS_DEFAULT_COLOR;

    courseForGuidedTour?: Course;

    constructor(
        private courseService: CourseManagementService,
        private jhiAlertService: JhiAlertService,
        private eventManager: JhiEventManager,
        private guidedTourService: GuidedTourService,
        private sortService: SortService,
    ) {
        this.predicate = 'id';
        // show the newest courses first and the oldest last
        this.reverse = false;
    }

    /**
     * loads all courses from courseService
     */
    loadAll() {
        this.courseService.getWithUserStats({ onlyActive: this.showOnlyActive }).subscribe(
            (res: HttpResponse<Course[]>) => {
                this.courses = res.body!;
                this.courseForGuidedTour = this.guidedTourService.enableTourForCourseOverview(this.courses, tutorAssessmentTour, true);
                this.courseSemesters = this.courses
                    // test courses get their own section later
                    .filter((c) => !c.testCourse)
                    .map((c) => c.semester ?? '')
                    // filter down to unique values
                    .filter((course, index, courses) => courses.indexOf(course) === index)
                    .sort((a, b) => {
                        // sort last if the semester is unset
                        if (a === '') {
                            return 1;
                        }
                        if (b === '') {
                            return -1;
                        }
                        // parse years in base 10 by extracting the two digits after the WS or SS prefix
                        const yearsCompared = parseInt(b.substr(2, 2), 10) - parseInt(a.substr(2, 2), 10);
                        if (yearsCompared !== 0) {
                            return yearsCompared;
                        }
                        // if years are the same, sort WS over SS
                        return a.substr(0, 2) === 'WS' ? -1 : 1;
                    });
                this.semesterCollapsed = {};
                let firstUncollapsed = false;
                for (const semester of this.courseSemesters) {
                    this.semesterCollapsed[semester] = firstUncollapsed;
                    firstUncollapsed = true;
                }
                // add an extra category for test courses
                if (this.courses.find((c) => c.testCourse) !== null) {
                    this.courseSemesters[this.courseSemesters.length] = 'test';
                    this.semesterCollapsed['test'] = false;
                }
            },
            (res: HttpErrorResponse) => onError(this.jhiAlertService, res),
        );
    }

    /**
     * loads all courses and subscribes to courseListModification
     */
    ngOnInit() {
        this.loadAll();
        this.registerChangeInCourses();
    }

    /**
     * notifies the guided-tour service that the current component has
     * been fully loaded
     */
    ngAfterViewInit(): void {
        this.guidedTourService.componentPageLoaded();
    }

    /**
     * unsubscribe on component destruction
     */
    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
        this.dialogErrorSource.unsubscribe();
    }

    /**
     * Returns the unique identifier for items in the collection
     * @param index - Index of a course in the collection
     * @param item - Current course
     */
    trackId(index: number, item: Course) {
        return item.id;
    }

    /**
     * subscribes to courseListModification event
     */
    registerChangeInCourses() {
        this.eventSubscriber = this.eventManager.subscribe('courseListModification', () => this.loadAll());
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
    }

    sortRows() {
        this.sortService.sortByProperty(this.courses, this.predicate, this.reverse);
    }

    /**
     * returns the current Date
     */
    get today(): Date {
        return new Date();
    }

    /**
     * toggles the attribute showOnlyActive and reloads all courses
     */
    toggleShowOnlyActive() {
        this.showOnlyActive = !this.showOnlyActive;
        this.loadAll();
    }
}
