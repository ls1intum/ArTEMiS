import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { JhiHealthModalComponent, JhiHealthService } from 'app/admin';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';

@Component({
    selector: 'jhi-health',
    templateUrl: './health.component.html',
})
export class JhiHealthCheckComponent implements OnInit {
    healthData: any;
    updatingHealth: boolean;
    disconnected = true;
    onConnected: () => void;
    onDisconnected: () => void;

    constructor(private modalService: NgbModal, private healthService: JhiHealthService, private trackerService: JhiWebsocketService) {}

    ngOnInit() {
        this.refresh();

        // listen to connect / disconnect events
        this.onConnected = () => {
            this.disconnected = false;
        };
        this.trackerService.bind('connect', () => {
            this.onConnected();
        });
        this.onDisconnected = () => {
            this.disconnected = true;
        };
        this.trackerService.bind('disconnect', () => {
            this.onDisconnected();
        });
    }

    baseName(name: string) {
        return this.healthService.getBaseName(name);
    }

    getBadgeClass(statusState: string) {
        if (statusState === 'UP') {
            return 'badge-success';
        } else {
            return 'badge-danger';
        }
    }

    refresh() {
        this.updatingHealth = true;

        this.healthService.checkHealth().subscribe(
            health => {
                this.healthData = this.healthService.transformHealthData(health);
                this.updatingHealth = false;
            },
            error => {
                if (error.status === 503) {
                    this.healthData = this.healthService.transformHealthData(error.error);
                    this.updatingHealth = false;
                }
            },
        );
    }

    showHealth(health: any) {
        const modalRef = this.modalService.open(JhiHealthModalComponent);
        modalRef.componentInstance.currentHealth = health;
        modalRef.result.then(
            result => {
                // Left blank intentionally, nothing to do here
            },
            reason => {
                // Left blank intentionally, nothing to do here
            },
        );
    }

    subSystemName(name: string) {
        return this.healthService.getSubSystemName(name);
    }
}
