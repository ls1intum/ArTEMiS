import { ActiveFeatureToggles } from 'app/shared/feature-toggle/feature-toggle.service';
import { GuidedTourMapping } from 'app/guided-tour/guided-tour-setting.model';
import { ProgrammingLanguageFeature } from 'app/exercises/programming/shared/service/programming-language-feature/programming-language-feature.service';

export class ProfileInfo {
    public activeProfiles: string[];
    public ribbonEnv: string;
    public inProduction: boolean;
    public openApiEnabled?: boolean;
    public sentry?: { dsn: string };
    public features: ActiveFeatureToggles;
    public guidedTourMapping?: GuidedTourMapping;
    public buildPlanURLTemplate: string;
    public sshCloneURLTemplate: string;
    public sshKeysURL: string;
    public externalUserManagementURL: string;
    public externalUserManagementName: string;
    public imprint: string;
    public contact: string;
    public testServer: boolean;
    public allowedMinimumOrionVersion: string;
    public registrationEnabled?: boolean;
    public allowedEmailPattern?: string;
    public allowedEmailPatternReadable?: string;
    public allowedLdapUsernamePattern?: string;
    public allowedCourseRegistrationUsernamePattern?: string;
    public accountName?: string;
    public versionControlUrl?: string;
    public programmingLanguageFeatures: ProgrammingLanguageFeature[];
}
