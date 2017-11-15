(function() {
    'use strict';

    angular
        .module('artemisApp')
        .controller('SubmittedAnswerDialogController', SubmittedAnswerDialogController);

    SubmittedAnswerDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'SubmittedAnswer', 'Question', 'QuizSubmission'];

    function SubmittedAnswerDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, SubmittedAnswer, Question, QuizSubmission) {
        var vm = this;

        vm.submittedAnswer = entity;
        vm.clear = clear;
        vm.save = save;
        vm.questions = Question.query();
        vm.quizsubmissions = QuizSubmission.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.submittedAnswer.id !== null) {
                SubmittedAnswer.update(vm.submittedAnswer, onSaveSuccess, onSaveError);
            } else {
                SubmittedAnswer.save(vm.submittedAnswer, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('artemisApp:submittedAnswerUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
