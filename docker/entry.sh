#!/bin/bash

/eve-ref/bin/eve-ref $1
ex=$?

if [ -f /tmp/hs_err_pid*.log ]; then
    cat /tmp/hs_err_pid*.log
fi

exit $ex
