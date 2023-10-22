/*
 * Copyright 2023 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi.ytloader;

import java.util.Arrays;
import java.util.Locale;

public enum YtVideoType {
    GPP3("video/3gpp"), MP4("video/mp4"), WEBM("video/webm"), UNKNOWN(""); 
    
    private static final YtVideoType [] VALUES = YtVideoType.values();
    public static YtVideoType find(final String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        final String lowCased = value.toLowerCase(Locale.ENGLISH);
        return Arrays.stream(VALUES).filter(x -> lowCased.contains(x.mime)).findFirst().orElse(UNKNOWN);
    }
    
    private final String mime;
    
    private YtVideoType(final String mime) {
        this.mime = mime;
    }
    
    public String getMime(){
        return this.mime;
    }
    
    
}
