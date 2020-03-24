/* tslint:disable max-line-length */
import { getTestBed, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { map, take } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';
import { CourseService } from 'app/course/manage/course.service';
import { Course, ICourse } from 'app/shared/model/course.model';

describe('Service Tests', () => {
    describe('Course Service', () => {
        let injector: TestBed;
        let service: CourseService;
        let httpMock: HttpTestingController;
        let elemDefault: ICourse;
        let currentDate: moment.Moment;
        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HttpClientTestingModule],
            });
            injector = getTestBed();
            service = injector.get(CourseService);
            httpMock = injector.get(HttpTestingController);
            currentDate = moment();

            elemDefault = new Course(0, 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', currentDate, currentDate, false);
        });

        describe('Service methods', async () => {
            it('should find an element', async () => {
                const returnedFromService = Object.assign(
                    {
                        startDate: currentDate.format(DATE_TIME_FORMAT),
                        endDate: currentDate.format(DATE_TIME_FORMAT),
                    },
                    elemDefault,
                );
                service
                    .find(123)
                    .pipe(take(1))
                    .subscribe((resp) => expect(resp).toMatchObject({ body: elemDefault }));

                const req = httpMock.expectOne({ method: 'GET' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should create a Course', async () => {
                const returnedFromService = Object.assign(
                    {
                        id: 0,
                        startDate: currentDate.format(DATE_TIME_FORMAT),
                        endDate: currentDate.format(DATE_TIME_FORMAT),
                    },
                    elemDefault,
                );
                const expected = Object.assign(
                    {
                        startDate: currentDate,
                        endDate: currentDate,
                    },
                    returnedFromService,
                );
                service
                    .create(new Course(null))
                    .pipe(take(1))
                    .subscribe((resp) => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'POST' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should update a Course', async () => {
                const returnedFromService = Object.assign(
                    {
                        title: 'BBBBBB',
                        studentGroupName: 'BBBBBB',
                        teachingAssistantGroupName: 'BBBBBB',
                        instructorGroupName: 'BBBBBB',
                        startDate: currentDate.format(DATE_TIME_FORMAT),
                        endDate: currentDate.format(DATE_TIME_FORMAT),
                        onlineCourse: true,
                    },
                    elemDefault,
                );

                const expected = Object.assign(
                    {
                        startDate: currentDate,
                        endDate: currentDate,
                    },
                    returnedFromService,
                );
                service
                    .update(expected)
                    .pipe(take(1))
                    .subscribe((resp) => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'PUT' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should return a list of Course', async () => {
                const returnedFromService = Object.assign(
                    {
                        title: 'BBBBBB',
                        studentGroupName: 'BBBBBB',
                        teachingAssistantGroupName: 'BBBBBB',
                        instructorGroupName: 'BBBBBB',
                        startDate: currentDate.format(DATE_TIME_FORMAT),
                        endDate: currentDate.format(DATE_TIME_FORMAT),
                        onlineCourse: true,
                    },
                    elemDefault,
                );
                const expected = Object.assign(
                    {
                        startDate: currentDate,
                        endDate: currentDate,
                    },
                    returnedFromService,
                );
                service
                    .query(expected)
                    .pipe(
                        take(1),
                        map((resp) => resp.body),
                    )
                    .subscribe((body) => expect(body).toContainEqual(expected));
                const req = httpMock.expectOne({ method: 'GET' });
                req.flush(JSON.stringify([returnedFromService]));
                httpMock.verify();
            });

            it('should delete a Course', async () => {
                const rxPromise = service.delete(123).subscribe((resp) => expect(resp.ok));

                const req = httpMock.expectOne({ method: 'DELETE' });
                req.flush({ status: 200 });
            });
        });

        afterEach(() => {
            httpMock.verify();
        });
    });
});
