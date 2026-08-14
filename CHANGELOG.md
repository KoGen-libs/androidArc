# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Initial extraction of `BaseMviViewModel` and `ScreenContainerWrapper` from Giraffe's internal
  UI layer into their own artifact (`io.github.eugenprog:androidarc`) - a minimal MVI ViewModel
  base class plus the Compose glue that wires it to a screen.
- `wrappedRequest` now returns the `Job` it launches, tracks every in-flight request internally,
  and cleans each one up from that tracking once it completes.
- `cancelAllRequests()` - cancels every `wrappedRequest` still in flight at once, e.g. from
  `onCleared()`.
- `wrappedRequest`'s optional `onFinal` callback, which always runs last regardless of how the
  request ended - success, error, or cancellation - typically used to dismiss a loading
  indicator no matter what.
- Two-screen demo app (`app` module - a notes list/details flow) wired through real KoGen DI and
  KoGen Navigation code generation, the same way Giraffe's own screens are.
- Unit test suite for `BaseMviViewModel`: action-handling crash safety, `wrappedRequest`'s
  success/error/cancellation paths, `onFinal`, `cancelAllRequests`, tracked-job cleanup, and
  effect delivery.

### Fixed
- `wrappedRequest`'s `catch (e: Exception)` also caught `CancellationException`, so cancelling a
  request (e.g. via `cancelAllRequests`) would have been reported through `onError` as if it had
  failed, and would have swallowed the cancellation itself instead of letting it propagate.
