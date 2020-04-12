import { Component, Input, ViewChild, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { NgForm } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService } from 'ng-jhipster';
import { HttpResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { TeamService } from 'app/exercises/shared/team/team.service';
import { Team, TeamImportStrategyType as ImportStrategy } from 'app/entities/team.model';
import { Exercise } from 'app/entities/exercise.model';
import { ActionType } from 'app/shared/delete-dialog/delete-dialog.model';
import { flatMap } from 'lodash';
import { User } from 'app/core/user/user.model';

@Component({
    selector: 'jhi-teams-import-dialog',
    templateUrl: './teams-import-dialog.component.html',
    styleUrls: ['./teams-import-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
})
export class TeamsImportDialogComponent implements OnInit, OnDestroy {
    readonly ImportStrategy = ImportStrategy;
    readonly ActionType = ActionType;

    @ViewChild('importForm', { static: false }) importForm: NgForm;

    @Input() exercise: Exercise;
    @Input() teams: Team[]; // existing teams already in exercise

    sourceExercise: Exercise;

    searchingExercises = false;
    searchingExercisesQueryTooShort = false;
    searchingExercisesFailed = false;
    searchingExercisesNoResultsForQuery: string | null = null;

    sourceTeams: Team[] | null;
    loadingSourceTeams = false;
    loadingSourceTeamsFailed = false;

    importStrategy: ImportStrategy | null;
    readonly defaultImportStrategy: ImportStrategy = ImportStrategy.CREATE_ONLY;

    isImporting = false;

    // computed properties
    teamShortNamesAlreadyExistingInExercise: string[] = [];
    studentLoginsAlreadyExistingInExercise: string[] = [];
    sourceTeamsFreeOfConflicts: Team[] = [];

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    constructor(private teamService: TeamService, private activeModal: NgbActiveModal, private jhiAlertService: JhiAlertService) {}

    ngOnInit() {
        this.computePotentialConflictsBasedOnExistingTeams();
    }

    ngOnDestroy(): void {
        this.dialogErrorSource.unsubscribe();
    }

    loadSourceTeams(sourceExercise: Exercise) {
        this.sourceTeams = null;
        this.loadingSourceTeams = true;
        this.loadingSourceTeamsFailed = false;
        this.teamService.findAllByExerciseId(sourceExercise.id).subscribe(
            (teamsResponse) => {
                this.sourceTeams = teamsResponse.body!;
                this.computeSourceTeamsFreeOfConflicts();
                this.loadingSourceTeams = false;
            },
            () => {
                this.loadingSourceTeams = false;
                this.loadingSourceTeamsFailed = true;
            },
        );
    }

    onSelectSourceExercise(exercise: Exercise) {
        this.sourceExercise = exercise;
        this.initImportStrategy();
        this.loadSourceTeams(exercise);
    }

    /**
     * If the exercise has no teams yet, the user doesn't have to chose an import strategy
     * since there is no need for conflict handling decisions when no teams exist yet.
     */
    initImportStrategy() {
        this.importStrategy = this.teams.length === 0 ? this.defaultImportStrategy : null;
    }

    computePotentialConflictsBasedOnExistingTeams() {
        this.teamShortNamesAlreadyExistingInExercise = this.teams.map((team) => team.shortName);
        this.studentLoginsAlreadyExistingInExercise = flatMap(this.teams, (team) => team.students.map((student) => student.login!));
    }

    computeSourceTeamsFreeOfConflicts() {
        this.sourceTeamsFreeOfConflicts = this.sourceTeams!.filter((team: Team) => this.isSourceTeamFreeOfAnyConflicts(team));
    }

    isSourceTeamFreeOfAnyConflicts(sourceTeam: Team): boolean {
        // Short name of source team already exists among teams of destination exercise
        if (this.teamShortNamesAlreadyExistingInExercise.includes(sourceTeam.shortName)) {
            return false;
        }
        // One of the students of the source team is already part of a team in the destination exercise
        if (sourceTeam.students.some((student) => this.studentLoginsAlreadyExistingInExercise.includes(student.login!))) {
            return false;
        }
        // This source team can be imported without any issues
        return true;
    }

    get numberOfConflictFreeSourceTeams(): number {
        return this.sourceTeamsFreeOfConflicts.length;
    }

    get numberOfTeamsToBeDeleted(): number | null {
        switch (this.importStrategy) {
            case ImportStrategy.PURGE_EXISTING:
                return this.teams.length;
            case ImportStrategy.CREATE_ONLY:
                return 0;
            default:
                return null;
        }
    }

    get numberOfTeamsToBeImported(): number | null {
        switch (this.importStrategy) {
            case ImportStrategy.PURGE_EXISTING:
                return this.sourceTeams!.length;
            case ImportStrategy.CREATE_ONLY:
                return this.numberOfConflictFreeSourceTeams;
            default:
                return null;
        }
    }

    get numberOfTeamsAfterImport(): number | null {
        switch (this.importStrategy) {
            case ImportStrategy.PURGE_EXISTING:
                return this.sourceTeams!.length;
            case ImportStrategy.CREATE_ONLY:
                return this.teams.length + this.numberOfConflictFreeSourceTeams;
            default:
                return null;
        }
    }

    /**
     * Computed flag whether to prompt the user to pick an import strategy.
     *
     * Conditions that need to be fulfilled in order for the strategy choices to show:
     * 1. A source exercise has been selected
     * 2. The source exercise has teams that could be imported
     * 3. The current exercise already has existing teams in it
     */
    get showImportStrategyChoices(): boolean {
        return this.sourceExercise && this.sourceTeams!?.length > 0 && this.teams.length > 0;
    }

    updateImportStrategy(importStrategy: ImportStrategy) {
        this.importStrategy = importStrategy;
    }

    get showImportPreviewNumbers(): boolean {
        return this.sourceExercise && this.sourceTeams! && Boolean(this.importStrategy);
    }

    /**
     * The import button is disabled if one of the following conditions apply:
     *
     * 1. Import is already in progress
     * 2. No source exercise has been selected yet
     * 3. Source teams have not been loaded yet
     * 4. No import strategy has been chosen yet
     * 5. There are no (conflict-free depending on strategy) source teams to be imported
     */
    get isSubmitDisabled(): boolean {
        return this.isImporting || !this.sourceExercise || !this.sourceTeams || !this.importStrategy || !this.numberOfTeamsToBeImported;
    }

    clear() {
        this.activeModal.dismiss('cancel');
    }

    purgeAndImportTeams() {
        this.dialogErrorSource.next('');
        this.importTeams();
    }

    importTeams() {
        if (this.isSubmitDisabled) {
            return;
        }
        this.isImporting = true;
        this.teamService.importTeamsFromSourceExercise(this.exercise, this.sourceExercise, this.importStrategy!).subscribe(
            (res) => this.onSaveSuccess(res),
            () => this.onSaveError(),
        );
    }

    onSaveSuccess(teams: HttpResponse<Team[]>) {
        this.activeModal.close(teams.body);
        this.isImporting = false;
    }

    onSaveError() {
        this.jhiAlertService.error('artemisApp.team.importError');
        this.isImporting = false;
    }

    get sampleTeamForLegend() {
        const team = new Team();
        const student = new User(1, 'ga12abc', 'John', 'Doe', 'john.doe@tum.de');
        student.name = `${student.firstName} ${student.lastName}`;
        team.students = [student];
        return team;
    }

    get sampleErrorStudentLoginsForLegend() {
        return this.sampleTeamForLegend.students.map((student) => student.login);
    }
}
