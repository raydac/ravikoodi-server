/*
 * Copyright 2018 Igor Maznitsa.
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

import com.igormaznitsa.ravikoodi.Utils;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsTest {

  @Test
  public void testCalculateNextKodiSpeedValue() {
    assertEquals(1, Utils.calculateNextKodiSpeedValue(0, true));
    assertEquals(-1, Utils.calculateNextKodiSpeedValue(0, false));
    
    assertEquals(1, Utils.calculateNextKodiSpeedValue(0, true));
    assertEquals(2, Utils.calculateNextKodiSpeedValue(1, true));
    assertEquals(4, Utils.calculateNextKodiSpeedValue(2, true));
    assertEquals(8, Utils.calculateNextKodiSpeedValue(4, true));
    assertEquals(32, Utils.calculateNextKodiSpeedValue(16, true));
    assertEquals(32, Utils.calculateNextKodiSpeedValue(32, true));

    assertEquals(-1, Utils.calculateNextKodiSpeedValue(0, false));
    assertEquals(0, Utils.calculateNextKodiSpeedValue(1, false));
    assertEquals(1, Utils.calculateNextKodiSpeedValue(2, false));
    assertEquals(2, Utils.calculateNextKodiSpeedValue(4, false));
    assertEquals(8, Utils.calculateNextKodiSpeedValue(16, false));
    assertEquals(16, Utils.calculateNextKodiSpeedValue(32, false));

    assertEquals(-32, Utils.calculateNextKodiSpeedValue(-32, false));
    assertEquals(-8, Utils.calculateNextKodiSpeedValue(-16, true));
    assertEquals(-32, Utils.calculateNextKodiSpeedValue(-16, false));
  }
  
}
