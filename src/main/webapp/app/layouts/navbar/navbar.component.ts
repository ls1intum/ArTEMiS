import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { distinctUntilChanged, tap } from 'rxjs/operators';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiLanguageService } from 'ng-jhipster';
import { SessionStorageService } from 'ngx-webstorage';

import { ProfileService } from '../profiles/profile.service';
import { AccountService, JhiLanguageHelper, LoginService, User } from 'app/core';

import { VERSION } from 'app/app.constants';
import * as moment from 'moment';
import { ParticipationWebsocketService } from 'app/entities/participation';

@Component({
    selector: 'jhi-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['navbar.scss'],
})
export class NavbarComponent implements OnInit {
    inProduction: boolean;
    isNavbarCollapsed: boolean;
    languages: string[];
    modalRef: NgbModalRef;
    version: string;
    currAccount: User | null;

    constructor(
        private loginService: LoginService,
        private languageService: JhiLanguageService,
        private languageHelper: JhiLanguageHelper,
        private sessionStorage: SessionStorageService,
        private accountService: AccountService,
        private profileService: ProfileService,
        private participationWebsocketService: ParticipationWebsocketService,
        private router: Router,
    ) {
        this.version = VERSION ? VERSION : '';
        this.isNavbarCollapsed = true;
    }

    ngOnInit() {
        this.languageHelper.getAll().then(languages => {
            this.languages = languages;
        });

        this.profileService.getProfileInfo().subscribe(
            profileInfo => {
                if (profileInfo) {
                    this.inProduction = profileInfo.inProduction;
                }
            },
            reason => {},
        );

        this.accountService
            .getAuthenticationState()
            .pipe(
                distinctUntilChanged(),
                tap((user: User) => (this.currAccount = user)),
            )
            .subscribe();
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
}
