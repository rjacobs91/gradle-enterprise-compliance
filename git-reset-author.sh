#!/bin/sh

# Credits: http://stackoverflow.com/a/750191

git filter-branch -f --env-filter "
    GIT_AUTHOR_NAME='Russell Jacobs'
    GIT_AUTHOR_EMAIL='russell.jacobs@outlook.com'
    GIT_COMMITTER_NAME='Russell Jacobs'
    GIT_COMMITTER_EMAIL='russell.jacobs@outlook.com'
  " HEAD