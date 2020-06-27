import { DomainDependentService } from 'app/exercises/programming/shared/code-editor/service/code-editor-domain-dependent.service';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { DomainChange, DomainType } from 'app/exercises/programming/shared/code-editor/model/code-editor.model';
import { DomainService } from 'app/exercises/programming/shared/code-editor/service/code-editor-domain.service';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { TemplateProgrammingExerciseParticipation } from 'app/entities/participation/template-programming-exercise-participation.model';
import { SolutionProgrammingExerciseParticipation } from 'app/entities/participation/solution-programming-exercise-participation.model';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { ProgrammingExerciseStudentParticipation } from 'app/entities/participation/programming-exercise-student-participation.model';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

/**
 * Service that can be extended to update rest endpoint urls with the received domain information.
 */
export abstract class DomainDependentEndpointService extends DomainDependentService {
    private restResourceUrlBase = `${SERVER_API_URL}/api`;
    protected restResourceUrl: string | null;
    private websocketResourceUrlBase = '/topic';
    protected websocketResourceUrlSend: string | null;
    protected websocketResourceUrlReceive: string | null;
    protected domainValue: StudentParticipation | TemplateProgrammingExerciseParticipation | SolutionProgrammingExerciseParticipation | ProgrammingExercise;

    private participations: ProgrammingExerciseStudentParticipation[] = [];

    constructor(protected http: HttpClient, protected jhiWebsocketService: JhiWebsocketService, domainService: DomainService) {
        super(domainService);
        this.initDomainSubscription();
    }

    /**
     * Sets resourceUrls according to the parameter.
     * @param domain - enum that defines the type of the domain.
     */
    setDomain(domain: DomainChange) {
        super.setDomain(domain);
        const [domainType, domainValue] = this.domain;
        this.domainValue = domainValue;
        switch (domainType) {
            case DomainType.PARTICIPATION:
                this.restResourceUrl = `${this.restResourceUrlBase}/repository/${domainValue.id}`;
                this.websocketResourceUrlSend = `${this.websocketResourceUrlBase}/repository/${domainValue.id}`;
                this.websocketResourceUrlReceive = `/user${this.websocketResourceUrlSend}`;
                break;
            case DomainType.TEST_REPOSITORY:
                this.restResourceUrl = `${this.restResourceUrlBase}/test-repository/${domainValue.id}`;
                this.websocketResourceUrlSend = `${this.websocketResourceUrlBase}/test-repository/${domainValue.id}`;
                this.websocketResourceUrlReceive = `/user${this.websocketResourceUrlSend}`;
                break;
            default:
                this.restResourceUrl = null;
                this.websocketResourceUrlSend = null;
                this.websocketResourceUrlReceive = null;
        }
    }

    isOnline() {
        return navigator.onLine; // TODO change to more robust method
    }

    fallbackWhenOfflineOrUnavailable<T>(executeRequest: () => Observable<T>, executeFallback: (participation: ProgrammingExerciseStudentParticipation) => Observable<T>) {
        let fallback = () => {
            const participation = this.getParticipation();
            if (participation) return executeFallback(participation);
            else return throwError(new Error('Cannot find participation for current domain.'));
        };

        if (!this.isOnline()) {
            return fallback();
        } else {
            return executeRequest().pipe(
                catchError((err: HttpErrorResponse) => {
                    if (err.status == 0) {
                        return fallback();
                    } else {
                        throw err;
                    }
                }),
            );
        }
    }

    addParticipation(participation: ProgrammingExerciseStudentParticipation) {
        this.participations = this.participations.filter((p) => p.exercise.id == participation.exercise.id && p.id == participation.id).concat([participation]);
    }

    getParticipation(): ProgrammingExerciseStudentParticipation | undefined {
        return this.participations.find((participation) => participation.id === this.domainValue.id);
    }
}
