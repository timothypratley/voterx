#!/bin/bash
set -e
cd $(dirname $0)
boot production build target
cd target
git init
git add .
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "git@github.com:timothypratley/voterx.git" master:gh-pages
rm -fr resources/public/.git
