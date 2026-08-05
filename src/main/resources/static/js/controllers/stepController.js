(function () {
    'use strict';

    angular.module('onboardingApp').controller('StepController', [
        'OnboardingApi', '$routeParams', '$location',
        function (OnboardingApi, $routeParams, $location) {
            var vm = this;

            vm.applicationId = $routeParams.applicationId;
            vm.loading = true;
            vm.submitting = false;
            vm.error = null;
            vm.page = null;
            vm.reviewEntries = [];

            function normalizeValues(page) {
                var values = angular.copy(page.values) || {};
                page.step.fields.forEach(function (field) {
                    if (values[field.name] === undefined || values[field.name] === null) {
                        values[field.name] = field.type === 'CHECKBOX' ? 'false' : '';
                    }
                });
                return values;
            }

            function toRows(obj) {
                return obj ? Object.keys(obj).map(function (k) { return { key: k, value: obj[k] }; }) : [];
            }

            function stepClass(page, step) {
                if (page.completedStepKeys.indexOf(step.key) !== -1) {
                    return 'done';
                }
                if (step.key === page.step.key) {
                    return 'current';
                }
                return '';
            }

            function render(page) {
                vm.page = page;
                vm.values = normalizeValues(page);
                vm.reviewEntries = page.reviewData ? Object.keys(page.reviewData).map(function (title) {
                    return { title: title, rows: toRows(page.reviewData[title]) };
                }) : [];
                vm.progressSteps = page.allSteps.map(function (s) {
                    return { key: s.key, title: s.title, cssClass: stepClass(page, s) };
                });
            }

            OnboardingApi.getStep(vm.applicationId).then(function (page) {
                render(page);
                vm.loading = false;
            }, function () {
                vm.error = 'Could not load this application. It may not exist any more.';
                vm.loading = false;
            });

            vm.submit = function () {
                vm.submitting = true;
                var stepKey = vm.page.step.key;
                var values = {};
                vm.page.step.fields.forEach(function (field) {
                    var v = vm.values[field.name];
                    values[field.name] = (v === undefined || v === null) ? '' : String(v);
                });
                OnboardingApi.submitStep(vm.applicationId, stepKey, values).then(function (response) {
                    vm.submitting = false;
                    if (response.success) {
                        if (response.applicationComplete) {
                            $location.path('/result/' + vm.applicationId);
                        } else {
                            render(response.page);
                        }
                    } else {
                        render(response.page);
                    }
                }, function () {
                    vm.submitting = false;
                    vm.error = 'Something went wrong submitting this step. Please try again.';
                });
            };

            vm.buttonLabel = function () {
                if (!vm.page) {
                    return 'Continue';
                }
                if (vm.page.step.reviewStep) {
                    return 'Submit application';
                }
                return vm.page.step.integrationType ? 'Continue and run check' : 'Continue';
            };
        }
    ]);
})();
