import { Injectable } from '@angular/core';
import * as moment from 'moment';

import { Result } from './result.model';
import { JhiWebsocketService, AccountService } from 'app/core';
import { Subject, Observable } from 'rxjs';
import { finalize, share } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ResultWebsocketService {
    private subscriptions: { [key: string]: [Subject<Result>, Observable<Result>] } = {};

    constructor(private jhiWebsocketService: JhiWebsocketService, private accountService: AccountService) {}

    /**
     * Subscribe to receiving new results for a given participation.
     * Unsubscribes from the websocket channel when the last subscriber has unsubscribed.
     * @param participationId
     */
    async subscribeResultForParticipation(participationId: number): Promise<Observable<Result>> {
        return this.accountService.identity().then(() => {
            const channel = `/topic/participation/${participationId}/newResults`;
            if (!this.subscriptions[channel]) {
                const subject = new Subject() as Subject<Result>;
                const shared = subject.pipe(
                    // unsubscribe websocket if all subscribers of the subject have unsubscribed
                    finalize(() => this.unsubscribeChannel(channel)),
                    share(),
                ) as Observable<Result>;
                this.subscribeChannel(channel, subject);
                this.subscriptions[channel] = [subject, shared];
                return shared;
            } else {
                const [, observable] = this.subscriptions[channel];
                return observable;
            }
        });
    }

    private subscribeChannel(channel: string, subject: Subject<Result>) {
        this.jhiWebsocketService.subscribe(channel);
        this.jhiWebsocketService.receive(channel).subscribe((newResult: Result) => {
            console.log('Received new result ' + newResult.id + ': ' + newResult.resultString);
            // convert json string to moment
            newResult.completionDate = newResult.completionDate != null ? moment(newResult.completionDate) : null;
            subject.next(newResult);
        });
    }

    private unsubscribeChannel(channel: string) {
        this.jhiWebsocketService.unsubscribe(channel);
        delete this.subscriptions[channel];
    }
}
