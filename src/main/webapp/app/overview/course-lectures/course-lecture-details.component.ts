import { Component, OnDestroy, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { AuthServerProvider, JhiWebsocketService } from 'app/core';
import { HttpResponse, HttpClient } from '@angular/common/http';
import { Lecture, LectureService } from 'app/entities/lecture';
import * as moment from 'moment';
import { Attachment } from 'app/entities/attachment';

@Component({
    selector: 'jhi-course-lecture-details',
    templateUrl: './course-lecture-details.component.html',
    styleUrls: ['../course-overview.scss', './course-lectures.scss'],
})
export class CourseLectureDetailsComponent implements OnInit, OnDestroy {
    private subscription: Subscription;
    public lecture: Lecture;
    public isDownloadingLink: string;

    constructor(
        private $location: Location,
        private jhiWebsocketService: JhiWebsocketService,
        private lectureService: LectureService,
        private httpClient: HttpClient,
        private authServerProvider: AuthServerProvider,
        private route: ActivatedRoute,
        private router: Router,
    ) {
        const navigation = this.router.getCurrentNavigation();
        if (navigation.extras.state) {
            const stateLecture = navigation.extras.state.lecture as Lecture;
            if (stateLecture && stateLecture.startDate) {
                stateLecture.startDate = moment(stateLecture.startDate);
            }
            if (stateLecture && stateLecture.endDate) {
                stateLecture.endDate = moment(stateLecture.endDate);
            }
            this.lecture = stateLecture;
        }
    }

    ngOnInit() {
        this.subscription = this.route.params.subscribe(params => {
            if (!this.lecture || this.lecture.id !== params.lectureId) {
                this.lecture = null;
                this.lectureService.find(params.lectureId).subscribe((lectureResponse: HttpResponse<Lecture>) => {
                    this.lecture = lectureResponse.body;
                });
            }
        });
    }

    ngOnDestroy() {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    backToCourse() {
        this.$location.back();
    }

    attachmentNotReleased(attachment: Attachment) {
        return attachment.releaseDate && !moment(attachment.releaseDate).isBefore(moment());
    }

    attachmentExtension(attachment: Attachment) {
        if (!attachment.link) {
            return 'N/A';
        }

        return attachment.link.split('.').pop();
    }

    downloadAttachment(downloadUrl: string) {
        this.isDownloadingLink = downloadUrl;
        this.httpClient.get(downloadUrl, { observe: 'response', responseType: 'blob' }).subscribe(
            response => {
                const blob = new Blob([response.body], { type: response.headers.get('content-type') });
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.setAttribute('href', url);
                link.setAttribute('download', response.headers.get('filename'));
                document.body.appendChild(link); // Required for FF
                link.click();
                window.URL.revokeObjectURL(url);
                this.isDownloadingLink = null;
            },
            error => {
                this.isDownloadingLink = null;
            },
        );
    }
}
