import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { Observable } from 'rxjs/Observable';
import { map } from 'rxjs/operators';

import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { ProgrammingExerciseService } from './services/programming-exercise.service';
import { CanDeactivateGuard } from 'app/shared/guard/can-deactivate.guard';
import { ProgrammingExerciseManageTestCasesComponent } from 'app/exercises/programming/manage/test-cases/programming-exercise-manage-test-cases.component';

@Injectable({ providedIn: 'root' })
export class ProgrammingExerciseResolve implements Resolve<ProgrammingExercise> {
    constructor(private service: ProgrammingExerciseService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((programmingExercise: HttpResponse<ProgrammingExercise>) => programmingExercise.body!));
        }
        return Observable.of(new ProgrammingExercise());
    }
}

export const programmingExerciseRoute: Routes = [
    {
        path: 'course/:courseId/programming-exercise/:exerciseId/manage-test-cases',
        component: ProgrammingExerciseManageTestCasesComponent,
        data: {
            authorities: ['ROLE_INSTRUCTOR', 'ROLE_ADMIN'],
            pageTitle: 'artemisApp.programmingExercise.home.title',
        },
        canActivate: [UserRouteAccessService],
        canDeactivate: [CanDeactivateGuard],
    },
];
