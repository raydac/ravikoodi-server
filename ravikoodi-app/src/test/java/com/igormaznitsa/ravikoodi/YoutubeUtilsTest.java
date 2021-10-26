/*
 * Copyright 2021 Igor Maznitsa.
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

import org.junit.Test;
import static org.junit.Assert.*;

public class YoutubeUtilsTest {
    
    @Test
    public void testExtractYoutubeVideoId_NoId() {
        assertTrue(YoutubeUtils.extractYoutubeVideoId("http://google.com").isEmpty());
        assertTrue(YoutubeUtils.extractYoutubeVideoId("").isEmpty());
        assertTrue(YoutubeUtils.extractYoutubeVideoId("jkhsfd").isEmpty());
    }
    
    @Test
    public void testExtractYoutubeVideoId_IdExists() {
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://www.youtube.com/watch?v=blAX3y2pmcs").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://www.youtube.com/watch?v=blAX3y2pmcs").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://www.youtube.com/watch?V=blAX3y2pmcs+dds").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://www.youtube.com/watch?V=blAX3y2pmcs dsd").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://www.youtube.com/watch?v=blAX3y2pmcs&sound=1").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://www.youtube.com/watch?sound=1&v=blAX3y2pmcs&some=v").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://youtu.be/blAX3y2pmcs").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("http://youtu.be/blAX3y2pmcs").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://youtu.be/blAX3y2pmcs?s=0").orElseThrow());
        assertEquals("blAX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://youtu.be/blAX3y2pmcs?s=0&be=you").orElseThrow());
        assertEquals("bl-AX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://youtu.be/bl-AX3y2pmcs?s=0").orElseThrow());
        assertEquals("bl_AX3y2pmcs",YoutubeUtils.extractYoutubeVideoId("https://youtu.be/bl_AX3y2pmcs?s=0&be=you").orElseThrow());
    }

    @Test
    public void testExtractYoutubePlaylistId_NoId() {
        assertTrue(YoutubeUtils.extractYoutubePlaylistId("http://google.com").isEmpty());
        assertTrue(YoutubeUtils.extractYoutubePlaylistId("").isEmpty());
        assertTrue(YoutubeUtils.extractYoutubePlaylistId("jkhsfd").isEmpty());
    }
    
    @Test
    public void testExtractYoutubePlaylistId_IdExists() {
        assertEquals("PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S",YoutubeUtils.extractYoutubePlaylistId("https://www.youtube.com/watch?v=UyN_mamhHlg&list=PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S").orElseThrow());
        assertEquals("PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S",YoutubeUtils.extractYoutubePlaylistId("https://www.youtube.com/playlist?list=PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S&s=1").orElseThrow());
        assertEquals("PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S",YoutubeUtils.extractYoutubePlaylistId("https://www.youtube.com/playlist?list=PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S").orElseThrow());
        assertEquals("PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S",YoutubeUtils.extractYoutubePlaylistId("http://youtube.com/playlist?list=PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S").orElseThrow());
        assertEquals("PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S",YoutubeUtils.extractYoutubePlaylistId("http://youtu.be?list=PLOl4b517qn8hmH4Pe0_P8-Y723NmtL08S").orElseThrow());
    }
    
}
