import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import * as moment from 'moment';
import { Lecture } from 'app/entities/lecture.model';
import { FileService } from 'app/shared/http/file.service';
import { Attachment } from 'app/entities/attachment.model';
import { LectureService } from 'app/lecture/lecture.service';
import { LectureUnit, LectureUnitType } from 'app/entities/lecture-unit/lectureUnit.model';
import { PostingsComponent } from 'app/overview/postings/postings.component';
import { onError } from 'app/shared/util/global.utils';
import { finalize } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';

@Component({
    selector: 'jhi-course-lecture-details',
    templateUrl: './course-lecture-details.component.html',
    styleUrls: ['../course-overview.scss', './course-lectures.scss'],
})
export class CourseLectureDetailsComponent implements OnInit {
    lectureId?: number;
    isLoading = false;
    lecture?: Lecture;
    isDownloadingLink?: string;
    lectureUnits: LectureUnit[] = [];
    postings?: PostingsComponent;

    readonly LectureUnitType = LectureUnitType;

    constructor(private alertService: JhiAlertService, private lectureService: LectureService, private activatedRoute: ActivatedRoute, private fileService: FileService) {}

    ngOnInit(): void {
        this.activatedRoute.params.subscribe((params) => {
            this.lectureId = +params['lectureId'];
            if (this.lectureId) {
                this.loadData();
            }
        });
    }

    loadData() {
        this.isLoading = true;
        this.lectureService
            .find(this.lectureId!)
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe(
                (findLectureResult) => {
                    this.lecture = findLectureResult.body!;
                    if (this.lecture?.lectureUnits) {
                        this.lectureUnits = this.lecture.lectureUnits;
                    }
                    if (this.postings) {
                        // We need to manually update the lecture property of the student questions component
                        this.postings.lecture = this.lecture;
                        this.postings.loadPosts(); // reload the student questions
                    }
                },
                (errorResponse: HttpErrorResponse) => onError(this.alertService, errorResponse),
            );
    }
    attachmentNotReleased(attachment: Attachment): boolean {
        return attachment.releaseDate != undefined && !moment(attachment.releaseDate).isBefore(moment())!;
    }

    attachmentExtension(attachment: Attachment): string {
        if (!attachment.link) {
            return 'N/A';
        }

        return attachment.link.split('.').pop()!;
    }

    downloadAttachment(downloadUrl?: string): void {
        if (!this.isDownloadingLink && downloadUrl) {
            this.isDownloadingLink = downloadUrl;
            this.fileService.downloadFileWithAccessToken(downloadUrl);
            this.isDownloadingLink = undefined;
        }
    }

    /**
     * This function gets called if the router outlet gets activated. This is
     * used only for the PostingsComponent
     * @param instance The component instance
     */
    onChildActivate(instance: PostingsComponent) {
        this.postings = instance; // save the reference to the component instance
        if (this.lecture) {
            instance.lecture = this.lecture;
            instance.loadPosts(); // reload the student questions
        }
    }
}
