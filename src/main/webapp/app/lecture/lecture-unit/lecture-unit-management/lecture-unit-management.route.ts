import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { LectureUnitManagementComponent } from 'app/lecture/lecture-unit/lecture-unit-management/lecture-unit-management.component';
import { CreateExerciseUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/create-exercise-unit/create-exercise-unit.component';

export const lectureUnitRoute: Routes = [
    {
        path: ':courseId/lectures/:lectureId/unit-management',
        component: LectureUnitManagementComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lectureUnit.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/exercise-units/create',
        component: CreateExerciseUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lectureUnit.createExerciseUnit.title',
        },
    },
];
