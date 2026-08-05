(function () {
    'use strict';

    angular.module('onboardingApp').controller('ResumeController', [
        'OnboardingApi', '$routeParams', '$location',
        function (OnboardingApi, $routeParams, $location) {
            var vm = this;

            vm.loading = true;
            vm.reason = null;

            OnboardingApi.resolveResume($routeParams.token).then(function (response) {
                if (response.status === 'IN_PROGRESS') {
                    $location.path('/step/' + response.applicationId).replace();
                } else if (response.status === 'FINISHED') {
                    $location.path('/result/' + response.applicationId).replace();
                } else {
                    vm.reason = response.reason;
                    vm.loading = false;
                }
            }, function () {
                vm.reason = 'This resume link is not recognised.';
                vm.loading = false;
            });
        }
    ]);
})();
