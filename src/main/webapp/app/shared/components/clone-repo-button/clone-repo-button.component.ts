import { Component, Input, OnInit } from '@angular/core';
import { FeatureToggle } from 'app/shared/feature-toggle/feature-toggle.service';
import { SourceTreeService } from 'app/exercises/programming/shared/service/sourceTree.service';
import { TranslateService } from '@ngx-translate/core';
import { ProfileInfo } from 'app/shared/layouts/profiles/profile-info.model';
import { AccountService } from 'app/core/auth/account.service';
import { User } from 'app/core/user/user.model';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { ProgrammingExerciseStudentParticipation } from 'app/entities/participation/programming-exercise-student-participation.model';

@Component({
    selector: 'jhi-clone-repo-button',
    templateUrl: './clone-repo-button.component.html',
    styles: [],
})
export class CloneRepoButtonComponent implements OnInit {
    @Input()
    loading = false;

    @Input()
    smallButtons: boolean;

    @Input()
    repositoryUrl: string;

    useSsh = false;
    sshKeysUrl: string;
    sshEnabled: boolean;
    sshTemplateUrl: string;
    repositoryPassword: string;
    versionControlUrl: string;
    wasCopied = false;
    FeatureToggle = FeatureToggle;
    user: User;

    constructor(
        private translateService: TranslateService,
        private sourceTreeService: SourceTreeService,
        private accountService: AccountService,
        private profileService: ProfileService,
    ) {}

    ngOnInit() {
        this.accountService.identity().then((user) => {
            this.user = user!;

            // Only load password if current user login starts with 'edx_' or 'u4i_'
            if (user && user.login && (user.login.startsWith('edx_') || user.login.startsWith('u4i_'))) {
                this.getRepositoryPassword();
            }
        });

        // Get ssh information from the user
        this.profileService.getProfileInfo().subscribe((info: ProfileInfo) => {
            this.sshKeysUrl = info.sshKeysURL;
            this.sshTemplateUrl = info.sshCloneURLTemplate;
            this.sshEnabled = !!this.sshTemplateUrl;
            if (info.versionControlUrl) {
                this.versionControlUrl = info.versionControlUrl;
            }
        });
    }

    /**
     * get the repositoryPassword
     */
    getRepositoryPassword() {
        this.sourceTreeService.getRepositoryPassword().subscribe((res) => {
            const password = res['password'];
            if (password) {
                this.repositoryPassword = password;
            }
        });
    }

    getHtmlOrSshRepositoryUrl() {
        return this.useSsh ? this.getSshCloneUrl(this.repositoryUrl) : this.repositoryUrl;
    }

    /**
     * The user info part of the repository url of a team participation has to be be added with the current user's login.
     *
     * @return repository url with username of current user inserted
     */
    private repositoryUrlForTeam(url?: ProgrammingExerciseStudentParticipation) {
        // (https://)(bitbucket.ase.in.tum.de/...-team1.git)  =>  (https://)ga12abc@(bitbucket.ase.in.tum.de/...-team1.git)
        return url?.repositoryUrl?.replace(/^(\w*:\/\/)(.*)$/, `$1${this.user.login}@$2`);
    }

    /**
     * Transforms the repository url to a ssh url
     */
    getSshCloneUrl(url?: string) {
        return url?.replace(/^\w*:\/\/[^/]*?\/(scm\/)?(.*)$/, this.sshTemplateUrl + '$2');
    }

    /**
     * Inserts the correct link to the translated ssh tip.
     */
    getSshKeyTip() {
        return this.translateService.instant('artemisApp.exerciseActions.sshKeyTip').replace(/{link:(.*)}/, '<a href="' + this.sshKeysUrl + '" target="_blank">$1</a>');
    }

    /**
     * set wasCopied for 3 seconds on success
     */
    onCopySuccess() {
        this.wasCopied = true;
        setTimeout(() => {
            this.wasCopied = false;
        }, 3000);
    }

    /**
     * console log if copy fails
     */
    onCopyFailure() {}

    /**
     * build the sourceTreeUrl from the repository url
     * @return sourceTreeUrl
     */
    buildSourceTreeUrl() {
        return this.sourceTreeService.buildSourceTreeUrl(this.versionControlUrl, this.repositoryUrl);
    }
}
