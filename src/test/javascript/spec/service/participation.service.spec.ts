import { getTestBed, TestBed } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { map, take } from 'rxjs/operators';
import * as moment from 'moment';
import { ParticipationService } from 'app/exercises/shared/participation/participation.service';
import { Participation } from 'app/entities/participation/participation.model';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';

describe('Participation Service', () => {
    let injector: TestBed;
    let service: ParticipationService;
    let httpMock: HttpTestingController;
    let elemDefault: Participation;
    let currentDate: moment.Moment;
    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
        });
        injector = getTestBed();
        service = injector.get(ParticipationService);
        httpMock = injector.get(HttpTestingController);
        currentDate = moment();

        elemDefault = new StudentParticipation();
    });

    it('should find an element', async () => {
        const returnedFromService = Object.assign(
            {
                initializationDate: currentDate.toDate(),
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

    it('should find an element with latest result', async () => {
        const returnedFromService = { ...elemDefault, initializationDate: currentDate.toDate() };
        returnedFromService.results = [{ id: 1 }];
        service
            .findWithLatestResult(123)
            .pipe(take(1))
            .subscribe((resp) => expect(resp).toMatchObject({ body: returnedFromService }));

        const req = httpMock.expectOne({ method: 'GET' });
        req.flush(JSON.stringify(returnedFromService));
    });

    it('should find participation for the exercise', async () => {
        const returnedFromService = { ...elemDefault, initializationDate: currentDate.toDate() };
        returnedFromService.id = 123;
        service
            .findParticipation(123)
            .pipe(take(1))
            .subscribe((resp) => expect(resp).toMatchObject({ body: returnedFromService }));

        const req = httpMock.expectOne({ method: 'GET' });
        req.flush(JSON.stringify(returnedFromService));
    });

    it('should find no participation for the exercise', async () => {
        service
            .findParticipation(123)
            .pipe(take(1))
            .subscribe((resp) => expect(resp).toBeUndefined());

        httpMock.expectOne({ method: 'GET' });
    });

    it('should delete for guided tour', async () => {
        service.deleteForGuidedTour(123).subscribe((resp) => expect(resp.ok));
        let request = httpMock.expectOne({ method: 'DELETE' });
        expect(request.request.params.keys().length).toEqual(0);

        service.deleteForGuidedTour(123, { a: 'param' }).subscribe((resp) => expect(resp.ok));
        request = httpMock.expectOne({ method: 'DELETE' });
        expect(request.request.params.keys().length).toEqual(1);
        expect(request.request.params.get('a')).toEqual('param');
    });

    it('should cleanup build plan', async () => {
        service.cleanupBuildPlan(elemDefault).subscribe((resp) => expect(resp).toMatchObject(elemDefault));
        httpMock.expectOne({ method: 'PUT' });
    });

    it('should update a Participation', async () => {
        const returnedFromService = Object.assign(
            {
                repositoryUrl: 'BBBBBB',
                buildPlanId: 'BBBBBB',
                initializationState: 'BBBBBB',
                initializationDate: currentDate.toDate(),
                presentationScore: 1,
            },
            elemDefault,
        );

        const expected = Object.assign(
            {
                initializationDate: currentDate,
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

    it('should return a list of Participation', async () => {
        const returnedFromService = Object.assign(
            {
                repositoryUrl: 'BBBBBB',
                buildPlanId: 'BBBBBB',
                initializationState: 'BBBBBB',
                initializationDate: currentDate.toDate(),
                presentationScore: 1,
            },
            elemDefault,
        );
        const expected = Object.assign(
            {
                initializationDate: currentDate,
            },
            returnedFromService,
        );
        service
            .findAllParticipationsByExercise(1)
            .pipe(
                take(1),
                map((resp) => resp.body),
            )
            .subscribe((body) => expect(body).toContainEqual(expected));
        const req = httpMock.expectOne({ method: 'GET' });
        req.flush(JSON.stringify([returnedFromService]));
        httpMock.verify();
    });

    it('should delete a Participation', async () => {
        service.delete(123).subscribe((resp) => expect(resp.ok));

        const req = httpMock.expectOne({ method: 'DELETE' });
        req.flush({ status: 200 });
    });

    it.each<any>([
        ['attachment; filename="FixArtifactDownload-Tests-1.0.jar"', 'FixArtifactDownload-Tests-1.0.jar'],
        ['', 'artifact'],
        ['filename="FixArtifactDownload-Tests-1.0.jar"', 'FixArtifactDownload-Tests-1.0.jar'],
        ['f="abc"', 'artifact'],
    ])('%# should download artifact and extract file name: %p', async (headerVal: string, expectedFileName: string, done: jest.DoneCallback) => {
        const expectedBlob = new Blob(['abc', 'cfe'], { type: 'application/java-archive' });
        const headers = new HttpHeaders({ 'content-disposition': headerVal, 'content-type': 'application/java-archive' });
        const response = { body: expectedBlob, headers, status: 200 };

        service.downloadArtifact(123).subscribe((resp) => {
            expect(resp.fileName).toBe(expectedFileName);
            expect(resp.fileContent).toBe(expectedBlob);
            done();
        });

        const req = httpMock.expectOne({ method: 'GET' });
        req.event(new HttpResponse<Blob>(response));
    });

    afterEach(() => {
        httpMock.verify();
    });
});
