/*
 * Copyright 2022 Igor Maznitsa.
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

import java.awt.Font;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.UIManager;
import org.springframework.lang.NonNull;

public class TableLookupButton extends JButton {
    
    public TableLookupButton(@NonNull final String text) {
        super(Objects.requireNonNull(text));
        this.setFont(UIManager.getFont("Button.font").deriveFont(Font.BOLD));
        this.setFocusable(false);
        this.setBackground(UIManager.getColor("ComboBox.buttonBackground"));
        this.setBorder(UIManager.getBorder("ComboBox[button].border"));
    }
    
}
