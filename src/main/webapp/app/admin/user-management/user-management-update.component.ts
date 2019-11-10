import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { JhiLanguageHelper, User, UserService } from 'app/core';

@Component({
    selector: 'jhi-user-management-update',
    templateUrl: './user-management-update.component.html',
})
export class UserManagementUpdateComponent implements OnInit {
    user: User;
    languages: any[];
    authorities: any[];
    isSaving: boolean;

    constructor(private languageHelper: JhiLanguageHelper, private userService: UserService, private route: ActivatedRoute) {}

    /**
     * Enable subscriptions to retrieve the user based on the activated route, all authorities and all languages on init
     */
    ngOnInit() {
        this.isSaving = false;
        this.route.data.subscribe(({ user }) => {
            this.user = user.body ? user.body : user;
        });
        this.authorities = [];
        this.userService.authorities().subscribe(authorities => {
            this.authorities = authorities;
        });
        this.languageHelper.getAll().then(languages => {
            this.languages = languages;
        });
    }

    /**
     * Navigate to the previous page when the user cancels the update process
     */
    previousState() {
        window.history.back();
    }

    /**
     * Update or create user in the user management component
     */
    save() {
        this.isSaving = true;
        if (this.user.id !== null) {
            this.userService.update(this.user).subscribe(
                response => this.onSaveSuccess(response.body!),
                () => this.onSaveError(),
            );
        } else {
            this.userService.create(this.user).subscribe(
                response => this.onSaveSuccess(response.body!),
                () => this.onSaveError(),
            );
        }
    }

    /**
     * Set isSaving to false and navigate to previous page
     * @param result
     */
    private onSaveSuccess(result: User) {
        this.isSaving = false;
        this.previousState();
    }

    /**
     * Set isSaving to false
     */
    private onSaveError() {
        this.isSaving = false;
    }
}
