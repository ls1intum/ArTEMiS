import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Course } from 'app/entities/course/course.model';
import { CourseService } from './course.service';
import { CourseComponent } from './course.component';
import { CourseDetailComponent } from './course-detail.component';
import { CourseUpdateComponent } from './course-update.component';
import { CourseDeletePopupComponent } from './course-delete-dialog.component';
import { CourseScoreCalculationComponent } from './course-score-calculation.component';

@Injectable({ providedIn: 'root' })
export class CourseResolve implements Resolve<Course> {
    constructor(private service: CourseService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((course: HttpResponse<Course>) => course.body));
        }
        return of(new Course());
    }
}

export const courseRoute: Routes = [
    {
        path: 'course',
        component: CourseComponent,
        data: {
            authorities: ['ROLE_TA', 'ROLE_INSTRUCTOR', 'ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'course/:id/view',
        component: CourseDetailComponent,
        resolve: {
            course: CourseResolve
        },
        data: {
            authorities: ['ROLE_INSTRUCTOR', 'ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'course/new',
        component: CourseUpdateComponent,
        resolve: {
            course: CourseResolve
        },
        data: {
            authorities: ['ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'course/:id/edit',
        component: CourseUpdateComponent,
        resolve: {
            course: CourseResolve
        },
        data: {
            authorities: ['ROLE_INSTRUCTOR', 'ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'course/:id/score-calculation',
        component: CourseScoreCalculationComponent,
        data: {
            authorities: ['ROLE_USER', 'ROLE_TA', 'ROLE_INSTRUCTOR', 'ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const coursePopupRoute: Routes = [
    {
        path: 'course/:id/delete',
        component: CourseDeletePopupComponent,
        resolve: {
            course: CourseResolve
        },
        data: {
            authorities: ['ROLE_ADMIN'],
            pageTitle: 'arTeMiSApp.course.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
