import { CourseService } from '../entities/course';
import { JhiAlertService } from 'ng-jhipster';
import { AfterViewInit, Component, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { ActivatedRoute } from '@angular/router';
import { Participation, ParticipationService } from '../entities/participation';
import { RepositoryFileService, RepositoryService } from '../entities/repository/repository.service';
import { Result } from '../entities/result';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import * as $ from 'jquery';
import * as interact from 'interactjs';

@Component({
    selector: 'jhi-editor',
    templateUrl: './editor.component.html',
    providers: [JhiAlertService, CourseService, RepositoryFileService]
})

/**
 * @class EditorComponent
 * @desc This component acts as a wrapper for the upgraded editor component (directive).
 * The dependencies are passed along to the directive, from there to the legacy component.
 */
export class EditorComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {
    /** Dependencies as defined by the Editor component */
    participation: Participation;
    repository: RepositoryService;
    file: string;
    paramSub: Subscription;
    repositoryFiles: string[];
    latestResult: Result;
    saveStatusLabel: string;

    /** Enable initial refresh call for result component **/
    doInitialRefresh = true;

    /** File Status Booleans **/
    isSaved = true;
    isBuilding = false;
    isCommitted: boolean;

    /** Resizable sizing constants **/
    resizableMinWidth = 100;
    resizableMaxWidth = 800;

    /**
     * @constructor EditorComponent
     * @param {ActivatedRoute} route
     * @param {ParticipationService} participationService
     * @param {RepositoryService} repositoryService
     * @param {RepositoryFileService} repositoryFileService
     */
    constructor(
        private route: ActivatedRoute,
        private participationService: ParticipationService,
        private repositoryService: RepositoryService,
        private repositoryFileService: RepositoryFileService
    ) {}

    /**
     * @function ngOnInit
     * @desc Fetches the participation and the repository files for the provided participationId in params
     */
    ngOnInit(): void {
        this.paramSub = this.route.params.subscribe(params => {
            /** Query the participationService for the participationId given by the params */
            this.participationService.find(params['participationId']).subscribe((response: HttpResponse<Participation>) => {
                this.participation = response.body;
                this.checkIfRepositoryIsClean();
            });
            /** Query the repositoryFileService for files in the repository */
            this.repositoryFileService.query(params['participationId']).subscribe(
                files => {
                    this.repositoryFiles = files;
                },
                (error: HttpErrorResponse) => {
                    console.log('There was an error while getting files: ' + error.message + ': ' + error.error);
                }
            );
        });

        /** Assign repository */
        this.repository = this.repositoryService;
    }

    /**
     * @function ngAfterViewInit
     * @desc After the view was initialized, we create an interact.js resizable object,
     *       designate the edges which can be used to resize the target element and set min and max values.
     *       The 'resizemove' callback function processes the event values and sets new width and height values for the element.
     */
    ngAfterViewInit(): void {
        interact('.resizable-filebrowser')
            .resizable({
                // Enable resize from right edge; triggered by class rg-right
                edges: { left: false, right: '.rg-right', bottom: false, top: false },
                // Set min and max width
                restrictSize: {
                    min: { width: this.resizableMinWidth },
                    max: { width: this.resizableMaxWidth }
                },
                inertia: true
            })
            .on('resizemove', function(event) {
                const target = event.target;
                // Update element size
                target.style.width = event.rect.width + 'px';
                target.style.height = event.rect.height + 'px';
            });
    }

    /**
     * @function ngOnChanges
     * @desc Checks if the repository has uncommitted changes
     * @param changes
     */
    ngOnChanges(changes: SimpleChanges) {
        this.checkIfRepositoryIsClean();
    }

    /**
     * @function checkIfRepositoryIsClean
     * @desc Calls the repository service to see if the repository has uncommitted changes
     */
    checkIfRepositoryIsClean(): void {
        this.repository.isClean(this.participation.id).subscribe(res => {
            this.isCommitted = res.isClean;
        });
    }

    /**
     * @function updateSaveStatusLabel
     * @desc Callback function for a save status changes of files
     * @param $event Event object which contains information regarding the save status of files
     */
    updateSaveStatusLabel($event: any) {
        this.isSaved = $event.isSaved;
        if (!this.isSaved) {
            this.isCommitted = false;
        }
        this.saveStatusLabel = $event.saveStatusLabel;
    }

    /**
     * @function updateLatestResult
     * @desc Callback function for when a new result is received from the result component
     * @param $event Event object which contains the newly received result
     */
    updateLatestResult($event: any) {
        this.isBuilding = false;
        this.latestResult = $event.newResult;
    }

    /**
     * @function updateSelectedFile
     * @desc Callback function for when a new file is selected within the file-browser component
     * @param $event Event object which contains the new file name
     */
    updateSelectedFile($event: any) {
        this.file = $event.fileName;
    }

    /**
     * @function updateRepositoryCommitStatus
     * @desc Callback function for when a file was created or deleted; updates the current repository files
     */
    updateRepositoryCommitStatus($event: any) {
        this.isSaved = false;
        this.isCommitted = false;
        /** Query the repositoryFileService for updated files in the repository */
        this.repositoryFileService.query(this.participation.id).subscribe(
            files => {
                this.repositoryFiles = files;
                // Select newly created file
                if ($event.mode === 'create') {
                    this.file = $event.file;
                }
            },
            (error: HttpErrorResponse) => {
                console.log('There was an error while getting files: ' + error.message + ': ' + error.error);
            }
        );
    }

    /**
     * @function toggleCollapse
     * @desc Collapse parts of the editor (file browser, build output...)
     * @param $event
     * @param horizontal
     */
    toggleCollapse($event: any, horizontal: boolean) {
        const target = $event.toElement || $event.relatedTarget || $event.target;
        target.blur();
        const $card = $(target).closest('.card');

        if ($card.hasClass('collapsed')) {
            $card.removeClass('collapsed');
        } else {
            $card.addClass('collapsed');
            horizontal ? $card.height('35px') : $card.width('55px');
        }
    }

    /**
     * @function commit
     * @desc Commits the current repository files
     * @param $event
     */
    commit($event: any) {
        const target = $event.toElement || $event.relatedTarget || $event.target;
        target.blur();
        this.isBuilding = true;
        this.repository.commit(this.participation.id).subscribe(
            res => {
                this.isCommitted = true;
            },
            err => {
                console.log('Error during commit ocurred!', err);
            }
        );
    }

    /**
     * @function ngOnDestroy
     * @desc Framework function which is executed when the component is destroyed.
     * Used for component cleanup, close open sockets, connections, subscriptions...
     */
    ngOnDestroy() {
        /** Unsubscribe onDestroy to avoid performance issues due to a high number of open subscriptions */
        this.paramSub.unsubscribe();
    }
}
