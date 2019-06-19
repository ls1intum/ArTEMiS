import { concat, compose, differenceWith } from 'lodash/fp';
import { differenceWith as _differenceWith } from 'lodash';
import { AnnotationArray } from 'app/entities/ace-editor/annotation.model';
import { TextChange } from './text-change.model';

type sessionObj = [string, { code?: string; unsavedChanges: boolean; errors: AnnotationArray | null }];

/**
 * Wrapper class for managing editor file sessions.
 * This includes the files content (code), possible errors and a dirty flag (unsavedChanges).
 */
export class EditorFileSession {
    private fileSession: Array<sessionObj>;

    public addFiles(...files: Array<{ fileName: string; code?: string; errors?: AnnotationArray }>) {
        const newSessionObjs = files.map(
            ({ fileName, code, errors }): sessionObj => [fileName, { code: code || undefined, errors: errors || new AnnotationArray(), unsavedChanges: false }],
        );
        this.fileSession = [...this.fileSession, ...newSessionObjs];
    }

    /**
     * Util method for adding new files and removing old ones at the same time.
     * @param filesToAdd files for which an initialized entry should be created.
     * @param filesToRemove files that should be removed from the file session.
     */
    public update(filesToAdd: string[], filesToRemove: string[]) {
        const newEntries = filesToAdd.map((fileName): sessionObj => [fileName, { errors: new AnnotationArray(), code: undefined, unsavedChanges: false }]);
        this.fileSession = compose(
            concat(newEntries),
            differenceWith(([a], b) => a === b, this.fileSession),
        )(filesToRemove);
    }

    public updateErrorPositions(fileName: string, change: TextChange) {
        this.fileSession = this.fileSession.map(([f, session]): sessionObj => [f, f === fileName ? { ...session, errors: session.errors!.update(change) } : session]);
    }

    public setCode(fileName: string, code: string) {
        this.fileSession = this.fileSession.map(([f, session]): sessionObj => (f === fileName ? [f, { ...session, code }] : [f, session]));
    }

    public getCode(fileName: string) {
        const session = this.fileSession.find(([f]) => f === fileName);
        return session ? session[1].code : undefined;
    }

    public getErrors(fileName: string) {
        const session = this.fileSession.find(([f]) => f === fileName);
        return session ? session[1].errors : undefined;
    }

    public setErrors(...buildLogErrors: Array<[string, AnnotationArray]>) {
        this.fileSession = this.fileSession.map(
            ([fileName, session]): sessionObj => {
                const buildLog = buildLogErrors.find(([f]) => f === fileName);
                return [fileName, { ...session, errors: buildLog ? buildLog[1] : new AnnotationArray() }];
            },
        );
    }

    public removeFiles(...fileNames: string[]) {
        this.fileSession = _differenceWith(this.fileSession, fileNames, ([fileName], b) => fileName === b);
    }

    public setUnsaved(...fileNames: string[]) {
        this.fileSession = this.fileSession.map(
            ([fileName, session]): sessionObj => (fileNames.includes(fileName) ? [fileName, { ...session, unsavedChanges: true }] : [fileName, session]),
        );
    }

    public setSaved(...fileNames: string[]) {
        this.fileSession = this.fileSession.map(
            ([fileName, session]): sessionObj => (fileNames.includes(fileName) ? [fileName, { ...session, unsavedChanges: false }] : [fileName, session]),
        );
    }

    public getUnsavedFileNames() {
        return this.fileSession.filter(([, { unsavedChanges }]) => unsavedChanges).map(([fileName]) => fileName);
    }

    public getUnsavedFiles() {
        return this.fileSession.filter(([, { unsavedChanges }]) => unsavedChanges).map(([fileName, { code }]) => ({ fileName, fileContent: code }));
    }

    /**
     * Serialize the file session with all relevant attributes for persisting the file session.
     */
    public serialize() {
        return this.fileSession.reduce(
            (acc, [file, { errors }]) => ({
                ...acc,
                [file]: errors,
            }),
            {},
        );
    }
}
