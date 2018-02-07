// Karma configuration
// http://karma-runner.github.io/0.13/config/configuration-file.html

var sourcePreprocessors = ['coverage'];

function isDebug() {
    return process.argv.indexOf('--debug') >= 0;
}

if (isDebug()) {
    // Disable JS minification if Karma is run with debug option.
    sourcePreprocessors = [];
}

module.exports = function (config) {
    config.set({
        // base path, that will be used to resolve files and exclude
        basePath: 'src/test/javascript/'.replace(/[^/]+/g, '..'),

        // testing framework to use (jasmine/mocha/qunit/...)
        frameworks: ['jasmine'],

        // list of files / patterns to load in the browser
        files: [
            // bower:js
            'src/main/webapp/bower_components/jquery/dist/jquery.js',
            'src/main/webapp/bower_components/messageformat/messageformat.js',
            'src/main/webapp/bower_components/jquery-ui/jquery-ui.js',
            'src/main/webapp/bower_components/json3/lib/json3.js',
            'src/main/webapp/bower_components/lodash/lodash.js',
            'src/main/webapp/bower_components/sockjs-client/dist/sockjs.js',
            'src/main/webapp/bower_components/stomp-websocket/lib/stomp.min.js',
            'src/main/webapp/bower_components/moment/moment.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/ace.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-java.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-html.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-json.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-xml.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-python.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-sql.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-javascript.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-markdown.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-jsp.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-jade.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/mode-swift.js',
            'src/main/webapp/bower_components/ace-builds/src-min-noconflict/ext-modelist.js',
            'src/main/webapp/bower_components/blob-polyfill/Blob.js',
            'src/main/webapp/bower_components/file-saver.js/FileSaver.js',
            'src/main/webapp/bower_components/remarkable/dist/remarkable.js',
            'src/main/webapp/bower_components/showdown/dist/showdown.js',
            'src/main/webapp/bower_components/chart.js/dist/Chart.js',
            'src/main/webapp/bower_components/hone/hone.js',
            'src/main/webapp/bower_components/tether/dist/js/tether.js',
            'src/main/webapp/bower_components/angular-hotkeys/angular-hotkeys.js',
            'src/main/webapp/bower_components/angular-hotkeys/angular-hotkeys.min.js',
            'src/main/webapp/bower_components/cfp-angular-hotkeys/build/hotkeys.js',
            'src/main/webapp/bower_components/angular/angular.js',
            'src/main/webapp/bower_components/angular-aria/angular-aria.js',
            'src/main/webapp/bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
            'src/main/webapp/bower_components/angular-cache-buster/angular-cache-buster.js',
            'src/main/webapp/bower_components/angular-cookies/angular-cookies.js',
            'src/main/webapp/bower_components/angular-dynamic-locale/src/tmhDynamicLocale.js',
            'src/main/webapp/bower_components/ngstorage/ngStorage.js',
            'src/main/webapp/bower_components/angular-loading-bar/build/loading-bar.js',
            'src/main/webapp/bower_components/angular-resource/angular-resource.js',
            'src/main/webapp/bower_components/angular-sanitize/angular-sanitize.js',
            'src/main/webapp/bower_components/angular-translate/angular-translate.js',
            'src/main/webapp/bower_components/angular-translate-interpolation-messageformat/angular-translate-interpolation-messageformat.js',
            'src/main/webapp/bower_components/angular-translate-loader-partial/angular-translate-loader-partial.js',
            'src/main/webapp/bower_components/angular-translate-storage-cookie/angular-translate-storage-cookie.js',
            'src/main/webapp/bower_components/angular-ui-router/release/angular-ui-router.js',
            'src/main/webapp/bower_components/bootstrap-ui-datetime-picker/dist/datetime-picker.js',
            'src/main/webapp/bower_components/ng-file-upload/ng-file-upload.js',
            'src/main/webapp/bower_components/ngInfiniteScroll/build/ng-infinite-scroll.js',
            'src/main/webapp/bower_components/angular-moment/angular-moment.js',
            'src/main/webapp/bower_components/bootstrap-treeview/dist/bootstrap-treeview.min.js',
            'src/main/webapp/bower_components/angular-ui-ace/ui-ace.js',
            'src/main/webapp/bower_components/angular-file-saver/dist/angular-file-saver.bundle.js',
            'src/main/webapp/bower_components/angular-resizable/src/angular-resizable.js',
            'src/main/webapp/bower_components/ez-ng/dist/ez-ng.js',
            'src/main/webapp/bower_components/angular-chart.js/dist/angular-chart.js',
            'src/main/webapp/bower_components/ngSmoothScroll/lib/angular-smooth-scroll.js',
            'src/main/webapp/bower_components/angular-scroll/angular-scroll.js',
            'src/main/webapp/bower_components/angular-bind-html-compile/angular-bind-html-compile.js',
            'src/main/webapp/bower_components/angular-ui-tour/dist/angular-ui-tour.js',
            'src/main/webapp/bower_components/angular-ui-sortable/sortable.js',
            'src/main/webapp/bower_components/angular-mocks/angular-mocks.js',
            // endbower
            'src/main/webapp/app/app.module.js',
            'src/main/webapp/app/app.state.js',
            'src/main/webapp/app/app.constants.js',
            'src/main/webapp/app/**/*.+(js|html)',
            'src/test/javascript/spec/helpers/module.js',
            'src/test/javascript/spec/helpers/httpBackend.js',
            'src/test/javascript/**/!(karma.conf|protractor.conf).js'
        ],


        // list of files / patterns to exclude
        exclude: ['src/test/javascript/e2e/**'],

        preprocessors: {
            './**/*.js': sourcePreprocessors
        },

        reporters: ['dots', 'junit', 'coverage', 'progress'],

        junitReporter: {
            outputFile: '../build/test-results/karma/TESTS-results.xml'
        },

        coverageReporter: {
            dir: 'build/test-results/coverage',
            reporters: [
                {type: 'lcov', subdir: 'report-lcov'}
            ]
        },

        // web server port
        port: 9876,

        // level of logging
        // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
        logLevel: config.LOG_INFO,

        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: false,

        // Start these browsers, currently available:
        // - Chrome
        // - ChromeCanary
        // - Firefox
        // - Opera
        // - Safari (only Mac)
        // - IE (only Windows)
        browsers: ['PhantomJS'],

        // Continuous Integration mode
        // if true, it capture browsers, run tests and exit
        singleRun: false,

        // to avoid DISCONNECTED messages when connecting to slow virtual machines
        browserDisconnectTimeout: 10000, // default 2000
        browserDisconnectTolerance: 1, // default 0
        browserNoActivityTimeout: 4 * 60 * 1000 //default 10000
    });
};
