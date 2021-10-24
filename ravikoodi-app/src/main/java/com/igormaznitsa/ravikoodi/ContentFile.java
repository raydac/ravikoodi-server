/*
 * Copyright 2020 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.ravikoodi;

import java.nio.file.Path;
import org.springframework.lang.NonNull;

public final class ContentFile implements ContentTreeItem {

    private final Path filePath;
    private final String fileName;
    private final MimeTypes.ContentType contentType;

    public ContentFile(@NonNull final Path file, final MimeTypes.ContentType contentType) {
        this(file.getFileName().toString(), file, contentType);
    }

    public ContentFile(@NonNull final String name, @NonNull final Path file, final MimeTypes.ContentType contentType) {
        this.fileName = name;
        this.filePath = file;
        this.contentType = contentType;
    }

    @NonNull
    public MimeTypes.ContentType getContentType() {
        return this.contentType;
    }

    @NonNull
    public Path getFilePath() {
        return this.filePath;
    }

    @NonNull
    public String getFilePathAsString() {
        return this.filePath.toString();
    }

    @Override
    @NonNull
    public String getFileNameAsString() {
        return this.fileName;
    }

    @Override
    @NonNull
    public String toString() {
        return this.getFileNameAsString();
    }
}
