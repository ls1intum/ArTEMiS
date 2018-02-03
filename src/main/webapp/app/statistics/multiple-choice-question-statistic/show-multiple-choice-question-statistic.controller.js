(function() {
    'use strict';

    angular
        .module('artemisApp')
        .controller('ShowMultipleChoiceQuestionStatisticController', ShowMultipleChoiceQuestionStatisticController);

    ShowMultipleChoiceQuestionStatisticController.$inject = ['$translate','$scope', '$state', 'Principal', 'JhiWebsocketService', 'QuizExercise', 'QuizExerciseForStudent' , 'MultipleChoiceQuestionStatistic', 'MultipleChoiceQuestionStatisticForStudent', 'ArtemisMarkdown'];

    function ShowMultipleChoiceQuestionStatisticController ($translate, $scope, $state, Principal, JhiWebsocketService, QuizExercise, QuizExerciseForStudent, MultipleChoiceQuestionStatistic, MultipleChoiceQuestionStatisticForStudent, ArtemisMarkdown) {

        var vm = this;

        // Variables for the chart:
        vm.labels = [];
        vm.data = [];
        vm.colors = [];

        var label;
        var solutionLabel;
        var ratedData;
        var unratedData;
        var backgroundColor;
        var backgroundSolutionColor;
        var ratedCorrectData;
        var unratedCorrectData;

        vm.switchSolution = switchSolution;
        vm.switchRated = switchRated;
        vm.nextStatistic = nextStatistic;
        vm.previousStatistic = previousStatistic;
        vm.releaseStatistics = releaseStatistics;
        vm.releaseButtonDisabled = releaseButtonDisabled;

        vm.showSolution = false;
        vm.rated = true;

        vm.$onInit = init;

        /**
         * loads quizExercise with the selected multipleChoiceQuestionStatistic from server and sets up socket connections
         */
        function init() {
            // use different REST-call if the User is a Student
            if(Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) {
                QuizExercise.get({id: _.get($state,"params.quizId")}).$promise.then(loadQuiz);
            }
            else{
                QuizExerciseForStudent.get({id: _.get($state,"params.quizId")}).$promise.then(loadQuiz);
            }
            //subscribe websocket for new statistical data
            var websocketChannelForData = '/topic/statistic/'+ _.get($state,"params.quizId");
            JhiWebsocketService.subscribe(websocketChannelForData);

            //subscribe websocket which notifies the user if the release status was changed
            var websocketChannelForReleaseState = websocketChannelForData + '/release';
            JhiWebsocketService.subscribe(websocketChannelForReleaseState);

            // ask for new Data if the websocket for new statistical data was notified
            JhiWebsocketService.receive(websocketChannelForData).then(null, null, function(notify) {
                if(Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) {
                    MultipleChoiceQuestionStatistic.get({id: vm.questionStatistic.id}).$promise.then(loadNewData);
                }
                else{
                    MultipleChoiceQuestionStatisticForStudent.get({id: vm.questionStatistic.id}).$promise.then(loadNewData);
                }

            });
            // refresh release information
            JhiWebsocketService.receive(websocketChannelForReleaseState).then(null, null, function(payload) {
                vm.quizExercise.quizPointStatistic.released = payload;
                vm.questionStatistic.released = payload;
                // send students back to courses if the statistic was revoked
                if(!Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA']) && !payload) {
                    $state.go('courses');
                }
            });

            $scope.$on('$destroy', function() {
                JhiWebsocketService.unsubscribe(websocketChannelForData);
                JhiWebsocketService.unsubscribe(websocketChannelForReleaseState);
            });

            // add Axes-labels based on selected language
            $translate('showStatistic.multipleChoiceQuestionStatistic.xAxes').then(function (xLabel) {
                vm.options.scales.xAxes[0].scaleLabel.labelString = xLabel;
            });
            $translate('showStatistic.multipleChoiceQuestionStatistic.yAxes').then(function (yLabel) {
                vm.options.scales.yAxes[0].scaleLabel.labelString = yLabel;
            });
        }

        /**
         * This functions loads the Quiz, which is necessary to build the Web-Template
         *
         * @param {QuizExercise} quiz: the quizExercise, which the selected question is part of.
         */
        function loadQuiz(quiz) {
            // if the Student finds a way to the Website, while the Statistic is not released -> the Student will be send back to Courses
            if( (!Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) && quiz.quizPointStatistic.released == false) {
                $state.go('courses');
            }
            //search selected question in quizExercise based on questionId
            vm.quizExercise = quiz;
            vm.question = null;
            for(var i = 0; vm.question === null && i < vm.quizExercise.questions.length; i++) {
                if (_.get($state,"params.questionId") == vm.quizExercise.questions[i].id) {
                    vm.question = vm.quizExercise.questions[i];
                }
            }
            // if the Anyone finds a way to the Website, with an wrong combination of QuizId and QuestionId -> go back to Courses
            if(vm.question === null) {
                $state.go('courses');
            }
            //render Markdown-text
            vm.questionTextRendered = ArtemisMarkdown.htmlForMarkdown(vm.question.text);
            vm.answerTextRendered = vm.question.answerOptions.map( function (answer) {
                return ArtemisMarkdown.htmlForMarkdown(answer.text);
            });

            vm.questionStatistic = vm.question.questionStatistic;
            loadLayout();
            loadData();
        }

        /**
         * load the new multipleChoiceQuestionStatistic from the server if the Websocket has been notified
         *
         * @param {MultipleChoiceQuestionStatistic} statistic: the new multipleChoiceQuestionStatistic from the server with the new Data.
         */
        function loadNewData(statistic) {
            // if the Student finds a way to the Website, while the Statistic is not released -> the Student will be send back to Courses
            if( (!Principal.hasAnyAuthority(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) && quiz.quizPointStatistic.released == false) {
                $state.go('courses');
            }
            vm.questionStatistic = statistic;
            loadData();
        }

        /**
         * build the Chart-Layout based on the the Json-entity (questionStatistic)
         */
        function loadLayout(){

            // reset old data
            label = new Array(vm.question.answerOptions.length + 1);
            backgroundColor = [];
            backgroundSolutionColor = new Array(vm.question.answerOptions.length + 1);
            solutionLabel = new Array(vm.question.answerOptions.length + 1);

            //set label and backgroundcolor based on the AnswerOptions
            for(var i = 0; i < vm.question.answerOptions.length; i++) {
                label[i] = (String.fromCharCode(65 + i) + ".");
                backgroundColor.push(
                    {backgroundColor: "#428bca",
                        borderColor: "#428bca",
                        pointBackgroundColor: "#428bca",
                        pointBorderColor: "#428bca"
                    });
            }
            backgroundColor.push(
                {backgroundColor: "#5bc0de",
                    borderColor: "#5bc0de",
                    pointBackgroundColor: "#5bc0de",
                    pointBorderColor: "#5bc0de"
                });
            backgroundSolutionColor[vm.question.answerOptions.length] =
                {backgroundColor: "#5bc0de",
                    borderColor: "#5bc0de",
                    pointBackgroundColor: "#5bc0de",
                    pointBorderColor: "#5bc0de"
                };

            //add Text for last label based on the language
            $translate('showStatistic.quizStatistic.yAxes').then(function (lastLabel) {
                solutionLabel[vm.question.answerOptions.length] = (lastLabel.split(" "));
                label[vm.question.answerOptions.length] = (lastLabel.split(" "));
                vm.labels = label;
            });

            //set Background for invalid answers = grey
            $translate('showStatistic.invalid').then(function (invalidLabel) {
                for (var j = 0; j < vm.question.answerOptions.length; j++) {
                    if (vm.question.answerOptions[j].invalid) {
                        backgroundColor[j] = (
                            {backgroundColor: "#838383",
                                borderColor: "#838383",
                                pointBackgroundColor: "#838383",
                                pointBorderColor: "#838383"
                            });
                        backgroundSolutionColor[j] = (
                            {backgroundColor: "#838383",
                                borderColor: "#838383",
                                pointBackgroundColor: "#838383",
                                pointBorderColor: "#838383"
                            });

                        solutionLabel[j] = ([String.fromCharCode(65 + i) + ".", " " + invalidLabel]);
                    }
                }
            });

            //add correct-text to the label based on the language
            $translate('showStatistic.multipleChoiceQuestionStatistic.correct').then(function (correctLabel) {
                for(var i = 0; i < vm.question.answerOptions.length; i++) {
                    if (vm.question.answerOptions[i].isCorrect) {
                        // check if the answer is valid and if true change solution-label and -color
                        if (!vm.question.answerOptions[i].invalid) {
                            backgroundSolutionColor[i] = (
                                {backgroundColor: "#5cb85c",
                                    borderColor: "#5cb85c",
                                    pointBackgroundColor: "#5cb85c",
                                    pointBorderColor: "#5cb85c"
                                });
                            solutionLabel[i] = ([String.fromCharCode(65 + i) + ".", " (" + correctLabel + ")"]);
                        }
                    }
                }
            });

            //add incorrect-text to the label based on the language
            $translate('showStatistic.multipleChoiceQuestionStatistic.incorrect').then(function (incorrectLabel) {
                for(var i = 0; i < vm.question.answerOptions.length; i++) {
                    if (!vm.question.answerOptions[i].isCorrect) {
                        // check if the answer is valid and if true change solution-label and -color
                        if (!vm.question.answerOptions[i].invalid) {
                            backgroundSolutionColor[i] = (
                                {backgroundColor: "#d9534f",
                                    borderColor: "#d9534f",
                                    pointBackgroundColor: "#d9534f",
                                    pointBorderColor: "#d9534f"
                                });
                            solutionLabel[i] = ([String.fromCharCode(65 + i) + ".", " (" + incorrectLabel + ")"]);
                        }
                    }
                }
            });
        }

        /**
         * load the Data from the Json-entity to the chart: myChart
         */
        function loadData() {

            // reset old data
            ratedData = [];
            unratedData = [];

            //set data based on the answerCounters for each AnswerOption
            for(var i = 0; i < vm.question.answerOptions.length; i++) {
                for(var j = 0; j < vm.questionStatistic.answerCounters.length; j++) {
                    if (vm.question.answerOptions[i].id === (vm.questionStatistic.answerCounters[j].answer.id)) {
                        ratedData.push(vm.questionStatistic.answerCounters[j].ratedCounter);
                        unratedData.push(vm.questionStatistic.answerCounters[j].unRatedCounter);
                    }
                }
            }
            //add data for the last bar (correct Solutions)
            ratedCorrectData = vm.questionStatistic.ratedCorrectCounter;
            unratedCorrectData = vm.questionStatistic.unRatedCorrectCounter;

            // if show Solution is true use the label, backgroundColor and Data, which show the solution
            if(vm.showSolution) {
                // show Solution
                vm.labels = solutionLabel;
                // if show Solution is true use the backgroundColor which shows the solution
                vm.colors = backgroundSolutionColor;
                if (vm.rated) {
                    vm.participants = vm.questionStatistic.participantsRated;
                    // if rated is true use the rated Data and add the rated CorrectCounter
                    vm.data = ratedData.slice(0);
                    vm.data.push(ratedCorrectData);
                }
                else {
                    vm.participants = vm.questionStatistic.participantsUnrated;
                    // if rated is false use the unrated Data and add the unrated CorrectCounter
                    vm.data = unratedData.slice(0);
                    vm.data.push(unratedCorrectData);
                }
            }
            else {
                // don't show Solution
                vm.labels = label;
                // if show Solution is false use the backgroundColor which doesn't show the solution
                vm.colors = backgroundColor;
                // if rated is true use the rated Data
                if (vm.rated) {
                    vm.participants = vm.questionStatistic.participantsRated;
                    vm.data = ratedData;
                }
                // if rated is false use the unrated Data
                else {
                    vm.participants = vm.questionStatistic.participantsUnrated;
                    vm.data = unratedData;
                }
            }
        }

        /**
         * switch between showing and hiding the solution in the chart
         *  1. change the amount of  participants
         *  2. change the bar-Data
         */
        function switchRated() {
            if(vm.rated) {
                //load unrated Data
                vm.participants = vm.questionStatistic.participantsUnrated;
                // if show Solution is true use the backgroundColor which shows the solution
                if(vm.showSolution) {
                    vm.data = unratedData.slice(0);
                    vm.data.push(unratedCorrectData);
                }else{
                    vm.data = unratedData;
                }
                vm.rated = false;
            }
            else{
                //load rated Data
                vm.participants = vm.questionStatistic.participantsRated;
                // if show Solution is true use the backgroundColor which shows the solution
                if(vm.showSolution) {
                    vm.data = ratedData.slice(0);
                    vm.data.push(ratedCorrectData);
                }else{
                    vm.data = ratedData;
                }
                vm.rated = true;
            }
        }

        /**
         * switch between showing and hiding the solution in the chart
         *  1. change the BackgroundColor of the bars
         *  2. change the bar-Labels
          */
        function switchSolution() {
            if(vm.showSolution) {
                // don't show Solution
                vm.labels = label;
                // if show Solution is false use the backgroundColor which doesn't show the solution
                vm.colors = backgroundColor;
                // if rated is true use the rated Data
                if (vm.rated) {
                    vm.data = ratedData;
                }
                // if rated is false use the unrated Data
                else {
                    vm.data = unratedData;
                }
                vm.showSolution = false;
            }
            else {
                // show Solution
                vm.labels = solutionLabel;
                // if show Solution is true use the backgroundColor which shows the solution
                vm.colors = backgroundSolutionColor;
                if (vm.rated) {
                    // if rated is true use the rated Data and add the rated CorrectCounter
                    vm.data = ratedData.slice(0);
                    vm.data.push(ratedCorrectData);
                }
                else {
                    // if rated is false use the unrated Data and add the unrated CorrectCounter
                    vm.data = unratedData.slice(0);
                    vm.data.push(unratedCorrectData);
                }
                vm.showSolution = true;
            }
        }

        /**
         * got to the Template with the previous Statistic
         * if first QuestionStatistic -> go to the Quiz-Statistic
         */
        function previousStatistic() {
            if(vm.quizExercise.questions[0].id === vm.question.id) {
            $state.go('quiz-statistic-chart',{quizId: vm.quizExercise.id});
        }
        else{
            for (var i = 0; i < vm.quizExercise.questions.length; i++) {
                if(vm.quizExercise.questions[i].id === vm.question.id) {
                    if(vm.quizExercise.questions[i - 1].type === "multiple-choice") {
                        $state.go('multiple-choice-question-statistic-chart', {
                            quizId: vm.quizExercise.id,
                            questionId: vm.quizExercise.questions[i - 1].id
                        });
                    }
                    if (vm.quizExercise.questions[i - 1].type === "drag-and-drop") {
                        $state.go('drag-and-drop-question-statistic-chart', {
                            quizId: vm.quizExercise.id,
                            questionId: vm.quizExercise.questions[i - 1].id
                        });
                    }
                }
            }
        }

        }

        /**
         * got to the Template with the next Statistic
         * if last QuestionStatistic -> go to the Quiz-Point-Statistic
         */
        function nextStatistic() {
            if (vm.quizExercise.questions[vm.quizExercise.questions.length - 1].id === vm.question.id) {
                $state.go('quiz-point-statistic-chart', {quizId: vm.quizExercise.id});
            }
            else {
                for (var i = 0; i < vm.quizExercise.questions.length; i++) {
                    if (vm.quizExercise.questions[i].id === vm.question.id) {
                        if (vm.quizExercise.questions[i + 1].type === "multiple-choice") {
                            $state.go('multiple-choice-question-statistic-chart', {
                                quizId: vm.quizExercise.id,
                                questionId: vm.quizExercise.questions[i + 1].id
                            });
                        }
                        if (vm.quizExercise.questions[i + 1].type === "drag-and-drop") {
                            $state.go('drag-and-drop-question-statistic-chart', {
                                quizId: vm.quizExercise.id,
                                questionId: vm.quizExercise.questions[i + 1].id
                            });
                        }
                    }
                }
            }
        }

        /**
         * release of revoke all statistics of the quizExercise
         *
         * @param {boolean} released: true to release, false to revoke
         */
        function releaseStatistics(released) {
            if (released === vm.quizExercise.quizPointStatistic.released ) {
                return;
            }
            // check if it's allowed to release the statistics, if not send alert and do nothing
            if (released && releaseButtonDisabled()) {
                alert("Quiz hasn't ended yet!");
                return;
            }
            if (vm.quizExercise.id) {
                vm.quizExercise.quizPointStatistic.released = released;
                if (released) {
                    QuizExercise.releaseStatistics({id: vm.quizExercise.id}, {}, function(){}, function () {alert("Error!");})
                } else {
                    QuizExercise.revokeStatistics({id: vm.quizExercise.id}, {});
                }
            }
        }

        /**
         * check if it's allowed to release the Statistic (allowed if the quiz is finished)
         * @returns {boolean} true if it's allowed, false if not
         */
        function releaseButtonDisabled() {
            if (vm.quizExercise != null) {
                return (!vm.quizExercise.isPlannedToStart || moment().isBefore(vm.quizExercise.dueDate));
            }else{
                return true;
            }
        }

        // options for chart in chart.js style
        vm.options= {
            layout: {
                padding: {
                    left: 0,
                    right: 0,
                    top: 0,
                    bottom: 30
                }
            },
            legend: {
                display: false
            },
            title: {
                display: false,
                text: "",
                position: "top",
                fontSize: "16",
                padding: 20
            },
            tooltips: {
                enabled: false
            },
            scales: {
                yAxes: [{
                    scaleLabel: {
                        labelString: '',
                        display: true
                    },
                    ticks: {
                        beginAtZero: true
                    }
                }],
                xAxes: [{
                    scaleLabel: {
                        labelString: '',
                        display: true
                    }
                }]
            },
            hover: {animationDuration: 0},
            //add numbers on top of the bars
            animation: {
                duration: 500,
                onComplete: function () {
                    var chartInstance = this.chart,
                        ctx = chartInstance.ctx;
                    var fontSize = 12;
                    var fontStyle = 'normal';
                    var fontFamily = 'Calibri';
                    ctx.font = Chart.helpers.fontString(fontSize, fontStyle, fontFamily);
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';

                    this.data.datasets.forEach(function (dataset, i) {
                        var meta = chartInstance.controller.getDatasetMeta(i);
                        meta.data.forEach(function (bar, index) {
                            var data = (Math.round(dataset.data[index] * 100) / 100);
                            var dataPercentage = (Math.round((dataset.data[index] / vm.participants) * 1000) / 10);

                            var position = bar.tooltipPosition();

                            //if the bar is high enough -> write the percentageValue inside the bar
                            if (dataPercentage > 6) {
                                //if the bar is low enough -> write the amountValue above the bar
                                if (position.y > 15) {
                                    ctx.fillStyle = 'black';
                                    ctx.fillText(data, position.x, position.y - 10);


                                    if (vm.participants !== 0) {
                                        ctx.fillStyle = 'white';
                                        ctx.fillText(dataPercentage.toString() + "%", position.x, position.y + 10);
                                    }
                                }
                                //if the bar is too high -> write the amountValue inside the bar
                                else {
                                    ctx.fillStyle = 'white';
                                    if (vm.participants !== 0) {
                                        ctx.fillText(data + " / " + dataPercentage.toString() + "%", position.x, position.y + 10);
                                    } else {
                                        ctx.fillText(data, position.x, position.y + 10);
                                    }
                                }
                            }
                            //if the bar is to low -> write the percentageValue above the bar
                            else {
                                ctx.fillStyle = 'black';
                                if (vm.participants !== 0) {
                                    ctx.fillText(data + " / " + dataPercentage.toString() + "%", position.x, position.y - 10);
                                } else {
                                    ctx.fillText(data, position.x, position.y - 10);
                                }
                            }
                        });
                    });
                }
            }
        }
    }
})();
