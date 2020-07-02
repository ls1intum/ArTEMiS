import { Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs/Subscription';
import { JhiEventManager } from 'ng-jhipster';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { AlertService } from 'app/core/alert/alert.service';
import { User } from 'app/core/user/user.model';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { UserService } from 'app/core/user/user.service';
import { DataTableComponent } from 'app/shared/data-table/data-table.component';
import { iconsAsHTML } from 'app/utils/icons.utils';
import { Exam } from 'app/entities/exam.model';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { ButtonSize, ButtonType } from 'app/shared/components/button.component';

const cssClasses = {
    alreadyRegistered: 'already-registered',
    newlyRegistered: 'newly-registered',
};

@Component({
    selector: 'jhi-exam-students',
    templateUrl: './exam-students.component.html',
    styleUrls: ['./exam-students.component.scss'],
    encapsulation: ViewEncapsulation.None,
})
export class ExamStudentsComponent implements OnInit, OnDestroy {
    @ViewChild(DataTableComponent) dataTable: DataTableComponent;

    readonly ButtonType = ButtonType;
    readonly ButtonSize = ButtonSize;
    readonly ActionType = ActionType;

    courseId: number;
    exam: Exam;
    allRegisteredUsers: User[] = [];
    filteredUsersSize = 0;
    paramSub: Subscription;

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    isLoading = false;
    isSearching = false;
    searchFailed = false;
    searchNoResults = false;
    isTransitioning = false;
    rowClass: string | undefined = undefined;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private jhiAlertService: AlertService,
        private eventManager: JhiEventManager,
        private examManagementService: ExamManagementService,
        private userService: UserService,
    ) {}

    ngOnInit() {
        this.isLoading = true;
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        this.route.data.subscribe(({ exam }: { exam: Exam }) => {
            this.exam = exam;
            this.allRegisteredUsers = exam.registeredUsers! || [];
            this.isLoading = false;
        });
    }

    reloadExamWithRegisterdUsers() {
        this.isLoading = true;
        this.examManagementService.find(this.courseId, this.exam.id, true).subscribe((examResponse: HttpResponse<Exam>) => {
            this.exam = examResponse.body!;
            this.allRegisteredUsers = this.exam.registeredUsers! || [];
            this.isLoading = false;
        });
    }

    ngOnDestroy() {
        this.dialogErrorSource.unsubscribe();
    }

    /**
     * Receives the search text and filter results from DataTableComponent, modifies them and returns the result which will be used by ngbTypeahead.
     *
     * @param stream$ stream of searches of the format {text, entities} where entities are the results
     * @return stream of users for the autocomplete
     */
    searchAllUsers = (stream$: Observable<{ text: string; entities: User[] }>): Observable<User[]> => {
        return stream$.pipe(
            switchMap(({ text: loginOrName }) => {
                this.searchFailed = false;
                this.searchNoResults = false;
                if (loginOrName.length < 3) {
                    return of([]);
                }
                this.isSearching = true;
                return this.userService
                    .search(loginOrName)
                    .pipe(map((usersResponse) => usersResponse.body!))
                    .pipe(
                        tap((users) => {
                            if (users.length === 0) {
                                this.searchNoResults = true;
                            }
                        }),
                        catchError(() => {
                            this.searchFailed = true;
                            return of([]);
                        }),
                    );
            }),
            tap(() => {
                this.isSearching = false;
            }),
            tap((users) => {
                setTimeout(() => {
                    for (let i = 0; i < this.dataTable.typeaheadButtons.length; i++) {
                        const isAlreadyInCourseGroup = this.allRegisteredUsers.map((user) => user.id).includes(users[i].id);
                        this.dataTable.typeaheadButtons[i].insertAdjacentHTML('beforeend', iconsAsHTML[isAlreadyInCourseGroup ? 'users' : 'users-plus']);
                        if (isAlreadyInCourseGroup) {
                            this.dataTable.typeaheadButtons[i].classList.add(cssClasses.alreadyRegistered);
                        }
                    }
                });
            }),
        );
    };

    /**
     * Receives the user that was selected in the autocomplete and the callback from DataTableComponent.
     * The callback inserts the search term of the selected entity into the search field and updates the displayed users.
     *
     * @param user The selected user from the autocomplete suggestions
     * @param callback Function that can be called with the selected user to trigger the DataTableComponent default behavior
     */
    onAutocompleteSelect = (user: User, callback: (user: User) => void): void => {
        // If the user is not registered for this exam yet, perform the server call to add them
        if (!this.allRegisteredUsers.map((u) => u.id).includes(user.id) && user.login) {
            this.isTransitioning = true;
            this.examManagementService.addStudentToExam(this.courseId, this.exam.id, user.login).subscribe(
                () => {
                    this.isTransitioning = false;

                    // Add newly registered user to the list of all registered users for the exam
                    this.allRegisteredUsers.push(user);

                    // Hand back over to the data table for updating
                    callback(user);

                    // Flash green background color to signal to the user that this student was registered
                    this.flashRowClass(cssClasses.newlyRegistered);
                },
                () => {
                    this.isTransitioning = false;
                },
            );
        } else {
            // Hand back over to the data table
            callback(user);
        }
    };

    /**
     * Unregister student from exam
     *
     * @param user User that should be removed from the exam
     * @param event generated by the jhiDeleteButton. Has the property deleteParticipationsAndSubmission, reflecting the checkbox choice of the user
     */
    removeFromExam(user: User, $event: { [key: string]: boolean }) {
        this.examManagementService.removeStudentFromExam(this.courseId, this.exam.id, user.login!, $event.deleteParticipationsAndSubmission).subscribe(
            () => {
                this.allRegisteredUsers = this.allRegisteredUsers.filter((u) => u.login !== user.login);
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
    }

    /**
     * Update the number of filtered users
     *
     * @param filteredUsersSize Total number of users after filters have been applied
     */
    handleUsersSizeChange = (filteredUsersSize: number) => {
        this.filteredUsersSize = filteredUsersSize;
    };

    /**
     * Formats the results in the autocomplete overlay.
     *
     * @param user
     */
    searchResultFormatter = (user: User) => {
        const { name, login } = user;
        return `${name} (${login})`;
    };

    /**
     * Converts a user object to a string that can be searched for. This is
     * used by the autocomplete select inside the data table.
     *
     * @param user User
     */
    searchTextFromUser = (user: User): string => {
        return user.login || '';
    };

    /**
     * Computes the row class that is being added to all rows of the datatable
     */
    dataTableRowClass = () => {
        return this.rowClass;
    };

    /**
     * Can be used to highlight rows temporarily by flashing a certain css class
     *
     * @param className Name of the class to be applied to all rows
     */
    flashRowClass = (className: string) => {
        this.rowClass = className;
        setTimeout(() => (this.rowClass = undefined));
    };
}
