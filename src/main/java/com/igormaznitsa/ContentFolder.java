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
package com.igormaznitsa;

import com.igormaznitsa.ravikoodi.ContentFile;
import com.igormaznitsa.ravikoodi.ContentTreeItem;
import com.igormaznitsa.ravikoodi.MimeTypes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public final class ContentFolder implements ContentTreeItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentFolder.class);
    
    private final Path filePath;
    private final String fileName;
    private final List<ContentTreeItem> files = new ArrayList<>();

    public ContentFolder(@NonNull final Path filePath) throws IOException {
        this.filePath = filePath;
        this.fileName = this.filePath.getFileName().toString();

        Files.list(filePath).filter(f -> {
            return Files.isReadable(f) && (Files.isDirectory(f) || MimeTypes.ContentType.findType(f) != MimeTypes.ContentType.UNKNOWN);
        }).peek(f -> {
            if (Files.isDirectory(f)) {
                try {
                    this.files.add(new ContentFolder(f));
                } catch (IOException ex) {
                    LOGGER.error("Can't read folder {}", f, ex);
                }
            } else {
                this.files.add(new ContentFile(f, MimeTypes.ContentType.findType(f)));
            }
        }).count();

        Collections.sort(this.files, CONTENT_ITEM_COMPARATOR);
    }

    @Override
    @NonNull
    public String getFileNameAsString() {
        return this.fileName;
    }

    @NonNull
    public List<ContentTreeItem> getFiles() {
        return this.files;
    }
    
    @NonNull
    public Path getFilePath() {
        return this.filePath;
    }

    @Override
    @NonNull
    public String toString() {
        return this.getFileNameAsString();
    }
}
