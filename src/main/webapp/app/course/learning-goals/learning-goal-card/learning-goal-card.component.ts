import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { LearningGoal } from 'app/entities/learningGoal.model';
import { LectureUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/lectureUnit.service';
import { LearningGoalDetailModalComponent } from 'app/course/learning-goals/learning-goal-detail-modal/learning-goal-detail-modal.component';
import { LearningGoalProgress } from 'app/course/learning-goals/learning-goal-progress-dtos.model';

@Component({
    selector: 'jhi-learning-goal-card',
    templateUrl: './learning-goal-card.component.html',
    styleUrls: ['./learning-goal-card.component.scss'],
})
export class LearningGoalCardComponent implements OnInit, OnDestroy {
    @Input()
    learningGoal: LearningGoal;
    @Input()
    learningGoalProgress: LearningGoalProgress;

    public predicate = 'id';
    public reverse = false;
    public progressText = '';
    public progressInPercent = 0;
    public isProgressAvailable = false;
    public ModalComponent = LearningGoalDetailModalComponent;

    constructor(private modalService: NgbModal, public lectureUnitService: LectureUnitService, public translateService: TranslateService) {}

    ngOnInit(): void {
        if (!this.learningGoalProgress || this.learningGoalProgress.totalPointsAchievableByStudentsInLearningGoal === 0) {
            this.isProgressAvailable = false;
        } else {
            this.isProgressAvailable = true;
            this.progressText = this.translateService.instant('artemisApp.learningGoal.learningGoalCard.achieved');

            const progress = (this.learningGoalProgress.pointsAchievedByStudentInLearningGoal / this.learningGoalProgress.totalPointsAchievableByStudentsInLearningGoal) * 100;
            this.progressInPercent = Math.round(progress * 10) / 10;
        }
    }

    ngOnDestroy(): void {
        if (this.modalService.hasOpenModals()) {
            this.modalService.dismissAll();
        }
    }
    openLearningGoalDetailsModal() {
        const modalRef = this.modalService.open(this.ModalComponent, {
            size: 'xl',
        });
        if (modalRef) {
            modalRef.componentInstance.learningGoal = this.learningGoal;
            modalRef.componentInstance.learningGoalProgress = this.learningGoalProgress;
        }
    }
}
