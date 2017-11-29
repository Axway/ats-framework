/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.log.report.model.html.css;

import com.axway.ats.log.report.model.html.css.FontStyle;
import com.axway.ats.log.report.model.html.css.FontWeight;
import com.axway.ats.log.report.model.html.css.TextHorizontalAlign;
import com.axway.ats.log.report.model.html.css.TextVerticalAlign;

/**
 * CSS definitions for a table row we use when creating an html report
 */
public class TableRowCssSettings {

    // css class name
    private String              className;

    // font properties
    private String              fontFamily;
    private int                 fontSize;
    private FontStyle           fontStyle;
    private FontWeight          fontWeight;

    // colors
    private String              textColor;
    private String              backgroundColor;

    // text alignment in a table cell
    private TextHorizontalAlign textHorizontalAlign;
    private TextVerticalAlign   textVerticalAlign;

    public TableRowCssSettings( String className ) {

        this.className = className;
    }

    public String getFontFamily() {

        return " font-family: " + fontFamily;
    }

    public void setFontFamily(
                               String fontFamily ) {

        this.fontFamily = fontFamily;
    }

    public String getFontSize() {

        return "font-size: " + fontSize + "px";
    }

    public void setFontSize(
                             int fontSize ) {

        this.fontSize = fontSize;
    }

    public String getFontStyle() {

        return "font-style: " + fontStyle.toString().toLowerCase();
    }

    public void setFontStyle(
                              FontStyle fontStyle ) {

        this.fontStyle = fontStyle;
    }

    public String getFontWeight() {

        return "font-weight: " + fontWeight.toString().toLowerCase();
    }

    public void setFontWeight(
                               FontWeight fontWeight ) {

        this.fontWeight = fontWeight;
    }

    public String getTextColor() {

        return "color: " + textColor;
    }

    public void setTextColor(
                              String textColor ) {

        this.textColor = textColor;
    }

    public String getBackgroundColor() {

        return "background-color: " + backgroundColor;
    }

    public void setBackgroundColor(
                                    String backgroundColor ) {

        this.backgroundColor = backgroundColor;
    }

    public String getTextHorizontalAlign() {

        return "text-align: " + textHorizontalAlign.toString().toLowerCase();
    }

    public void setTextHorizontalAlign(
                                        TextHorizontalAlign textHorizontalAlign ) {

        this.textHorizontalAlign = textHorizontalAlign;
    }

    public String getTextVerticalAlign() {

        return "vertical-align: " + textVerticalAlign.toString().toLowerCase();
    }

    public void setTextVerticalAlign(
                                      TextVerticalAlign textVerticalAlign ) {

        this.textVerticalAlign = textVerticalAlign;
    }

    /**
     * Get the html representation of this row
     * @return
     */
    public String toHtml() {

        StringBuilder sb = new StringBuilder();

        sb.append( "." );
        sb.append( this.className );
        sb.append( " {" );

        sb.append( getFontFamily() );
        sb.append( "; " );

        sb.append( getFontSize() );
        sb.append( "; " );

        sb.append( getFontStyle() );
        sb.append( "; " );

        sb.append( getFontWeight() );
        sb.append( "; " );

        sb.append( getTextColor() );
        sb.append( "; " );

        sb.append( getBackgroundColor() );
        sb.append( "; " );

        sb.append( getTextHorizontalAlign() );
        sb.append( "; " );

        sb.append( getTextVerticalAlign() );
        sb.append( "; " );

        sb.append( " }" );

        return sb.toString();
    }
}
