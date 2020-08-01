package com.igormaznitsa.ravikoodi;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.filechooser.FileFilter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MimeTypes {

  public enum ContentType {
    VIDEO("Video files"),
    AUDIO("Audio files"),
    PICTURE("Picture files"),
    UNKNOWN("Unknown files");

    private final String description;

    private ContentType(@NonNull final String description) {
      this.description = description;
    }

    @NonNull
    public String getDescription() {
      return this.description;
    }

    @NonNull
    public static ContentType findType(@NonNull final Path path) {
      final String extension = Utils.getFileExtension(path);
      final MimeRecord record = EXTENSIONS.get(extension.toLowerCase(Locale.ENGLISH));
      return record == null ? UNKNOWN : record.getContentType();
    }

    @NonNull
    public static ContentType findType(@NonNull final File file) {
      final String extension = Utils.getFileExtension(file);
      final MimeRecord record = EXTENSIONS.get(extension.toLowerCase(Locale.ENGLISH));
      return record == null ? VIDEO : record.getContentType();
    }

  }

  public static final class MimeRecord implements Comparable<MimeRecord> {

    private final String extension;
    private final String mime;
    private final ContentType contentType;

    private MimeRecord(@NonNull final String extension, @NonNull final String mime, @NonNull final ContentType contentType) {
      this.extension = extension;
      this.contentType = contentType;
      this.mime = mime;
    }

    @NonNull
    public ContentType getContentType() {
      return this.contentType;
    }

    @NonNull
    public String getExtension() {
      return this.extension;
    }

    @NonNull
    public String getMime() {
      return this.mime;
    }

    @Override
    public int compareTo(@NonNull final MimeRecord o) {
      return this.extension.compareTo(o.extension);
    }
  }

  private static final Map<String, MimeRecord> EXTENSIONS = new HashMap<String, MimeRecord>();

  private static void addRecord(@NonNull final String[] extensions, @NonNull final String mime, @NonNull final ContentType type) {
    for (final String e : extensions) {
      final MimeRecord newRecord = new MimeRecord(e, mime, type);
      if (EXTENSIONS.put(newRecord.getExtension(), newRecord) != null) {
        throw new Error("Detected duplication for extension : " + e);
      }
    }
  }

  static {
    addRecord(new String[]{"mpg", "mpeg", "mp1", "mp2", "mp4", "m1v", "m1a", "m2a", "mpa", "mpv"}, "video/mpeg", ContentType.VIDEO);
    addRecord(new String[]{"rv", "rm"}, "video/x-pn-realvideo", ContentType.VIDEO);
    addRecord(new String[]{"rmvb"}, "application/vnd.rn-realmedia-vbr", ContentType.VIDEO);
    addRecord(new String[]{"flv"}, "video/x-flv", ContentType.VIDEO);
    addRecord(new String[]{"webm"}, "video/x-ms-wmv", ContentType.VIDEO);
    addRecord(new String[]{"m3u8"}, "application/x-mpegURL", ContentType.VIDEO);
    addRecord(new String[]{"ts"}, "video/MP2T", ContentType.VIDEO);
    addRecord(new String[]{"3gp"}, "video/3gpp", ContentType.VIDEO);
    addRecord(new String[]{"avi"}, "video/x-msvideo", ContentType.VIDEO);
    addRecord(new String[]{"wmv"}, "video/x-ms-wmv", ContentType.VIDEO);
    addRecord(new String[]{"mkv"}, "video/x-matroska", ContentType.VIDEO);
    addRecord(new String[]{"vob"}, "video/dvd", ContentType.VIDEO);
    addRecord(new String[]{"ogm"}, "video/ogg", ContentType.VIDEO);
    addRecord(new String[]{"fli", "flc"}, "video/flc", ContentType.VIDEO);
    addRecord(new String[]{"mov", "qt"}, "video/quicktime", ContentType.VIDEO);

    addRecord(new String[]{"mid", "midi"}, "application/midi", ContentType.AUDIO);
    addRecord(new String[]{"aiff", "aif", "aifc"}, "audio/x-aiff", ContentType.AUDIO);
    addRecord(new String[]{"wav", "wave"}, "audio/wav", ContentType.AUDIO);
    addRecord(new String[]{"aac"}, "audio/aac", ContentType.AUDIO);
    addRecord(new String[]{"m4a", "m4b", "m4p", "m4r", "m4v"}, "audio/mp4", ContentType.AUDIO);
    addRecord(new String[]{"mp3"}, "audio/mpeg", ContentType.AUDIO);
    addRecord(new String[]{"ogg"}, "audio/ogg", ContentType.AUDIO);
    addRecord(new String[]{"flac"}, "audio/flac", ContentType.AUDIO);
    addRecord(new String[]{"ape"}, "audio/x-monkeys-audio", ContentType.AUDIO);

    addRecord(new String[]{"gif"}, "image/gif", ContentType.PICTURE);
    addRecord(new String[]{"pcx"}, "image/pcx", ContentType.PICTURE);
    addRecord(new String[]{"bmp"}, "image/bmp", ContentType.PICTURE);
    addRecord(new String[]{"jpg", "jpeg"}, "image/jpeg", ContentType.PICTURE);
    addRecord(new String[]{"tif", "tiff"}, "image/tiff", ContentType.PICTURE);
    addRecord(new String[]{"png"}, "image/png", ContentType.PICTURE);
    addRecord(new String[]{"tga"}, "image/x-tga", ContentType.PICTURE);
  }

  public FileFilter makeFileFilter(final ContentType contentType) {

    final StringBuilder extensions = new StringBuilder();
    final Map<String, MimeRecord> records = new HashMap<>();

    EXTENSIONS
            .entrySet()
            .stream()
            .filter(x -> x.getValue().getContentType() == contentType)
            .peek(x -> {
              if (extensions.length() != 0) {
                extensions.append(',');
              }
              extensions.append("*.").append(x.getKey());
              records.put(x.getKey(), x.getValue());
            })
            .count();

    final String extensionList = extensions.toString();

    return new FileFilter() {

      @Override
      public boolean accept(@NonNull final File f) {
        return f.isDirectory() || records.containsKey(Utils.getFileExtension(f).toLowerCase(Locale.ENGLISH));
      }

      @Override
      public String getDescription() {
        return contentType.getDescription() + " (" + extensionList + ')';
      }
    };
  }

  @Nullable
  public MimeRecord getMimeRecord(@NonNull final String extension) {
    return EXTENSIONS.get(extension.toLowerCase(Locale.ENGLISH));
  }

  @Nullable
  public MimeRecord getMimeRecord(@NonNull final Path file) {
      final String extension = Utils.getFileExtension(file).toLowerCase(Locale.ENGLISH);
      return getMimeRecord(extension);
  }
  
  @NonNull
  public String findMimeTypeForFile(@NonNull final Path file) {
      final MimeRecord record = getMimeRecord(file);
      return record == null ? "application/x-binary" : record.getMime();
  }
}
