import { Component, OnInit } from '@angular/core';

import { AccountService, User } from '../../core';
import { PasswordService } from './password.service';

@Component({
    selector: 'jhi-password',
    templateUrl: './password.component.html',
})
export class PasswordComponent implements OnInit {
    doNotMatch: string | null;
    error: string | null;
    success: string | null;
    user: User;
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;

    constructor(private passwordService: PasswordService, private accountService: AccountService) {}

    ngOnInit() {
        this.accountService.identity().then(user => {
            this.user = user!;
        });
    }

    changePassword() {
        if (this.newPassword !== this.confirmPassword) {
            this.error = null;
            this.success = null;
            this.doNotMatch = 'ERROR';
        } else {
            this.doNotMatch = null;
            this.passwordService.save(this.newPassword, this.currentPassword).subscribe(
                () => {
                    this.error = null;
                    this.success = 'OK';
                },
                () => {
                    this.success = null;
                    this.error = 'ERROR';
                },
            );
        }
    }
}
