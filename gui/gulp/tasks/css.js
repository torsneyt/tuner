var gulp = require('gulp');
var sass = require('gulp-ruby-sass');
var filter = require('gulp-filter');
var config = require('../config');

gulp.task('sass', function() {
  var browserSync = require('browser-sync').reload;
  return gulp.src(config.src.scss + '/app.scss')
    .pipe(sass())
    .on('error', function(err) {console.error(err.message)})
    .pipe(gulp.dest(config.dest.css))
    .pipe(filter('**/*.css')) // only send css files to browsersync
    .pipe(browserSync({stream: true}));
});