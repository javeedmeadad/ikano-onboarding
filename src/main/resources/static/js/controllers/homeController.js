(function () {
    'use strict';

    angular.module('onboardingApp').controller('HomeController', [
        'OnboardingApi', '$location',
        function (OnboardingApi, $location) {
            var vm = this;

            vm.loading = true;
            vm.starting = false;
            vm.error = null;
            vm.countries = [];
            vm.customerTypes = [];
            vm.selection = { country: null, customerType: null };

            OnboardingApi.getMeta().then(function (meta) {
                vm.countries = meta.countries;
                vm.customerTypes = meta.customerTypes;
                vm.selection.country = meta.countries[0];
                vm.selection.customerType = meta.customerTypes[0];
                vm.loading = false;
            }, function () {
                vm.error = 'Could not reach the onboarding service. Please try again.';
                vm.loading = false;
            });

            vm.start = function () {
                vm.starting = true;
                vm.error = null;
                OnboardingApi.start(vm.selection.country, vm.selection.customerType).then(function (page) {
                    $location.path('/step/' + page.applicationId);
                }, function () {
                    vm.error = 'Could not start the application. Please try again.';
                    vm.starting = false;
                });
            };
        }
    ]);
})();
