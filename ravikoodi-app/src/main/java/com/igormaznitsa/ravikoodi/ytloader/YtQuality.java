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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum YtQuality {
    UNKNOWN(-1),
    p144(144), 
    p240(240), 
    p360(360), 
    p480(480), 
    p720(720), 
    p1080(1080), 
    p1440(1440), 
    p2160(2160); 
    
    private static final Pattern VALUE_EXTRACTOR = Pattern.compile("^[\\D]*(\\d+).*$", Pattern.CASE_INSENSITIVE);
    
    private static final YtQuality [] VALUES = YtQuality.values();
    
    
    public static YtQuality find(final String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        if (value.equalsIgnoreCase("tiny")) return p144;
        if (value.equalsIgnoreCase("small")) return p240;
        if (value.equalsIgnoreCase("medium")) return p360;
        if (value.equalsIgnoreCase("large")) return p480;
        
        final Matcher matcher = VALUE_EXTRACTOR.matcher(value);
        if (matcher.find()) {
            final int lines = Integer.parseInt(matcher.group(1));
            return Arrays.stream(VALUES).filter(x -> x.lines == lines).findFirst().orElse(UNKNOWN);
        } else {
            return UNKNOWN;
        }
    }
    
    public int getLines(){
        return this.lines;
    }
    
    private final int lines;
    private YtQuality(final int lines) {
        this.lines = lines;
    }
    
}
