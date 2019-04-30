import { Component, OnDestroy, OnInit } from '@angular/core';
import { ConnectionNotification, ConnectionNotificationType } from 'app/layouts/connection-notification';
import { AccountService, JhiWebsocketService, User } from 'app/core';

@Component({
    selector: 'jhi-connection-notification',
    templateUrl: './connection-notification.component.html',
})
export class ConnectionNotificationComponent implements OnInit, OnDestroy {
    notification = new ConnectionNotification();
    alert: { class: string; icon: string; text: string } | null = null;
    connected: boolean;

    constructor(private accountService: AccountService, private jhiWebsocketService: JhiWebsocketService) {}

    ngOnInit() {
        this.accountService.getAuthenticationState().subscribe((user: User) => {
            if (user) {
                // listen to connect / disconnect events
                this.jhiWebsocketService.bind('connect', this.onConnect);
                this.jhiWebsocketService.bind('disconnect', this.onDisconnect);
            }
        });
    }

    ngOnDestroy() {
        this.jhiWebsocketService.unbind('connect', this.onConnect);
        this.jhiWebsocketService.unbind('disconnect', this.onDisconnect);
    }

    /**
     * Only update on connect if there is not already an active connection.
     * This alert is temporary and disappears after 10 seconds.
     **/

    onConnect = () => {
        if (this.connected === false) {
            this.notification.type = ConnectionNotificationType.RECONNECTED;
            this.updateAlert();
            // The reconnect alert should only be displayed temporarily
            setTimeout(() => {
                this.notification.type = ConnectionNotificationType.CONNECTED;
                this.updateAlert();
            }, 10000);
        }
        this.connected = true;
    };

    /**
     * Only update on disconnect if the connection was active before.
     * This needs to be checked because the websocket service triggers a disconnect before the connect.
     **/

    onDisconnect = () => {
        if (this.connected === true) {
            this.notification.type = ConnectionNotificationType.DISCONNECTED;
            this.updateAlert();
            this.connected = false;
        }
    };

    /**
     * Update the alert to fit the state of the notification.
     **/

    updateAlert(): void {
        if (this.notification) {
            if (this.notification.type === ConnectionNotificationType.DISCONNECTED) {
                this.alert = { class: 'alert-danger', icon: 'times-circle', text: 'arTeMiSApp.connectionAlert.disconnected' };
            } else if (this.notification.type === ConnectionNotificationType.RECONNECTED) {
                this.alert = { class: 'alert-success', icon: 'check-circle', text: 'arTeMiSApp.connectionAlert.reconnected' };
            } else if (this.notification.type === ConnectionNotificationType.CONNECTED) {
                this.alert = null;
            }
        }
    }
}
