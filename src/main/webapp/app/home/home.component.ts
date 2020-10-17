import { AfterViewInit, Component, ElementRef, OnInit, Renderer2 } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { Router } from '@angular/router';
import { User } from 'app/core/user/user.model';
import { Credentials } from 'app/core/auth/auth-jwt.service';
import { HttpErrorResponse } from '@angular/common/http';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { OrionConnectorService } from 'app/shared/orion/orion-connector.service';
import { isOrion } from 'app/shared/orion/orion';
import { ModalConfirmAutofocusComponent } from 'app/shared/orion/modal-confirm-autofocus/modal-confirm-autofocus.component';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/core/login/login.service';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';

@Component({
    selector: 'jhi-home',
    templateUrl: './home.component.html',
    styleUrls: ['home.scss'],
})
export class HomeComponent implements OnInit, AfterViewInit {
    authenticationError = false;
    authenticationAttempts = 0;
    account: User;
    modalRef: NgbModalRef;
    password: string;
    rememberMe = true;
    acceptTerms = true;
    username: string;
    captchaRequired = false;
    credentials: Credentials;
    isRegistrationEnabled = false;

    // if the server is not connected to an external user management such as JIRA, we accept all valid username patterns
    usernameRegexPattern = /^[a-z0-9_-]{3,50}$/; // default, might be overridden in ngOnInit
    errorMessageUsername = 'home.errors.usernameIncorrect'; // default, might be overridden in ngOnInit
    accountName?: string; // additional information in the welcome message

    externalUserManagementActive = true;
    externalUserManagementUrl: string;
    externalUserManagementName: string;

    isSubmittingLogin = false;

    constructor(
        private router: Router,
        private accountService: AccountService,
        private loginService: LoginService,
        private stateStorageService: StateStorageService,
        private elementRef: ElementRef,
        private renderer: Renderer2,
        private eventManager: JhiEventManager,
        private guidedTourService: GuidedTourService,
        private javaBridge: OrionConnectorService,
        private modalService: NgbModal,
        private profileService: ProfileService,
    ) {}

    ngOnInit() {
        this.profileService.getProfileInfo().subscribe((profileInfo) => {
            if (profileInfo) {
                if (profileInfo.activeProfiles.includes('jira')) {
                    this.externalUserManagementUrl = profileInfo.externalUserManagementURL;
                    this.externalUserManagementName = profileInfo.externalUserManagementName;
                    if (profileInfo.allowedLdapUsernamePattern) {
                        this.usernameRegexPattern = new RegExp(profileInfo.allowedLdapUsernamePattern);
                    }
                } else {
                    // TODO: in the future we might also allow external user management for non jira profiles
                    this.externalUserManagementActive = false;
                }
                this.accountName = profileInfo.accountName;
                if (this.accountName === 'TUM') {
                    this.errorMessageUsername = 'home.errors.tumWarning';
                }
                this.isRegistrationEnabled = profileInfo.registrationEnabled || false;
            }
        });
        this.accountService.identity().then((user) => {
            this.currentUserCallback(user!);
        });
        this.registerAuthenticationSuccess();
    }

    registerAuthenticationSuccess() {
        this.eventManager.subscribe('authenticationSuccess', () => {
            this.accountService.identity().then((user) => {
                this.currentUserCallback(user!);
            });
        });
    }

    ngAfterViewInit() {
        this.renderer.selectRootElement('#username', true).focus();
    }

    login() {
        this.isSubmittingLogin = true;
        this.loginService
            .login({
                username: this.username,
                password: this.password,
                rememberMe: this.rememberMe,
            })
            .then(() => {
                this.authenticationError = false;
                this.authenticationAttempts = 0;
                this.captchaRequired = false;

                if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
                    this.router.navigate(['']);
                }

                this.eventManager.broadcast({
                    name: 'authenticationSuccess',
                    content: 'Sending Authentication Success',
                });

                // previousState was set in the authExpiredInterceptor before being redirected to login modal.
                // since login is successful, go to stored previousState and clear previousState
                const redirect = this.stateStorageService.getUrl();
                if (redirect) {
                    this.stateStorageService.storeUrl(null);
                    this.router.navigate([redirect]);
                }

                // Log in to Orion
                if (isOrion) {
                    const modalRef: NgbModalRef = this.modalService.open(ModalConfirmAutofocusComponent as Component, { size: 'lg', backdrop: 'static' });
                    modalRef.componentInstance.text = 'login.ide.confirmation';
                    modalRef.componentInstance.title = 'login.ide.title';
                    modalRef.result.then(
                        () => {
                            this.javaBridge.login(this.username, this.password);
                        },
                        () => {},
                    );
                }
            })
            .catch((error: HttpErrorResponse) => {
                // TODO: if registration is enabled, handle the case "User was not activated"
                this.captchaRequired = error.headers.get('X-artemisApp-error') === 'CAPTCHA required';
                this.authenticationError = true;
                this.authenticationAttempts++;
            })
            .finally(() => (this.isSubmittingLogin = false));
    }

    currentUserCallback(account: User) {
        this.account = account;
        if (account) {
            this.router.navigate(['courses']);
        }
    }

    isAuthenticated() {
        return this.accountService.isAuthenticated();
    }

    inputChange($event: any) {
        if ($event.target && $event.target.name === 'username') {
            this.username = $event.target.value;
        }
        if ($event.target && $event.target.name === 'password') {
            this.password = $event.target.value;
        }
    }
}
