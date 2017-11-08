(function () {
    'use strict';

    angular
        .module('artemisApp')
        .controller('InstructorDashboardResultDialogController', InstructorDashboardResultDialogController);

    InstructorDashboardResultDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'participationEntity', 'Result', 'AlertService'];

    function InstructorDashboardResultDialogController($timeout, $scope, $stateParams, $uibModalInstance, entity, participationEntity, Result, AlertService) {
        var vm = this;

        vm.result = entity;
        if(vm.result.completionDate) {
            vm.result.completionDate = new Date(vm.result.completionDate);
        }
        vm.clear = clear;
        vm.datePickerOpenStatus = {};
        vm.openCalendar = openCalendar;
        vm.save = save;

        if(participationEntity) {
            entity.participation = participationEntity;
        } else {
            clear();
        }



        $timeout(function () {
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear() {
            $uibModalInstance.dismiss('cancel');
        }

        function save() {
            vm.isSaving = true;
            if (vm.result.id !== null) {
                Result.update(vm.result, onSaveSuccess, onSaveError);
            } else {
                Result.save(vm.result, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess(result) {
            $scope.$emit('artemisApp:resultUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError() {
            vm.isSaving = false;
        }

        vm.datePickerOpenStatus.completionDate = false;

        function openCalendar(date) {
            vm.datePickerOpenStatus[date] = true;
        }
    }
})();
