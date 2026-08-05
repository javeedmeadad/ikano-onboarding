(function () {
    'use strict';

    angular.module('onboardingApp').factory('OnboardingApi', ['$http', function ($http) {
        return {
            getMeta: function () {
                return $http.get('/api/meta').then(function (res) {
                    return res.data;
                });
            },
            start: function (country, customerType) {
                return $http.post('/api/applications', { country: country, customerType: customerType })
                    .then(function (res) {
                        return res.data;
                    });
            },
            getStep: function (applicationId) {
                return $http.get('/api/applications/' + applicationId + '/step').then(function (res) {
                    return res.data;
                });
            },
            submitStep: function (applicationId, stepKey, values) {
                return $http.post('/api/applications/' + applicationId + '/step', { stepKey: stepKey, values: values })
                    .then(function (res) {
                        return res.data;
                    });
            },
            getResult: function (applicationId) {
                return $http.get('/api/applications/' + applicationId + '/result').then(function (res) {
                    return res.data;
                });
            },
            resolveResume: function (token) {
                return $http.get('/api/resume/' + token).then(function (res) {
                    return res.data;
                });
            }
        };
    }]);
})();
