# Vector Drawable Thumbnails Plugin Changelog

## [Unreleased]

## [2.3.6]

### Added
- Raster image thumbnails: an opt-in "Images" file-type checkbox now shows previews of `.png`, `.jpg`/`.jpeg`, `.webp`, `.gif`, and `.bmp` files alongside vector drawables and SVGs. WebP decoding is provided via the TwelveMonkeys ImageIO plugin.

### Fixed
- Thumbnails not appearing in large projects (e.g. React Native repos with iOS `Pods`): the file scanner now skips symlinks, the `Pods` directory, and `build` output folders, and results paint incrementally as the scan runs instead of only after it completes.
