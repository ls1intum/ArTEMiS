import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { orderBy as _orderBy } from 'lodash';
import { Subscription } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { hasParticipationChanged, Participation } from 'app/entities/participation';
import { ParticipationWebsocketService } from 'app/entities/participation/participation-websocket.service';
import { Result, ResultService } from '.';
import { AccountService } from '../../core';
import { RepositoryService } from 'app/entities/repository/repository.service';

import * as moment from 'moment';

@Component({
    selector: 'jhi-updating-result',
    templateUrl: './updating-result.component.html',
    providers: [ResultService, RepositoryService],
})

/**
 * A component that wraps the result component, updating its result on every websocket result event for the logged in user.
 * If the participation changes, the newest result from its result array will be used.
 * If the participation does not have any results, there will be no result displayed, until a new result is received through the websocket.
 */
export class UpdatingResultComponent implements OnChanges, OnDestroy {
    @Input() participation: Participation;
    @Input() isBuilding: boolean;
    @Input() short = false;
    @Input() result: Result;
    @Input() showUngradedResults: boolean;
    @Input() showGradedBadge: boolean;

    public resultUpdateListener: Subscription;

    constructor(private accountService: AccountService, private participationWebsocketService: ParticipationWebsocketService) {}

    ngOnChanges(changes: SimpleChanges) {
        if (hasParticipationChanged(changes)) {
            // Sort participation results by completionDate desc.
            this.participation.results = this.participation.results && _orderBy(this.participation.results, 'completionDate', 'desc');
            // The latest result is the first rated result in the sorted array (=newest) or any result if the option is active to show ungraded results.
            this.result = this.participation.results && this.participation.results.find(({ rated }) => this.showUngradedResults || rated === true);
            // Make sure that the participation result is connected to the newest result.
            this.result = this.result && { ...this.result, participation: this.participation };

            this.subscribeForNewResults();
        }
    }

    ngOnDestroy() {
        if (this.resultUpdateListener) {
            this.resultUpdateListener.unsubscribe();
        }
    }

    subscribeForNewResults() {
        // TODO: I don't think we need to check the users identity here.
        // If it could be the case that the result passed into the component would not belong to the user,
        // it would still be shown in html. Only the result subscription would not be instantiated.
        this.accountService.identity().then(user => {
            const { exercise, student } = this.participation;
            // only subscribe for the currently logged in user or if the participation is a template/solution participation and the student is at least instructor
            const isInstructorInCourse = student == null && exercise.course && this.accountService.isAtLeastInstructorInCourse(exercise.course);
            const isSameUser = student && user.id === student.id;

            if (isSameUser || isInstructorInCourse) {
                if (this.resultUpdateListener) {
                    this.resultUpdateListener.unsubscribe();
                }
                this.resultUpdateListener = this.participationWebsocketService
                    .subscribeForLatestResultOfParticipation(this.participation.id)
                    .pipe(
                        // Ignore initial null result of subscription
                        filter(result => !!result),
                        // Ignore ungraded results if ungraded results are supposed to be ignored.
                        filter(result => this.showUngradedResults || result.rated === true),
                        map(result => ({ ...result, completionDate: result.completionDate != null ? moment(result.completionDate) : null, participation: this.participation })),
                        tap(result => (this.result = result)),
                    )
                    .subscribe();
            }
        });
    }
}
