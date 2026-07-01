# Vector Drawable Thumbnails Plugin Changelog

## [Unreleased]

### Fixed
- Thumbnails not appearing in large projects (e.g. React Native repos with iOS `Pods`): the file scanner now skips symlinks, the `Pods` directory, and `build` output folders, and results paint incrementally as the scan runs instead of only after it completes.
