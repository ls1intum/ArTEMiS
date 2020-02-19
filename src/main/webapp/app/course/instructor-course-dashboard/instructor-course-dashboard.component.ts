import { AlertService } from 'app/core/alert/alert.service';
import { Component, OnInit } from '@angular/core';
import { Course } from 'app/entities/course.model';
import { CourseService } from 'app/course/manage/course.service';
import { catchError, map, tap } from 'rxjs/operators';
import { of, zip } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { StatsForDashboard } from 'app/course/instructor-course-dashboard/stats-for-dashboard.model';
import { ResultService } from 'app/exercises/shared/result/result.service';
import { getIcon, getIconTooltip } from 'app/entities/exercise.model';

@Component({
    selector: 'jhi-instructor-course-dashboard',
    templateUrl: './instructor-course-dashboard.component.html',
    providers: [],
})
export class InstructorCourseDashboardComponent implements OnInit {
    course: Course;

    getIcon = getIcon;
    getIconTooltip = getIconTooltip;

    isLoading = true;
    exercisesSortingPredicate = 'assessmentDueDate';
    exercisesReverseOrder = false;

    stats = new StatsForDashboard();
    dataForAssessmentPieChart: number[];

    readonly MIN_POINTS_GREEN = 100;
    readonly MIN_POINTS_ORANGE = 50;

    constructor(private courseService: CourseService, private resultService: ResultService, private route: ActivatedRoute, private jhiAlertService: AlertService) {}

    ngOnInit(): void {
        this.isLoading = true;
        this.loadCourse(Number(this.route.snapshot.paramMap.get('courseId')));
    }

    private loadCourse(courseId: number) {
        // Load the course.
        const loadCourseObservable = this.courseService.findWithExercisesAndParticipations(courseId).pipe(
            map((res: HttpResponse<Course>) => res.body!),
            tap((course: Course) => (this.course = course)),
            catchError((response: HttpErrorResponse) => {
                this.onError(response.message);
                return of(null);
            }),
        );

        // Load course stats.
        const loadStatsObservable = this.courseService.getStatsForInstructors(courseId).pipe(
            map((res: HttpResponse<StatsForDashboard>) => Object.assign({}, this.stats, res.body)),
            tap(stats => {
                this.stats = stats;
                this.dataForAssessmentPieChart = [this.stats.numberOfSubmissions - this.stats.numberOfAssessments, this.stats.numberOfAssessments];
            }),
            catchError((response: string) => {
                this.onError(response);
                return of(null);
            }),
        );

        // After both calls are done, the loading flag is removed.
        zip(loadCourseObservable, loadStatsObservable).subscribe(() => {
            this.isLoading = false;
        });
    }

    calculatePercentage(numerator: number, denominator: number): number {
        if (denominator === 0) {
            return 0;
        }

        return Math.round((numerator / denominator) * 100);
    }

    calculateClass(numberOfAssessments: number, length: number): string {
        const percentage = this.calculatePercentage(numberOfAssessments, length);

        if (percentage < this.MIN_POINTS_ORANGE) {
            return 'bg-danger';
        } else if (percentage < this.MIN_POINTS_GREEN) {
            return 'bg-warning';
        }

        return 'bg-success';
    }

    callback() {}

    private onError(error: string) {
        this.jhiAlertService.error(error, null, undefined);
    }
}
