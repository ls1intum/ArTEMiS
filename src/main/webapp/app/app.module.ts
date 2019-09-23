import './vendor.ts';

import { ErrorHandler, NgModule, Sanitizer } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import { NgxWebstorageModule } from 'ngx-webstorage';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {
    JhiAlertService,
    JhiConfigService,
    JhiLanguageService,
    JhiModuleConfig,
    JhiPaginationUtil,
    JhiResolvePagingParams,
    missingTranslationHandler,
    NgJhipsterModule,
    translatePartialLoader,
} from 'ng-jhipster';
import { Angulartics2Module } from 'angulartics2';
import * as moment from 'moment';

import { AuthInterceptor } from './blocks/interceptor/auth.interceptor';
import { AuthExpiredInterceptor } from './blocks/interceptor/auth-expired.interceptor';
import { ErrorHandlerInterceptor } from './blocks/interceptor/errorhandler.interceptor';
import { NotificationInterceptor } from './blocks/interceptor/notification.interceptor';
import { JhiWebsocketService, UserRouteAccessService } from './core';
import { ArtemisSharedModule } from './shared';
import { ArtemisCoreModule } from 'app/core';
import { ArtemisAppRoutingModule } from './app-routing.module';
import { ArtemisHomeModule } from './home';
import { ArtemisLegalModule } from './legal';
import { ArtemisOverviewModule } from './overview';
import { ArtemisAccountModule } from './account/account.module';
import { ArtemisCourseListModule } from './course-list';
import { ArtemisEntityModule } from './entities/entity.module';
import { ArtemisCourseScoresModule, ArtemisExerciseScoresModule } from './scores';
import { PaginationConfig } from './blocks/config/uib-pagination.config';
import { DifferencePipe, MomentModule } from 'ngx-moment';
import { RepositoryInterceptor, RepositoryService } from './entities/repository';
import { ArtemisQuizModule } from './quiz/participate';
import { ArtemisTextModule } from './text-editor';
import { ArtemisTextAssessmentModule } from './text-assessment';
import { ArtemisModelingStatisticsModule } from './modeling-statistics/';
import {
    ActiveMenuDirective,
    ErrorComponent,
    FooterComponent,
    JhiMainComponent,
    NavbarComponent,
    NotificationContainerComponent,
    PageRibbonComponent,
    ProfileService,
    SystemNotificationComponent,
} from './layouts';
import { ArtemisApollonDiagramsModule } from './apollon-diagrams';
import { ArtemisStatisticModule } from './quiz/statistics/quiz-statistic.module';
import { ArtemisModelingSubmissionModule } from 'app/modeling-submission';
import { QuizExerciseExportComponent } from './entities/quiz-exercise/quiz-exercise-export.component';
import { PendingChangesGuard } from 'app/shared';
import { ArtemisInstructorCourseStatsDashboardModule } from 'app/instructor-course-dashboard';
import { ArtemisInstructorExerciseStatsDashboardModule } from 'app/instructor-exercise-dashboard';
import { ParticipationDataProvider } from 'app/course-list';
import { ArtemisTutorCourseDashboardModule } from 'app/tutor-course-dashboard';
import { ArtemisTutorExerciseDashboardModule } from 'app/tutor-exercise-dashboard';
import { ArtemisExampleTextSubmissionModule } from 'app/example-text-submission';
import { ArtemisExampleModelingSubmissionModule } from 'app/example-modeling-submission';
import { ArtemisComplaintsModule } from 'app/complaints';
import { ArtemisNotificationModule } from 'app/entities/notification/notification.module';
import { ArtemisSystemNotificationModule } from 'app/entities/system-notification/system-notification.module';
import { ArtemisModelingAssessmentEditorModule } from 'app/modeling-assessment-editor/modeling-assessment-editor.module';
import { ArtemisExampleModelingSolutionModule } from 'app/example-modeling-solution';
import { ArtemisHeaderExercisePageWithDetailsModule } from 'app/exercise-headers';
import { ArtemisComplaintsForTutorModule } from 'app/complaints-for-tutor';
import { SentryErrorHandler } from 'app/sentry/sentry.error-handler';
import { ArtemisConnectionNotificationModule } from './layouts/connection-notification/connection-notification.module';
import { ArtemisListOfComplaintsModule } from 'app/list-of-complaints';
import { DeviceDetectorModule } from 'ngx-device-detector';
import { GuidedTourModule } from 'app/guided-tour/guided-tour.module';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { ArtemisProgrammingSubmissionModule } from 'app/programming-submission/programming-submission.module';
import { ArtemisParticipationModule } from 'app/entities/participation/participation.module';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArtemisFileUploadSubmissionModule } from 'app/file-upload-submission/file-upload-submission.module';

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        ArtemisAppRoutingModule,
        NgxWebstorageModule.forRoot({ prefix: 'jhi', separator: '-' }),
        DeviceDetectorModule,
        GuidedTourModule.forRoot(),
        /**
         * @external Moment is a date library for parsing, validating, manipulating, and formatting dates.
         */
        MomentModule,
        NgJhipsterModule.forRoot({
            // set below to true to make alerts look like toast
            alertAsToast: false,
            alertTimeout: 8000,
            i18nEnabled: true,
            defaultI18nLang: 'en',
        }),
        /**
         * @external Angulartics offers Vendor-agnostic analytics and integration with Matomo
         */
        Angulartics2Module.forRoot(),
        ArtemisSharedModule.forRoot(),
        ArtemisCoreModule,
        ArtemisHomeModule,
        ArtemisLegalModule,
        ArtemisParticipationModule.forRoot(),
        ArtemisProgrammingSubmissionModule.forRoot(),
        ArtemisOverviewModule,
        ArtemisAccountModule,
        ArtemisEntityModule,
        ArtemisApollonDiagramsModule,
        ArtemisCourseListModule,
        ArtemisQuizModule,
        ArtemisCourseScoresModule,
        ArtemisExerciseScoresModule,
        ArtemisStatisticModule,
        ArtemisModelingSubmissionModule,
        ArtemisModelingStatisticsModule,
        ArtemisTextModule,
        ArtemisTextAssessmentModule,
        ArtemisFileUploadSubmissionModule,
        ArtemisInstructorCourseStatsDashboardModule,
        ArtemisInstructorExerciseStatsDashboardModule,
        ArtemisTutorCourseDashboardModule,
        ArtemisTutorExerciseDashboardModule,
        ArtemisComplaintsModule,
        ArtemisComplaintsForTutorModule,
        ArtemisNotificationModule,
        ArtemisSystemNotificationModule,
        ArtemisModelingAssessmentEditorModule,
        ArtemisModelingSubmissionModule,
        ArtemisExampleTextSubmissionModule,
        ArtemisExampleModelingSubmissionModule,
        ArtemisExampleModelingSolutionModule,
        ArtemisHeaderExercisePageWithDetailsModule,
        ArtemisConnectionNotificationModule,
        ArtemisListOfComplaintsModule,
        // jhipster-needle-angular-add-module JHipster will add new module here
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: translatePartialLoader,
                deps: [HttpClient],
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useFactory: missingTranslationHandler,
                deps: [JhiConfigService],
            },
        }),
    ],
    declarations: [
        JhiMainComponent,
        NavbarComponent,
        ErrorComponent,
        PageRibbonComponent,
        ActiveMenuDirective,
        FooterComponent,
        SystemNotificationComponent,
        NotificationContainerComponent,
        QuizExerciseExportComponent,
    ],
    providers: [
        {
            provide: ErrorHandler,
            useClass: SentryErrorHandler,
        },
        GuidedTourService,
        ProfileService,
        RepositoryService,
        PaginationConfig,
        UserRouteAccessService,
        DifferencePipe,
        JhiWebsocketService,
        ParticipationDataProvider,
        PendingChangesGuard,
        TranslateService,
        /**
         * @description Interceptor declarations:
         * Interceptors are located at 'blocks/interceptor/.
         * All of them implement the HttpInterceptor interface.
         * They can be used to modify API calls or trigger additional function calls.
         * Most interceptors will transform the outgoing request before passing it to
         * the next interceptor in the chain, by calling next.handle(transformedReq).
         * Documentation: https://angular.io/api/common/http/HttpInterceptor
         */
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthExpiredInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: ErrorHandlerInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: NotificationInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: RepositoryInterceptor,
            multi: true,
        },
        {
            provide: JhiLanguageService,
            useClass: JhiLanguageService,
            deps: [TranslateService, JhiConfigService],
        },
        {
            provide: JhiResolvePagingParams,
            useClass: JhiResolvePagingParams,
            deps: [JhiPaginationUtil],
        },
        {
            provide: JhiAlertService,
            useClass: JhiAlertService,
            deps: [Sanitizer, JhiConfigService, TranslateService],
        },
        {
            provide: JhiConfigService,
            useClass: JhiConfigService,
            deps: [JhiModuleConfig],
        },
    ],
    bootstrap: [JhiMainComponent],
})
export class ArTeMiSAppModule {
    constructor(private dpConfig: NgbDatepickerConfig) {
        this.dpConfig.minDate = { year: moment().year() - 100, month: 1, day: 1 };
    }
}
