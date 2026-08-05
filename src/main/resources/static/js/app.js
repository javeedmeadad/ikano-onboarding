(function () {
    'use strict';

    angular.module('onboardingApp', ['ngRoute'])
        .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
            $locationProvider.hashPrefix('');

            $routeProvider
                .when('/', {
                    templateUrl: 'partials/home.html',
                    controller: 'HomeController',
                    controllerAs: 'vm'
                })
                .when('/step/:applicationId', {
                    templateUrl: 'partials/step.html',
                    controller: 'StepController',
                    controllerAs: 'vm'
                })
                .when('/result/:applicationId', {
                    templateUrl: 'partials/result.html',
                    controller: 'ResultController',
                    controllerAs: 'vm'
                })
                .when('/resume/:token', {
                    templateUrl: 'partials/resume.html',
                    controller: 'ResumeController',
                    controllerAs: 'vm'
                })
                .otherwise({ redirectTo: '/' });
        }]);
})();
