import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { tap } from 'rxjs/operators';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiLanguageService } from 'ng-jhipster';
import { SessionStorageService } from 'ngx-webstorage';

import { User } from 'app/core/user/user.model';
import { JhiLanguageHelper } from 'app/core/language/language.helper';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';

import { SERVER_API_URL, VERSION } from 'app/app.constants';
import * as moment from 'moment';
import { ParticipationWebsocketService } from 'app/overview/participation-websocket.service';
import { AccountService } from 'app/core/auth/account.service';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { LoginService } from 'app/core/login/login.service';
import { Router, NavigationEnd } from '@angular/router';
import { ExamParticipationService } from 'app/exam/participate/exam-participation.service';
import { Exam } from 'app/entities/exam.model';
import { ArtemisServerDateService } from 'app/shared/server-date.service';

@Component({
    selector: 'jhi-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['navbar.scss'],
})
export class NavbarComponent implements OnInit, OnDestroy {
    readonly SERVER_API_URL = SERVER_API_URL;

    inProduction: boolean;
    isNavbarCollapsed: boolean;
    isTourAvailable: boolean;
    languages: string[];
    modalRef: NgbModalRef;
    version: string;
    currAccount: User | null;

    private authStateSubscription: Subscription;
    private routerEventSubscription: Subscription;
    public exam: Exam | undefined;

    constructor(
        private loginService: LoginService,
        private languageService: JhiLanguageService,
        private languageHelper: JhiLanguageHelper,
        private sessionStorage: SessionStorageService,
        private accountService: AccountService,
        private profileService: ProfileService,
        private participationWebsocketService: ParticipationWebsocketService,
        public guidedTourService: GuidedTourService,
        private router: Router,
        private examParticipationService: ExamParticipationService,
        private serverDateService: ArtemisServerDateService,
    ) {
        this.version = VERSION ? VERSION : '';
        this.isNavbarCollapsed = true;
    }

    ngOnInit() {
        this.languages = this.languageHelper.getAll();

        this.profileService.getProfileInfo().subscribe((profileInfo) => {
            if (profileInfo) {
                this.inProduction = profileInfo.inProduction;
            }
        });

        this.subscribeForGuidedTourAvailability();

        // The current user is needed to hide menu items for not logged in users.
        this.authStateSubscription = this.accountService
            .getAuthenticationState()
            .pipe(tap((user: User) => (this.currAccount = user)))
            .subscribe();

        this.routerEventSubscription = this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.exam = undefined;
                const examRoute = new RegExp('exams/[0-9]');
                if (examRoute.test(event.url) && !event.url.includes('management')) {
                    const routeParams = event.url.split('/');
                    const courseId = +routeParams[2];
                    const examId = +routeParams[4];

                    if (examId !== undefined && courseId !== undefined) {
                        this.examParticipationService.loadExam(courseId, examId).subscribe((loadedExam) => (this.exam = loadedExam));
                    }
                }
            }
        });
    }

    ngOnDestroy(): void {
        if (this.authStateSubscription) {
            this.authStateSubscription.unsubscribe();
        }
        if (this.routerEventSubscription) {
            this.routerEventSubscription.unsubscribe();
        }
    }

    /**
     * Check if a guided tour is available for the current route to display the start tour button in the account menu
     */
    subscribeForGuidedTourAvailability(): void {
        // Check availability after first subscribe call since the router event been triggered already
        this.guidedTourService.getGuidedTourAvailabilityStream().subscribe((isAvailable) => {
            this.isTourAvailable = isAvailable;
        });
    }

    changeLanguage(languageKey: string) {
        this.sessionStorage.store('locale', languageKey);
        this.languageService.changeLanguage(languageKey);
        moment.locale(languageKey);
    }

    collapseNavbar() {
        this.isNavbarCollapsed = true;
    }

    isAuthenticated() {
        return this.accountService.isAuthenticated();
    }

    logout() {
        this.participationWebsocketService.resetLocalCache();
        this.collapseNavbar();
        this.loginService.logout();
    }

    toggleNavbar() {
        this.isNavbarCollapsed = !this.isNavbarCollapsed;
    }

    getImageUrl(): string | null {
        return this.accountService.getImageUrl();
    }

    /**
     * Determine the label for initiating the guided tour based on the last seen tour step
     */
    guidedTourInitLabel(): string {
        switch (this.guidedTourService.getLastSeenTourStepForInit()) {
            case -1: {
                return 'global.menu.restartTutorial';
            }
            case 0: {
                return 'global.menu.startTutorial';
            }
            default: {
                return 'global.menu.continueTutorial';
            }
        }
    }

    /**
     * check if exam already started
     */
    examHasStarted(): boolean {
        return this.exam?.startDate ? this.exam.startDate.isBefore(this.serverDateService.now()) : false;
    }

    /**
     * check if exam is over
     */
    examIsOver(): boolean {
        return this.exam?.endDate ? this.exam.endDate.isBefore(this.serverDateService.now()) : true;
    }

    /**
     * check if exam mode is active
     */
    examModeActive(): boolean {
        return this.examHasStarted() && !this.examIsOver();
    }
}
