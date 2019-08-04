import { ErrorHandler, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { cloneDeep } from 'lodash';
import { JhiAlertService } from 'ng-jhipster';
import { fromEvent, Observable, of, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/internal/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { courseOverviewTour } from 'app/guided-tour/tours/course-overview-tour';
import { GuidedTourSettings } from 'app/guided-tour/guided-tour-settings.model';
import { ContentType, GuidedTour, Orientation, OrientationConfiguration, TourStep } from './guided-tour.constants';
import { AccountService } from 'app/core';

export type EntityResponseType = HttpResponse<GuidedTourSettings>;

@Injectable({ providedIn: 'root' })
export class GuidedTourService {
    public resourceUrl = SERVER_API_URL + 'api/guided-tour-settings';

    public currentTourSteps: TourStep[];
    private guidedTourSettings: GuidedTourSettings;
    private guidedTourCurrentStepSubject = new Subject<TourStep | null>();
    private currentTourStepIndex = 0;
    private currentTour: GuidedTour | null;
    private onResizeMessage = false;

    constructor(
        public errorHandler: ErrorHandler,
        private http: HttpClient,
        private jhiAlertService: JhiAlertService,
        private accountService: AccountService,
        private router: Router,
    ) {
        this.getGuidedTourSettings();

        fromEvent(window, 'resize')
            .pipe(debounceTime(200))
            .subscribe(() => {
                if (this.currentTour && this.currentTourStepIndex > -1) {
                    if (this.currentTour.minimumScreenSize && this.currentTour.minimumScreenSize >= window.innerWidth) {
                        this.onResizeMessage = true;
                        this.guidedTourCurrentStepSubject.next({
                            headlineTranslateKey: 'tour.resize.headline',
                            contentType: ContentType.TEXT,
                            contentTranslateKey: 'tour.resize.content',
                        });
                    } else {
                        this.onResizeMessage = false;
                        this.guidedTourCurrentStepSubject.next(this.getPreparedTourStep(this.currentTourStepIndex));
                    }
                }
            });
    }

    public getGuidedTourCurrentStepStream(): Observable<TourStep | null> {
        return this.guidedTourCurrentStepSubject.asObservable();
    }

    /**
     * Load course overview tour
     */
    public getOverviewTour(): Observable<GuidedTour> {
        return of(courseOverviewTour);
    }

    /**
     * Navigate to next tour step
     */
    public nextStep(): void {
        if (!this.currentTour) {
            return;
        }
        const currentStep = this.currentTour.steps[this.currentTourStepIndex];
        if (currentStep.closeAction) {
            currentStep.closeAction();
        }
        if (this.currentTour.steps[this.currentTourStepIndex + 1]) {
            this.currentTourStepIndex++;
            if (currentStep.action) {
                currentStep.action();
            }
            // Usually an action is opening something so we need to give it time to render.
            setTimeout(() => {
                if (this.checkSelectorValidity()) {
                    this.guidedTourCurrentStepSubject.next(this.getPreparedTourStep(this.currentTourStepIndex));
                } else {
                    this.nextStep();
                }
            });
        } else {
            if (this.currentTour.completeCallback) {
                this.currentTour.completeCallback();
            }
            this.updateGuidedTourSettings(this.currentTour.settingsId, false).subscribe(guidedTourSettings => {
                if (guidedTourSettings.body) {
                    this.guidedTourSettings = guidedTourSettings.body;
                }
            });
            this.resetTour();
        }
    }

    /**
     * Navigate to previous tour step
     */
    public backStep(): void {
        if (this.currentTour) {
            const currentStep = this.currentTour.steps[this.currentTourStepIndex];
            if (currentStep.closeAction) {
                currentStep.closeAction();
            }
            if (this.currentTour.steps[this.currentTourStepIndex - 1]) {
                this.currentTourStepIndex--;
                if (currentStep.action) {
                    currentStep.action();
                }
                setTimeout(() => {
                    if (this.checkSelectorValidity()) {
                        this.guidedTourCurrentStepSubject.next(this.getPreparedTourStep(this.currentTourStepIndex));
                    } else {
                        this.backStep();
                    }
                });
            } else {
                this.resetTour();
            }
        }
    }

    /**
     * Skip tour
     */
    public skipTour(): void {
        if (this.currentTour) {
            if (this.currentTour.skipCallback) {
                this.currentTour.skipCallback(this.currentTourStepIndex);
            }
            this.resetTour();
        }
    }

    /**
     * Close tour and remove overlay
     */
    public resetTour(): void {
        document.body.classList.remove('tour-open');
        this.currentTour = null;
        this.currentTourStepIndex = 0;
        this.guidedTourCurrentStepSubject.next(null);
    }

    /**
     * Start guided tour for given guided tour
     * @param tour  guided tour
     */
    private startTour(tour: GuidedTour): void {
        this.currentTourSteps = tour.steps;

        // adjust tour steps according to permissions
        tour.steps.forEach((step, index) => {
            if (step.permission && !this.accountService.hasAnyAuthorityDirect(step.permission)) {
                this.currentTourSteps.splice(index, 1);
            }
        });

        this.currentTour = cloneDeep(tour);
        this.currentTour.steps = this.currentTour.steps.filter(step => !step.skipStep);
        this.currentTourStepIndex = 0;
        if (this.currentTour.steps.length > 0 && (!this.currentTour.minimumScreenSize || window.innerWidth >= this.currentTour.minimumScreenSize)) {
            const currentStep = this.currentTour.steps[this.currentTourStepIndex];
            if (currentStep.action) {
                currentStep.action();
            }

            if (this.checkSelectorValidity()) {
                this.guidedTourCurrentStepSubject.next(this.getPreparedTourStep(this.currentTourStepIndex));
            } else {
                this.nextStep();
            }
        }
    }

    /* Check if highlighted element is available */
    private checkSelectorValidity(): boolean {
        if (!this.currentTour) {
            return false;
        }
        if (this.currentTour.steps[this.currentTourStepIndex].selector) {
            const selectedElement = document.querySelector(this.currentTour.steps[this.currentTourStepIndex].selector!);
            if (!selectedElement) {
                this.errorHandler.handleError(
                    // If error handler is configured this should not block the browser.
                    new Error(
                        `Error finding selector ${this.currentTour.steps[this.currentTourStepIndex].selector} on step ${this.currentTourStepIndex + 1} during guided tour: ${
                            this.currentTour.settingsId
                        }`,
                    ),
                );
                return false;
            }
        }
        return true;
    }

    /**
     *  Is last tour step
     */
    public get isOnLastStep(): boolean {
        if (!this.currentTour) {
            return false;
        }
        return this.currentTour.steps.length - 1 === this.currentTourStepIndex;
    }

    /**
     * Is first tour step
     */
    public get isOnFirstStep(): boolean {
        return this.currentTourStepIndex === 0;
    }

    /* Show resize message */
    public get isOnResizeMessage(): boolean {
        return this.onResizeMessage;
    }

    /* Current tour step number */
    public get currentTourStepDisplay(): number {
        return this.currentTourStepIndex + 1;
    }

    /* Total count of tour steps */
    public get currentTourStepCount(): any {
        return this.currentTour && this.currentTour.steps ? this.currentTour.steps.length : 0;
    }

    /* Prevents the tour from advancing by clicking the backdrop */
    public get preventBackdropFromAdvancing(): boolean {
        if (this.currentTour) {
            return this.currentTour && (this.currentTour.preventBackdropFromAdvancing ? this.currentTour.preventBackdropFromAdvancing : false);
        }
        return false;
    }

    /**
     * Get the tour step with defined orientation
     * @param index current tour step index
     */
    private getPreparedTourStep(index: number): TourStep | undefined {
        if (this.currentTour) {
            return this.setTourOrientation(this.currentTour.steps[index]);
        } else {
            return undefined;
        }
    }

    /**
     * Set orientation of the passed on tour step
     * @param step  passed on tour step of a guided tour
     */
    private setTourOrientation(step: TourStep): TourStep {
        const convertedStep = cloneDeep(step);
        if (convertedStep.orientation && !(typeof convertedStep.orientation === 'string') && (convertedStep.orientation as OrientationConfiguration[]).length) {
            (convertedStep.orientation as OrientationConfiguration[]).sort((a: OrientationConfiguration, b: OrientationConfiguration) => {
                if (!b.maximumSize) {
                    return 1;
                }
                if (!a.maximumSize) {
                    return -1;
                }
                return b.maximumSize - a.maximumSize;
            });

            let currentOrientation: Orientation = Orientation.Top;
            (convertedStep.orientation as OrientationConfiguration[]).forEach((orientationConfig: OrientationConfiguration) => {
                if (!orientationConfig.maximumSize || window.innerWidth <= orientationConfig.maximumSize) {
                    currentOrientation = orientationConfig.orientationDirection;
                }
            });

            convertedStep.orientation = currentOrientation;
        }
        return convertedStep;
    }

    /**
     * Send a GET request for the guided tour settings of the current user
     */
    private fetchGuidedTourSettings(): Observable<GuidedTourSettings> {
        return this.http.get<GuidedTourSettings>(this.resourceUrl, { observe: 'response' }).map((res: EntityResponseType) => {
            if (!res.body) {
                throw new Error('Empty response returned while fetching guided tour settings');
            }
            return res.body;
        });
    }

    /**
     * Send a PUT request to update the guided tour settings of the current user
     * @param settingName   name of the tour setting that is stored in the guided tour settings json in the DB, e.g. showCourseOverviewTour
     * @param settingValue  boolean value that defines if the tour for [settingName] should be displayed automatically
     */
    public updateGuidedTourSettings(settingName: string, settingValue: boolean): Observable<EntityResponseType> {
        if (!this.guidedTourSettings) {
            throw new Error('Cannot update non existing guided tour settings');
        }
        this.guidedTourSettings[settingName] = settingValue;
        return this.http.put<GuidedTourSettings>(this.resourceUrl, this.guidedTourSettings, { observe: 'response' });
    }

    /**
     * Subscribe to guided tour settings and store value in service class variable
     */
    public getGuidedTourSettings() {
        this.fetchGuidedTourSettings().subscribe(guidedTourSettings => {
            if (guidedTourSettings) {
                this.guidedTourSettings = guidedTourSettings;
            }
        });
    }

    /**
     * Checks if the current component has a guided tour by comparing the current router url to manually defined urls
     * that provide tours.
     */
    public checkGuidedTourAvailabilityForCurrentRoute(): boolean {
        if (this.router.url === '/overview') {
            return true;
        }
        return false;
    }

    /**
     * Starts the guided tour of the current component
     * */
    public startGuidedTourForCurrentRoute() {
        if (this.router.url === '/overview') {
            this.getOverviewTour().subscribe(tour => {
                this.startTour(tour);
            });
        }
    }
}
