(function() {
    'use strict';

    angular
        .module('artemisApp')
        .controller('ShowMultipleChoiceQuestionStatisticController', ShowMultipleChoiceQuestionStatisticController);

    ShowMultipleChoiceQuestionStatisticController.$inject = ['$translate', '$rootScope','$scope', '$state', 'Principal', 'JhiWebsocketService', 'QuizExercise', 'MultipleChoiceQuestion', 'MultipleChoiceQuestionStatistic'];

    function ShowMultipleChoiceQuestionStatisticController ($translate, rootScope, $scope, $state, Principal, JhiWebsocketService, QuizExercise, MultipleChoiceQuestion, MultipleChoiceQuestionStatistic) {

        var vm = this;

        // Variables for the chart:
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


        vm.showSolution = false;
        vm.rated = true;
        vm.$onInit = init;

        function init(){
            QuizExercise.get({id: _.get($state,"params.quizId")}).$promise.then(loadQuestion);

            var websocketChannel = '/topic/statistic/'+ "params.quizId";

            JhiWebsocketService.subscribe(websocketChannel);

            JhiWebsocketService.receive(websocketChannel).then(null, null, function(notify) {
                MultipleChoiceQuestionStatistic.get({id: _.get($state, vm.question.questionStatistic.id)}).$promise.then(loadNewData);
            });

            $scope.$on('$destroy', function() {
                JhiWebsocketService.unsubscribe(websocketChannel);
            });

            $translate('showStatistic.multipleChoiceQuestionStatistic.xAxes').then(function (xLabel){
                window.myChart.options.scales.xAxes[0].scaleLabel.labelString = xLabel;
            });
            $translate('showStatistic.multipleChoiceQuestionStatistic.yAxes').then(function (yLabel){
                window.myChart.options.scales.yAxes[0].scaleLabel.labelString = yLabel;
            });
        }

        function loadQuestion(quiz) {
            vm.quizExercise = quiz;
            MultipleChoiceQuestion.get({id: _.get($state,"params.questionId")}).$promise.then(loadQuestionSuccess);
        }

        function loadQuestionSuccess(question){
            vm.question = question;
            vm.questionStatistic = vm.question.questionStatistic;
            loadData();
        }

        function loadNewData(statistic){
            vm.questionStatistic = statistic;
            loadData();
        }


        function loadData() {

            label = [];
            backgroundColor = [];
            backgroundSolutionColor = [];
            ratedData = [];
            unratedData = [];
            solutionLabel = [];

            for(var i = 0; i < vm.question.answerOptions.length; i++){
                label.push(String.fromCharCode(65 + i));
                backgroundColor.push("#428bca");
                for(var j = 0; j < vm.questionStatistic.answerCounters.length; j++){
                    if (vm.question.answerOptions[i].id === (vm.questionStatistic.answerCounters[j].answer.id)){
                        ratedData.push(vm.questionStatistic.answerCounters[j].ratedCounter);
                        unratedData.push(vm.questionStatistic.answerCounters[j].unRatedCounter);
                        if(vm.questionStatistic.answerCounters[j].answer.isCorrect){
                            backgroundSolutionColor.push("#5cb85c");
                            solutionLabel.push([label[i]," (richtig)"]);
                        }else{
                            backgroundSolutionColor.push("#d9534f");
                            solutionLabel.push([label[i]," (falsch)"]);
                        }
                    }
                }
            }

            label.push(["Richte", " Lösungen"]);
            ratedCorrectData = vm.questionStatistic.ratedCorrectCounter;
            unratedCorrectData = vm.questionStatistic.unRatedCorrectCounter;
            backgroundColor.push("#5bc0de");
            backgroundSolutionColor.push("#5bc0de");
            solutionLabel.push(["Richte", " Lösungen"]);


            if (vm.rated) {
                vm.participants = vm.questionStatistic.participantsRated;
                barChartData.participants = vm.questionStatistic.participantsRated;
                barChartData.datasets.forEach(function (dataset) {
                    dataset.data = ratedData.slice(0);
                    if(vm.showSolution){
                        dataset.backgroundColor = backgroundSolutionColor;
                        //if(dataset.data.length == ratedData.length)
                            dataset.data.push(ratedCorrectData);

                    }else{
                        dataset.backgroundColor = backgroundColor;
                    }
                });
            }
            else {
                vm.participants = vm.questionStatistic.participantsUnrated;
                barChartData.participants = vm.questionStatistic.participantsRated;
                barChartData.datasets.forEach(function (dataset) {
                    dataset.data = unratedData.slice(0);
                    if(vm.showSolution){
                        dataset.backgroundColor = backgroundSolutionColor;
                        //if(dataset.data.length == unratedData.length)
                        dataset.data.push(unratedCorrectData);
                    }else{
                        dataset.backgroundColor = backgroundColor;
                    }
                });
            }
            if(vm.showSolution){
                barChartData.labels = solutionLabel;

            }else{
                barChartData.labels = label;
            }
            window.myChart.update();

        }

        function switchRated(){
            if(vm.rated) {
                barChartData.datasets.forEach(function (dataset) {
                    dataset.data = unratedData.slice(0);
                    if(vm.showSolution){
                        dataset.backgroundColor = backgroundSolutionColor;
                        //if(dataset.data.length == unratedData.length)
                        dataset.data.push(unratedCorrectData);
                    }else{
                        dataset.backgroundColor = backgroundColor;
                    }
                });
                vm.participants = vm.questionStatistic.participantsUnrated;
                barChartData.participants = vm.questionStatistic.participantsUnrated;
                vm.rated = false;
            }
            else{
                barChartData.datasets.forEach(function (dataset) {
                    dataset.data = ratedData.slice(0);
                    if(vm.showSolution){
                        dataset.backgroundColor = backgroundSolutionColor;
                        //if(dataset.data.length == ratedData.length)
                        dataset.data.push(ratedCorrectData);
                    }else{
                        dataset.backgroundColor = backgroundColor;
                    }
                });
                vm.participants = vm.questionStatistic.participantsRated;
                barChartData.participants = vm.questionStatistic.participantsRated;
                vm.rated = true;
            }
            window.myChart.update();
        }

        function switchSolution(){
            if(vm.showSolution){
                barChartData.datasets.forEach(function (dataset) {
                    if (vm.rated) {
                        dataset.data = ratedData.slice(0);
                    } else {
                        dataset.data = unratedData.slice(0);
                    }
                    dataset.backgroundColor = backgroundColor;
                });
                barChartData.labels = label;
                vm.showSolution = false;
            }
            else {
                barChartData.datasets.forEach(function (dataset) {
                    if (vm.rated) {
                        dataset.data = ratedData.slice(0);
                        //if(dataset.data.length == ratedData.length)
                        dataset.data.push(ratedCorrectData);
                    }
                    else {
                        dataset.data = unratedData.slice(0);
                        //if(dataset.data.length == unratedData.length)
                        dataset.data.push(unratedCorrectData);
                    }
                    dataset.backgroundColor = backgroundSolutionColor;
                });
                barChartData.labels = solutionLabel;
                vm.showSolution = true;
            }
            window.myChart.update();
        }

        function previousStatistic() {
            if(vm.quizExercise.questions[0].id === vm.question.id){
            $state.go('quiz-statistic-chart',{quizId: vm.quizExercise.id});
        }
        else{
            for (var i = 0; i < vm.quizExercise.questions.length; i++){
                if(vm.quizExercise.questions[i].id === vm.question.id){
                    $state.go('multiple-choice-question-statistic-chart', {quizId: vm.quizExercise.id, questionId: vm.quizExercise.questions[i-1].id});
                }
            }
        }

        }
        function nextStatistic() {
            if(vm.quizExercise.questions[vm.quizExercise.questions.length - 1].id === vm.question.id){
                $state.go('quiz-point-statistic-chart',{quizId: vm.quizExercise.id});
            }
            else{
                for (var i = 0; i < vm.quizExercise.questions.length; i++){
                    if(vm.quizExercise.questions[i].id === vm.question.id){
                        $state.go('multiple-choice-question-statistic-chart', {quizId: vm.quizExercise.id, questionId: vm.quizExercise.questions[i+1].id});
                    }
                }
            }
        }

    }
})();
