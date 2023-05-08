# Logging

General guidance of when to use different log levels:

* `FATAL`: An unrecoverable error occurred and the entire runtime is going to exit. Only used by `Main` before exiting due to an exception.
* `ERROR`: An unrecoverable error occurred which terminates the current operation, but the application as a whole can still continue.
* `WARN`: A recoverable error occurred, but the operation can continue.
* `INFO`: General information, which would be of interest to a general user.
* `DEBUG`: Detailed information, which would be of interest to a general user debugging the application.
* `TRACE`: Highly detailed information, which would be of interest to a developer trying resolve a bug.

## Resources

* [When to use the different log levels](https://stackoverflow.com/questions/2031163/when-to-use-the-different-log-levels)
