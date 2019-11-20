import { Orientation, UserInteractionEvent } from 'app/guided-tour/guided-tour.constants';
import { GuidedTour } from 'app/guided-tour/guided-tour.model';
import { ImageTourStep, TextTourStep } from 'app/guided-tour/guided-tour-step.model';

/**
 * This constant contains the guided tour configuration and steps for the course overview page
 */
export const courseOverviewTour: GuidedTour = {
    courseShortName: 'artemistutorial',
    exerciseShortName: '',
    settingsKey: 'course_overview_tour',
    steps: [
        new ImageTourStep({
            headlineTranslateKey: 'tour.courseOverview.welcome.headline',
            subHeadlineTranslateKey: 'tour.courseOverview.welcome.subHeadline',
            contentTranslateKey: 'tour.courseOverview.welcome.content',
            imageUrl: 'https://ase.in.tum.de/lehrstuhl_1/images/teaching/interactive/InteractiveLearning.png',
        }),
        new TextTourStep({
            highlightSelector: '#overview-menu',
            headlineTranslateKey: 'tour.courseOverview.overviewMenu.headline',
            contentTranslateKey: 'tour.courseOverview.overviewMenu.content',
            highlightPadding: 10,
            orientation: Orientation.BOTTOM,
        }),
        new TextTourStep({
            highlightSelector: '#course-admin-menu',
            headlineTranslateKey: 'tour.courseOverview.courseAdminMenu.headline',
            contentTranslateKey: 'tour.courseOverview.courseAdminMenu.content',
            orientation: Orientation.BOTTOMRIGHT,
            highlightPadding: 10,
            permission: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
        }),
        new TextTourStep({
            highlightSelector: '#admin-menu',
            headlineTranslateKey: 'tour.courseOverview.adminMenu.headline',
            contentTranslateKey: 'tour.courseOverview.adminMenu.content',
            orientation: Orientation.BOTTOMRIGHT,
            highlightPadding: 10,
            permission: ['ROLE_ADMIN'],
        }),
        new TextTourStep({
            highlightSelector: '#notificationsNavBarDropdown',
            headlineTranslateKey: 'tour.courseOverview.notificationMenu.headline',
            contentTranslateKey: 'tour.courseOverview.notificationMenu.content',
            orientation: Orientation.BOTTOMRIGHT,
            highlightPadding: 10,
        }),
        new TextTourStep({
            highlightSelector: '#account-menu',
            headlineTranslateKey: 'tour.courseOverview.accountMenuClick.headline',
            contentTranslateKey: 'tour.courseOverview.accountMenuClick.content',
            orientation: Orientation.BOTTOMRIGHT,
            highlightPadding: 10,
            userInteractionEvent: UserInteractionEvent.CLICK,
            disableStep: true,
        }),
        new TextTourStep({
            highlightSelector: '#account-menu',
            headlineTranslateKey: 'tour.courseOverview.accountMenu.headline',
            contentTranslateKey: 'tour.courseOverview.accountMenu.content',
            orientation: Orientation.BOTTOMRIGHT,
            highlightPadding: 10,
        }),
        new TextTourStep({
            highlightSelector: '.card.guided-tour',
            headlineTranslateKey: 'tour.courseOverview.course.headline',
            contentTranslateKey: 'tour.courseOverview.course.content',
            orientation: Orientation.RIGHT,
        }),
        new TextTourStep({
            highlightSelector: '.guided-tour .card-footer',
            headlineTranslateKey: 'tour.courseOverview.courseFooter.headline',
            contentTranslateKey: 'tour.courseOverview.courseFooter.content',
            orientation: Orientation.TOPLEFT,
        }),
        // disabled
        new TextTourStep({
            highlightSelector: '.card.guided-tour',
            clickEventListenerSelector: 'body',
            headlineTranslateKey: 'tour.courseOverview.courseClick.headline',
            contentTranslateKey: 'tour.courseOverview.courseClick.content',
            orientation: Orientation.RIGHT,
            userInteractionEvent: UserInteractionEvent.CLICK,
            disableStep: true,
        }),
        // disabled
        new TextTourStep({
            highlightSelector: 'jhi-course-registration-selector button',
            headlineTranslateKey: 'tour.courseOverview.register.headline',
            contentTranslateKey: 'tour.courseOverview.register.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
            disableStep: true,
        }),
        new TextTourStep({
            highlightSelector: '.footer .col-sm-6',
            headlineTranslateKey: 'tour.courseOverview.contact.headline',
            contentTranslateKey: 'tour.courseOverview.contact.content',
            hintTranslateKey: 'tour.courseOverview.contact.hint',
            highlightPadding: 5,
            orientation: Orientation.TOPLEFT,
        }),
    ],
};
