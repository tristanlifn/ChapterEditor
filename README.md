# Chapter Editor

## Why?

This project is a personal project for editing the chapters in an audio file, there is also a select few other metadata tags.
I created this as an easy way to edit the chapters instead of first extracting and then reencoding them with `ffmpeg`.

## Building

### Requirements

- JDK 17 (the project targets `jvmToolchain(17)`)
- Gradle 9.6.1 — bundled via the Gradle wrapper (`./gradlew`), no manual install needed
- Internet access on first build (downloads dependencies and the Gradle distribution)
- Linux to build the AppImage (bundles a static `ffmpeg` build)
- Windows to build the Windows installer (`packageWindowsX64` requires a Windows host)

### Commands

Build the application:

```
./gradlew build
```

Run the application:

```
./gradlew run
```

Build native distributions (Deb, Rpm, AppImage, Exe):

```
./gradlew packageDeb
./gradlew packageRpm
./gradlew packageAppImage
./gradlew packageExe
```

Build the single-file Linux AppImage with bundled static ffmpeg:

```
./gradlew packageLinuxAppImage
```

Build the Windows x86_64 installer (Windows host only):

```
./gradlew packageWindowsX64
```

## Tag mappings

File wide:

- Title = `title` (file)
- Date = `date`
- Author = `author`
- Time base = `timebase` (shared by all chapters)
- Series = `album`
- Series artist = `album_artist`
- Comment = `comment`
- Narrator = `composer`

Chapter:

- Title = `title` (chapter)
- Start timestamp = `START`
- End timestamp = `END`

## Usage

### Import from an audio file

To import metadata from a file use the button `Import metadata`.
The chapters found will be mapped into chapter boxes containing a title and chapter start/end time.
The file wide metadata that the app supports will be mapped to the file metadata fields in the top.

NOTE: If a file has a metadata tag not supported by the app it will be lost when importing.

### Export to a txt file

You can also export the data to a txt file using the `Export metadata to txt file` button and then add metadata and/or then map it to an audio file.

### Export to an audio file

You can export the data to an audio file using the , the supported input formats are:
- mp3
- flac
- m4a
- m4b
- aac
- ogg
- opus
- wav 
- wma

NOTE: not all formats are tested.

You can then choose what file it should map to, the cover image (optional, ignored if no image is provided), and the export file name.

NOTE: do not call it the same as the input file name, as this can mess up the transcoding.

The output file will be in the same directory as the input file and will be converted to the format `m4b`.