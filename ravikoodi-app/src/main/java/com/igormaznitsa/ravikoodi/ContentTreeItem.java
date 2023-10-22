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

import java.util.Comparator;
import org.springframework.lang.NonNull;

public interface ContentTreeItem {
    static final Comparator<ContentTreeItem> CONTENT_ITEM_COMPARATOR = (@NonNull final ContentTreeItem o1, @NonNull final ContentTreeItem o2) -> {
        if (o1 instanceof ContentFolder && o2 instanceof ContentFile) {
            return -1;
        } else if (o1 instanceof ContentFile && o2 instanceof ContentFolder) {
            return 1;
        } else {
            return o1.getFileNameAsString().compareTo(o2.getFileNameAsString());
        }
    };


    @NonNull
    String getFileNameAsString();
}
