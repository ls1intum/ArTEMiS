import { ComponentFixture, TestBed, fakeAsync, tick, inject } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { ArtemisTestModule } from '../../test.module';
import { ArtemisAdminModule } from 'app/admin/admin.module';
import { UserManagementComponent } from 'app/admin/user-management/user-management.component';
import { UserService } from 'app/core/user/user.service';
import { AccountService } from 'app/core/auth/account.service';
import { MockAccountService } from '../../helpers/mocks/service/mock-account.service';
import { MockActivatedRoute, MockRouter } from '../../helpers/mocks/service/mock-route.service';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { User } from 'app/core/user/user.model';
import { of } from 'rxjs';

describe('UserManagementComponent', () => {
    let comp: UserManagementComponent;
    let fixture: ComponentFixture<UserManagementComponent>;
    let service: UserService;
    let mockActivatedRoute: MockActivatedRoute;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArtemisTestModule, ArtemisAdminModule],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: new MockActivatedRoute({ courseId: 123 }),
                },
                {
                    provide: Router,
                    useValue: MockRouter,
                },
                { provide: AccountService, useClass: MockAccountService },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(UserManagementComponent);
                comp = fixture.componentInstance;
                service = fixture.debugElement.injector.get(UserService);
                mockActivatedRoute = TestBed.inject(ActivatedRoute) as MockActivatedRoute;
                mockActivatedRoute.setParameters({
                    sort: 'id,desc',
                });
            });
    });

    // The admin module is lazy loaded - we therefore need a dummy test to load
    // the module and verify that there are no dependency related issues.
    it('should render a component from the admin module', () => {
        expect(comp).toBeDefined();
    });

    it('should parse the user search result into the correct component state', inject(
        [],
        fakeAsync(() => {
            const headers = new HttpHeaders().append('link', 'link;link').append('X-Total-Count', '1');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new User(1)],
                        headers,
                    }),
                ),
            );

            comp.loadAll();
            // 1 sec of pause, because of the debounce time
            tick(1000);

            expect(comp.users && comp.users[0].id).toEqual(1);
            expect(comp.totalItems).toEqual(1);
            expect(comp.loadingSearchResult).toEqual(false);
        }),
    ));

    describe('OnInit', () => {
        it('Should call load all on init', inject(
            [],
            fakeAsync(() => {
                // GIVEN
                const headers = new HttpHeaders().append('link', 'link;link');
                spyOn(service, 'query').and.returnValue(
                    of(
                        new HttpResponse({
                            body: [new User(123)],
                            headers,
                        }),
                    ),
                );

                // WHEN
                comp.ngOnInit();
                tick(1000); // simulate async

                // THEN
                expect(service.query).toHaveBeenCalled();
                expect(comp.users && comp.users[0]).toEqual(jasmine.objectContaining({ id: 123 }));
            }),
        ));
    });

    describe('setActive', () => {
        it('Should update user and call load all', inject(
            [],
            fakeAsync(() => {
                // GIVEN
                const headers = new HttpHeaders().append('link', 'link;link');
                const user = new User(123);
                spyOn(service, 'query').and.returnValue(
                    of(
                        new HttpResponse({
                            body: [user],
                            headers,
                        }),
                    ),
                );
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ status: 200 })));

                // WHEN
                comp.setActive(user, true);
                tick(1000); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith({ ...user, activated: true });
                expect(service.query).toHaveBeenCalled();
                expect(comp.users && comp.users[0]).toEqual(jasmine.objectContaining({ id: 123 }));
            }),
        ));
    });
});
