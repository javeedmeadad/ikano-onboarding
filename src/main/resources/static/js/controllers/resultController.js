(function () {
    'use strict';

    angular.module('onboardingApp').controller('ResultController', [
        'OnboardingApi', '$routeParams',
        function (OnboardingApi, $routeParams) {
            var vm = this;

            vm.loading = true;
            vm.error = null;
            vm.summary = null;
            vm.stepDataEntries = [];

            function toRows(obj) {
                return obj ? Object.keys(obj).map(function (k) { return { key: k, value: obj[k] }; }) : [];
            }

            OnboardingApi.getResult($routeParams.applicationId).then(function (summary) {
                vm.summary = summary;
                vm.stepDataEntries = Object.keys(summary.stepData).map(function (title) {
                    return { title: title, rows: toRows(summary.stepData[title]) };
                });
                vm.loading = false;
            }, function () {
                vm.error = 'Could not load the decision for this application.';
                vm.loading = false;
            });
        }
    ]);
})();
