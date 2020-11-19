import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { LectureUnitManagementComponent } from 'app/lecture/lecture-unit/lecture-unit-management/lecture-unit-management.component';
import { CreateExerciseUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/create-exercise-unit/create-exercise-unit.component';
import { CreateAttachmentUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/create-attachment-unit/create-attachment-unit.component';
import { EditAttachmentUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/edit-attachment-unit/edit-attachment-unit.component';
import { CreateTextUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/create-text-unit/create-text-unit.component';
import { EditTextUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/edit-text-unit/edit-text-unit.component';
import { CreateVideoUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/create-video-unit/create-video-unit.component';
import { EditVideoUnitComponent } from 'app/lecture/lecture-unit/lecture-unit-management/edit-video-unit/edit-video-unit.component';

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
            pageTitle: 'artemisApp.exerciseUnit.createExerciseUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/attachment-units/create',
        component: CreateAttachmentUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.attachmentUnit.createAttachmentUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/attachment-units/:attachmentUnitId/edit',
        component: EditAttachmentUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.attachmentUnit.editAttachmentUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/video-units/create',
        component: CreateVideoUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.videoUnit.createVideoUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/video-units/:videoUnitId/edit',
        component: EditVideoUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.videoUnit.editVideoUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/text-units/create',
        component: CreateTextUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.textUnit.createTextUnit.title',
        },
    },
    {
        path: ':courseId/lectures/:lectureId/unit-management/text-units/:textUnitId/edit',
        component: EditTextUnitComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.textUnit.editTextUnit.title',
        },
    },
];
