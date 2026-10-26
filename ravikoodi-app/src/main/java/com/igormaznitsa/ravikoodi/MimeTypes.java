package com.igormaznitsa.ravikoodi;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MimeTypes {

  private static final int MAX_FILTER_LABEL_LENGTH = 40;
  private static final Map<String, MimeRecord> EXTENSIONS = new HashMap<>();
  private static final List<ExtensionGroup> GROUPS = new ArrayList<>();

  static {
    addRecord(new String[] {"mpg", "mpeg", "mp1", "mp2", "mp4", "m1v", "m1a", "m2a", "mpa", "mpv"},
        "video/mpeg", ContentType.VIDEO);
    addRecord(new String[] {"rv", "rm"}, "video/x-pn-realvideo", ContentType.VIDEO);
    addRecord(new String[] {"rmvb"}, "application/vnd.rn-realmedia-vbr", ContentType.VIDEO);
    addRecord(new String[] {"flv"}, "video/x-flv", ContentType.VIDEO);
    addRecord(new String[] {"webm"}, "video/x-ms-wmv", ContentType.VIDEO);
    addRecord(new String[] {"m3u8"}, "application/x-mpegURL", ContentType.VIDEO);
    addRecord(new String[] {"ts"}, "video/MP2T", ContentType.VIDEO);
    addRecord(new String[] {"3gp"}, "video/3gpp", ContentType.VIDEO);
    addRecord(new String[] {"avi"}, "video/x-msvideo", ContentType.VIDEO);
    addRecord(new String[] {"wmv"}, "video/x-ms-wmv", ContentType.VIDEO);
    addRecord(new String[] {"mkv"}, "video/x-matroska", ContentType.VIDEO);
    addRecord(new String[] {"vob"}, "video/dvd", ContentType.VIDEO);
    addRecord(new String[] {"ogm"}, "video/ogg", ContentType.VIDEO);
    addRecord(new String[] {"fli", "flc"}, "video/flc", ContentType.VIDEO);
    addRecord(new String[] {"mov", "qt"}, "video/quicktime", ContentType.VIDEO);

    addRecord(new String[] {"mid", "midi"}, "application/midi", ContentType.AUDIO);
    addRecord(new String[] {"aiff", "aif", "aifc"}, "audio/x-aiff", ContentType.AUDIO);
    addRecord(new String[] {"wav", "wave"}, "audio/wav", ContentType.AUDIO);
    addRecord(new String[] {"aac"}, "audio/aac", ContentType.AUDIO);
    addRecord(new String[] {"m4a", "m4b", "m4p", "m4r", "m4v"}, "audio/mp4", ContentType.AUDIO);
    addRecord(new String[] {"mp3"}, "audio/mpeg", ContentType.AUDIO);
    addRecord(new String[] {"ogg"}, "audio/ogg", ContentType.AUDIO);
    addRecord(new String[] {"flac"}, "audio/flac", ContentType.AUDIO);
    addRecord(new String[] {"ape"}, "audio/x-monkeys-audio", ContentType.AUDIO);

    addRecord(new String[] {"gif"}, "image/gif", ContentType.PICTURE);
    addRecord(new String[] {"pcx"}, "image/pcx", ContentType.PICTURE);
    addRecord(new String[] {"bmp"}, "image/bmp", ContentType.PICTURE);
    addRecord(new String[] {"jpg", "jpeg"}, "image/jpeg", ContentType.PICTURE);
    addRecord(new String[] {"tif", "tiff"}, "image/tiff", ContentType.PICTURE);
    addRecord(new String[] {"png"}, "image/png", ContentType.PICTURE);
    addRecord(new String[] {"tga"}, "image/x-tga", ContentType.PICTURE);
  }

  private static void addRecord(@NonNull final String[] extensions, @NonNull final String mime,
                                @NonNull final ContentType type) {
    if (extensions.length > 0) {
      GROUPS.add(new ExtensionGroup(extensions, type));
    }
    for (final String e : extensions) {
      final MimeRecord newRecord = new MimeRecord(e, mime, type);
      if (EXTENSIONS.put(newRecord.getExtension(), newRecord) != null) {
        throw new Error("Detected duplication for extension : " + e);
      }
    }
  }

  private static FileNameExtensionFilter categoryFilter(final ContentType contentType) {
    final String[] extensions = GROUPS.stream()
        .filter(group -> group.type() == contentType)
        .flatMap(group -> Arrays.stream(group.extensions()))
        .toArray(String[]::new);
    return new FileNameExtensionFilter(contentType.getFilterName(), extensions);
  }

  private static List<FileNameExtensionFilter> extensionFilters() {
    final Collator order = Collator.getInstance(Locale.ROOT);
    return GROUPS.stream()
        .flatMap(group -> filtersForGroup(group).stream())
        .sorted((left, right) -> order.compare(left.getDescription(), right.getDescription()))
        .toList();
  }

  private static List<FileNameExtensionFilter> filtersForGroup(final ExtensionGroup group) {
    if (group.extensions().length == 0) {
      return List.of();
    }
    final String combined = filterLabel(group.extensions()[0], group.extensions());
    if (combined.length() <= MAX_FILTER_LABEL_LENGTH) {
      return List.of(new FileNameExtensionFilter(combined, group.extensions()));
    }
    return Arrays.stream(group.extensions())
        .map(extension -> new FileNameExtensionFilter(filterLabel(extension, extension), extension))
        .toList();
  }

  private static String filterLabel(final String name, final String... extensions) {
    final String listed = Arrays.stream(extensions)
        .map(extension -> "*." + extension)
        .collect(joining(", "));
    return name.toUpperCase(Locale.ROOT) + " (" + listed + ')';
  }

  public List<FileFilter> makeOpenFileFilters() {
    final List<FileFilter> filters = new ArrayList<>();
    filters.add(categoryFilter(ContentType.VIDEO));
    filters.add(categoryFilter(ContentType.AUDIO));
    filters.add(categoryFilter(ContentType.PICTURE));
    filters.addAll(extensionFilters());
    return List.copyOf(filters);
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

  public enum ContentType {
    VIDEO("Video files", "Video"),
    AUDIO("Audio files", "Audio"),
    PICTURE("Picture files", "Picture"),
    UNKNOWN("Unknown files", "Unknown");

    private final String description;
    private final String filterName;

    ContentType(@NonNull final String description, @NonNull final String filterName) {
      this.description = description;
      this.filterName = filterName;
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

    @NonNull
    public String getDescription() {
      return this.description;
    }

    @NonNull
    public String getFilterName() {
      return this.filterName;
    }

  }

  private record ExtensionGroup(@NonNull String[] extensions, @NonNull ContentType type) {
    private ExtensionGroup {
      extensions = extensions.clone();
    }
  }

  public static final class MimeRecord implements Comparable<MimeRecord> {

    private final String extension;
    private final String mime;
    private final ContentType contentType;

    private MimeRecord(@NonNull final String extension, @NonNull final String mime,
                       @NonNull final ContentType contentType) {
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

    @Override
    public boolean equals(final Object other) {
      return other instanceof MimeRecord that && this.extension.equals(that.extension);
    }

    @Override
    public int hashCode() {
      return this.extension.hashCode();
    }
  }
}
