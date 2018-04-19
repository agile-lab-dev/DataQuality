/**
 * System configuration for Angular samples
 * Adjust as necessary for your application needs.
 */
(function (global) {
    System.config({
        paths: {
            // paths serve as alias
            'npm:': 'dataquality/assets/lib/',
            'vendor:': 'dataquality/assets/vendor/',
            'unpkg:': 'https://unpkg.com/'
        },
        // map tells the System loader where to look for things
        map: {
            // our app is within the app folder
            'app': 'dataquality/assets/app',

            // angular bundles
            '@angular/core': 'npm:angular__core/bundles/core.umd.js',
            '@angular/common': 'npm:angular__common/bundles/common.umd.js',
            '@angular/compiler': 'npm:angular__compiler/bundles/compiler.umd.js',
            '@angular/platform-browser': 'npm:angular__platform-browser/bundles/platform-browser.umd.js',
            '@angular/platform-browser-dynamic': 'npm:angular__platform-browser-dynamic/bundles/platform-browser-dynamic.umd.js',
            '@angular/platform-browser/animations':'npm:angular__platform-browser/bundles/platform-browser-animations.umd.js',
            '@angular/http': 'npm:angular__http/bundles/http.umd.js',
            '@angular/router': 'npm:angular__router/bundles/router.umd.js',
            '@angular/forms': 'npm:angular__forms/bundles/forms.umd.js',
            '@angular/animations': 'npm:angular__animations/bundles/animations.umd.js',
            '@angular/animations/browser': 'npm:angular__animations/bundles/animations-browser.umd.js',
            '@angular/material': 'npm:angular__material/bundles/material.umd.js',
            '@angular/cdk/platform': 'npm:angular__cdk/bundles/cdk-platform.umd.js',
            '@angular/cdk/a11y': 'npm:angular__cdk/bundles/cdk-a11y.umd.js',
            '@angular/cdk/bidi': 'npm:angular__cdk/bundles/cdk-bidi.umd.js',
            '@angular/cdk/coercion': 'npm:angular__cdk/bundles/cdk-coercion.umd.js',
            '@angular/cdk/collections': 'npm:angular__cdk/bundles/cdk-collections.umd.js',
            '@angular/cdk/keycodes': 'npm:angular__cdk/bundles/cdk-keycodes.umd.js',
            '@angular/cdk/observers': 'npm:angular__cdk/bundles/cdk-observers.umd.js',
            '@angular/cdk/overlay': 'npm:angular__cdk/bundles/cdk-overlay.umd.js',
            '@angular/cdk/platform': 'npm:angular__cdk/bundles/cdk-platform.umd.js',
            '@angular/cdk/portal': 'npm:angular__cdk/bundles/cdk-portal.umd.js',
            '@angular/cdk/rxjs': 'npm:angular__cdk/bundles/cdk-rxjs.umd.js',
            '@angular/cdk/scrolling': 'npm:angular__cdk/bundles/cdk-scrolling.umd.js',
            '@angular/cdk/stepper': 'npm:angular__cdk/bundles/cdk-stepper.umd.js',
            '@angular/cdk/table': 'npm:angular__cdk/bundles/cdk-table.umd.js',
            '@angular/cdk/testing': 'npm:angular__cdk/bundles/cdk-testing.umd.js',
            '@angular/flex-layout' : 'npm:angular__flex-layout/bundles/flex-layout.umd.js',

            // other libraries
            'rxjs':                      'npm:rxjs',
            'angular-in-memory-web-api': 'npm:angular-in-memory-web-api/bundles/in-memory-web-api.umd.js',
            'file-saver': 'npm:file-saver',
            'ng2-file-upload': 'npm:ng2-file-upload/bundles/ng2-file-upload.umd.js',

            // change this paths only if you understand what you do!
            'codemirror': 'npm:codemirror/lib/codemirror.js',
            'ng2-codemirror': 'npm:ng2-codemirror/lib/index.js',
            'codemirror.module': 'npm:ng2-codemirror',
            'codemirror.component': 'npm:ng2-codemirror/lib/codemirror.component.js',
            'codemirror/mode/sql/sql': 'npm:codemirror'
        },
        // packages tells the System loader how to load when no filename and/or no extension
        packages: {
            app: {
                defaultExtension: 'js',
                meta: {
                    './*.js': {
                        loader: 'dataquality/assets/systemjs-angular-loader.js'
                    }
                }
            },
            rxjs: {
                defaultExtension: 'js'
            },
             // this configurations are required for codemirror component.
             // touch it only if you understand what you do!
            'codemirror.module' : {
                main: '/lib/codemirror.module.js',
                defaultExtension: 'js'
            },
            'ng2-codemirror': {
                defaultExtension: 'js'
            },
            'codemirror/mode/sql/sql': {
                main: '/mode/sql/sql.js',
                defaultExtension: 'js'
            },
             'file-saver': {
                    format: 'global',
                    main: 'FileSaver.js',
                    defaultExtension: 'js'
             }
        }
    });
})(this);