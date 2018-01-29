(function () {
    'use strict';

    angular
        .module('artemisApp')
        .controller('QuizExerciseController', QuizExerciseController);

    QuizExerciseController.$inject = ['Principal', 'QuizExercise', 'courseEntity', 'CourseQuizExercises'];

    function QuizExerciseController(Principal, QuizExercise, courseEntity, CourseQuizExercises) {

        var vm = this;

        vm.quizExercises = [];
        vm.sort = sort;
        vm.predicate = 'id';
        vm.reverse = true;
        vm.course = courseEntity;
        vm.statusForQuiz = statusForQuiz;
        vm.fullMinutesForSeconds = fullMinutesForSeconds;
        vm.userIsInstructor = userIsInstructor;
        vm.quizIsOver = quizIsOver;
        vm.startQuiz = startQuiz;
        vm.showQuiz = showQuiz;
        vm.openForPractice = openForPractice;

        /**
         * Display texts for the possible quiz states. Can be used like an enum.
         *
         * @type {{HIDDEN: string, VISIBLE: string, ACTIVE: string, CLOSED: string, OPEN_FOR_PRACTICE: string}}
         */
        vm.QuizStatus = {
            HIDDEN: "Hidden",
            VISIBLE: "Visible",
            ACTIVE: "Active",
            CLOSED: "Closed",
            OPEN_FOR_PRACTICE: "Open for Practice"
        };

        function load() {
            if (vm.course) {
                loadForCourse(vm.course);
            } else {
                loadAll();
            }
        }

        load();

        function loadAll() {
            QuizExercise.query(function (result) {
                vm.quizExercises = result;
                vm.searchQuery = null;
            });
        }

        function loadForCourse(course) {
            CourseQuizExercises.query({
                courseId: course.id
            }, function (result) {
                vm.quizExercises = result;
                vm.searchQuery = null;
            });
        }

        function sort() {
            vm.quizExercises.sort(function (a, b) {
                var result = (a[vm.predicate] < b[vm.predicate]) ? -1 : (a[vm.predicate] > b[vm.predicate]) ? 1 : (
                    (a.id < b.id) ? -1 : (a.id > b.id) ? 1 : 0
                );
                return result * (vm.reverse ? -1 : 1);
            });
        }

        /**
         * Method for determining the current status of a quiz exercise
         * @param quizExercise The quiz exercise we want to determine the status of
         * @returns {string} The status as a string
         */
        function statusForQuiz(quizExercise) {
            if (quizExercise.isPlannedToStart && quizExercise.remainingTime != null) {
                if (quizExercise.remainingTime <= 0) {
                    // the quiz is over
                    return quizExercise.isOpenForPractice ? vm.QuizStatus.OPEN_FOR_PRACTICE : vm.QuizStatus.CLOSED;
                } else {
                    return vm.QuizStatus.ACTIVE;
                }
            }
            // the quiz hasn't started yet
            return quizExercise.isVisibleBeforeStart ? vm.QuizStatus.VISIBLE : vm.QuizStatus.HIDDEN;
        }

        /**
         * Start the given quiz-exercise immediately
         *
         * @param quizExercise {object} the quiz exercise to start
         */
        function startQuiz(quizExercise) {
            QuizExercise.start({
                id: quizExercise.id
            }, {}).$promise.then(
                function () {
                    // success
                    load();
                },
                function (error) {
                    // error
                    alert(error && error.data && error.data.message);
                    load();
                }
            );
        }

        /**
         * Make the given quiz-exercise visible to students
         *
         * @param quizExercise {object} the quiz exercise to make visible
         */
        function showQuiz(quizExercise) {
            QuizExercise.setVisible({
                id: quizExercise.id
            }, {}).$promise.then(
                function () {
                    // success
                    load();
                },
                function (error) {
                    // error
                    alert(error && error.data && error.data.message);
                    load();
                }
            );
        }

        /**
         * Set the quiz open for practice
         *
         * @param quizExercise {object} the quiz exercise to release for practice
         */
        function openForPractice(quizExercise) {
            QuizExercise.openForPractice({
                id: quizExercise.id
            }, {}).$promise.then(
                function () {
                    // success
                    load();
                },
                function (error) {
                    // error
                    alert(error && error.data && error.data.message);
                    load();
                }
            );
        }

        /**
         * Convert seconds to full minutes
         * @param seconds {number} the number of seconds
         * @returns {number} the number of full minutes
         */
        function fullMinutesForSeconds(seconds) {
            return Math.floor(seconds / 60);
        }

        /**
         * Checks if the User is Admin/Instructor or Teaching Assistant
         * @returns {boolean} true if the User is an Admin/Instructor, false if not.
         */
        function userIsInstructor() {
            return Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR']);
        }

        /**
         * Checks if the quiz exercise is over
         * @param quizExercise The quiz exercise we want to know if it's over
         * @returns {boolean} true if the quiz exercise is over, false if not.
         */
        function quizIsOver(quizExercise) {
            if (quizExercise.isPlannedToStart) {
                var plannedEndMoment = moment(quizExercise.releaseDate).add(quizExercise.duration, "seconds");
                return plannedEndMoment.isBefore(moment());
                    // the quiz is over
            }
            // the quiz hasn't started yet
            return false;
        }
    }
})();
