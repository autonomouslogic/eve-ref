#!/bin/bash -e

# Runs a make command and commits changes if tests pass.
# Sends Discord notification if tests fail.
# Used by Github Actions.

COMMAND=$1
URL=$2
COMMIT_MESSAGE=$3

MAKE="make $COMMAND"
echo Running $MAKE
$MAKE

if [[ `git status --porcelain` ]]; then
  echo Changes detected
  git status

  # Run Java tests to validate changes
  echo "Running tests..."
  if make test-java; then
    echo "Tests passed, committing changes"

    # Configure git
    git config user.name "github-actions"
    git config user.email "github-actions@users.noreply.github.com"

    # Add all changes
    git add -A

    # Commit with provided message
    git commit -m "${COMMIT_MESSAGE}"

    # Push to current branch
    if ! git push; then
      echo "Git push failed, sending notification"
      curl -fsS -m 10 --retry 5 -o /dev/null \
        -X POST -H "Content-Type: application/json" \
        --data "{\"content\": \"${COMMAND} tests passed, but git push failed\"}" \
        $URL
      exit 1
    fi

    echo "Changes committed and pushed successfully"
  else
    echo "Tests failed, sending notification"
    curl -fsS -m 10 --retry 5 -o /dev/null \
      -X POST -H "Content-Type: application/json" \
      --data "{\"content\": \"${COMMAND} changes detected, but tests failed\"}" \
      $URL
    exit 1
  fi
else
  echo No changes detected
fi
