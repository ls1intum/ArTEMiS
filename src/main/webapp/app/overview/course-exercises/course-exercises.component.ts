import { Component, OnInit } from '@angular/core';
import { Course, CourseScoreCalculationService, CourseService } from 'app/entities/course';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { HttpResponse } from '@angular/common/http';
import * as moment from 'moment';
import { Exercise } from 'app/entities/exercise';

@Component({
    selector: 'jhi-course-exercises',
    templateUrl: './course-exercises.component.html',
    styleUrls: ['../course-overview.scss']
})
export class CourseExercisesComponent implements OnInit {
    public readonly DUE_DATE_ASC = 1;
    public readonly DUE_DATE_DESC = -1;
    private courseId: number;
    private subscription: Subscription;
    public course: Course;
    public weeklyIndexKeys: string[];
    public weeklyExercisesGrouped: object;

    constructor(
        private courseService: CourseService,
        private courseCalculationService: CourseScoreCalculationService,
        private courseServer: CourseService,
        private route: ActivatedRoute) {
    }

    ngOnInit() {
        this.subscription = this.route.parent.params.subscribe(params => {
            this.courseId = parseInt(params['courseId'], 10);
        });

        this.course = this.courseCalculationService.getCourse(this.courseId);
        if (this.course === undefined) {
            this.courseService.findAll().subscribe((res: HttpResponse<Course[]>) => {
                this.courseCalculationService.setCourses(res.body);
                this.course = this.courseCalculationService.getCourse(this.courseId);
            });
        }
        this.groupExercises(this.DUE_DATE_DESC);
    }

    public groupExercises(selectedOrder: number): void {
        this.weeklyExercisesGrouped = {};
        this.weeklyIndexKeys = [];
        const groupedExercises = {};
        const indexKeys: string[] = [];
        const courseExercises = [...this.course.exercises];
        const sortedExercises = this.sortExercises(courseExercises, selectedOrder);
        sortedExercises.forEach(exercise => {
            const dateValue = exercise.dueDate ? exercise.dueDate : exercise.releaseDate;
            const dateIndex = moment(dateValue).startOf('week').format('YYYY-MM-DD');
            if (!groupedExercises[dateIndex]) {
                indexKeys.push(dateIndex);
                groupedExercises[dateIndex] = {
                    label: `<b>${moment(dateValue).startOf('week').format('DD/MM/YYYY')}</b> - <b>${moment(dateValue).endOf('week').format('DD/MM/YYYY')}</b>`,
                    isCollapsed: dateValue.isBefore(moment(), 'week'),
                    isCurrentWeek: dateValue.isSame(moment(), 'week'),
                    exercises: []
                };
            }
            groupedExercises[dateIndex].exercises.push(exercise);
        });
        this.weeklyExercisesGrouped = groupedExercises;
        this.weeklyIndexKeys = indexKeys;
    }

    private sortExercises(exercises: Exercise[], selectedOrder: number) {
        return exercises.sort((a, b) => {
            const aValue = a.dueDate ? a.dueDate.valueOf() : a.releaseDate.valueOf();
            const bValue = b.dueDate ? b.dueDate.valueOf() : a.releaseDate.valueOf();

            return selectedOrder * (aValue - bValue);
        });
    }

}
