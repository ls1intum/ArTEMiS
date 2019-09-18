import { LinkType, Orientation, UserInteractionEvent } from 'app/guided-tour/guided-tour.constants';
import { GuidedTour } from 'app/guided-tour/guided-tour.model';
import { ImageTourStep, TextLinkTourStep, TextTourStep } from 'app/guided-tour/guided-tour-step.model';
import { clickOnElement } from 'app/guided-tour/guided-tour.utils';

/**
 * This constant contains the guided tour configuration and steps for the course overview page
 */
export const courseOverviewTour: GuidedTour = {
    settingsKey: 'course_overview_tour',
    steps: [
        new ImageTourStep({
            headlineTranslateKey: 'tour.course-overview.welcome.headline',
            subHeadlineTranslateKey: 'tour.course-overview.welcome.subHeadline',
            contentTranslateKey: 'tour.course-overview.welcome.content',
            imageUrl: 'https://ase.in.tum.de/lehrstuhl_1/images/teaching/interactive/InteractiveLearning.png',
        }),
        new TextTourStep({
            highlightSelector: '#overview-menu',
            headlineTranslateKey: 'tour.course-overview.overview-menu.headline',
            contentTranslateKey: 'tour.course-overview.overview-menu.content',
            highlightPadding: 10,
            orientation: Orientation.BOTTOMLEFT,
        }),
        new TextTourStep({
            highlightSelector: '#course-admin-menu',
            headlineTranslateKey: 'tour.course-overview.course-admin-menu.headline',
            contentTranslateKey: 'tour.course-overview.course-admin-menu.content',
            orientation: Orientation.BOTTOMLEFT,
            highlightPadding: 10,
            permission: ['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'],
        }),
        new TextTourStep({
            highlightSelector: '#admin-menu',
            headlineTranslateKey: 'tour.course-overview.admin-menu.headline',
            contentTranslateKey: 'tour.course-overview.admin-menu.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
            permission: ['ROLE_ADMIN'],
        }),
        new TextTourStep({
            highlightSelector: '#notificationsNavBarDropdown',
            headlineTranslateKey: 'tour.course-overview.notification-menu.headline',
            contentTranslateKey: 'tour.course-overview.notification-menu.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
        }),
        new TextTourStep({
            highlightSelector: '#account-menu',
            headlineTranslateKey: 'tour.course-overview.account-menu.headline',
            contentTranslateKey: 'tour.course-overview.account-menu.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
            userInteractionEvent: UserInteractionEvent.CLICK,
        }),
        new TextTourStep({
            highlightSelector: '.nav-item .dropdown-menu.show',
            headlineTranslateKey: 'tour.course-overview.startTourOption.headline',
            contentTranslateKey: 'tour.course-overview.startTourOption.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
            closeAction: () => {
                clickOnElement('.dropdown-menu.show #account-menu');
            },
        }),
        new TextTourStep({
            highlightSelector: 'jhi-overview-course-card .card',
            headlineTranslateKey: 'tour.course-overview.course.headline',
            contentTranslateKey: 'tour.course-overview.course.content',
            orientation: Orientation.RIGHT,
        }),
        new TextTourStep({
            highlightSelector: '.card-footer',
            headlineTranslateKey: 'tour.course-overview.course-footer.headline',
            contentTranslateKey: 'tour.course-overview.course-footer.content',
            orientation: Orientation.RIGHT,
        }),
        new TextTourStep({
            highlightSelector: 'jhi-course-registration-selector button',
            headlineTranslateKey: 'tour.course-overview.register.headline',
            contentTranslateKey: 'tour.course-overview.register.content',
            orientation: Orientation.LEFT,
            highlightPadding: 10,
        }),
        new TextLinkTourStep({
            highlightSelector: '.footer .col-sm-6',
            headlineTranslateKey: 'tour.course-overview.contact.headline',
            contentTranslateKey: 'tour.course-overview.contact.content',
            externalUrlTranslateKey: 'tour.course-overview.contact.link',
            externalUrl: 'https://github.com/ls1intum/ArTEMiS',
            linkType: LinkType.BUTTON,
            orientation: Orientation.TOPLEFT,
        }),
    ],
};
