import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoUnit } from 'app/entities/lecture-unit/videoUnit.model';
import { VideoUnitFormData } from 'app/lecture/lecture-unit/lecture-unit-management/video-unit-form/video-unit-form.component';
import { VideoUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/videoUnit.service';
import { onError } from 'app/shared/util/global.utils';
import { JhiAlertService } from 'ng-jhipster';
import { finalize } from 'rxjs/operators';

@Component({
    selector: 'jhi-create-video-unit',
    templateUrl: './create-video-unit.component.html',
    styles: [],
})
export class CreateVideoUnitComponent implements OnInit {
    videoUnitToCreate: VideoUnit = new VideoUnit();
    isLoading: boolean;
    lectureId: number;
    courseId: number;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private videoUnitService: VideoUnitService, private alertService: JhiAlertService) {}

    ngOnInit(): void {
        this.activatedRoute.paramMap.subscribe((params) => {
            this.lectureId = Number(params.get('lectureId'));
            this.courseId = Number(params.get('courseId'));
        });
        this.videoUnitToCreate = new VideoUnit();
    }

    createVideoUnit(formData: VideoUnitFormData) {
        if (!formData?.name || !formData?.source) {
            return;
        }

        const { name, description, releaseDate, source } = formData;

        this.videoUnitToCreate.name = name || undefined;
        this.videoUnitToCreate.releaseDate = releaseDate || undefined;
        this.videoUnitToCreate.description = description || undefined;
        this.videoUnitToCreate.source = source || undefined;

        this.isLoading = true;

        this.videoUnitService
            .create(this.videoUnitToCreate!, this.lectureId)
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe(
                () => {
                    this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
                },
                (res: HttpErrorResponse) => onError(this.alertService, res),
            );
    }
}
