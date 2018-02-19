/*
 * Copyright (c) 2018 Ronald Brill.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.css.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.stylesheets.MediaList;
import org.w3c.dom.stylesheets.StyleSheet;

import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSOMParser;
import com.gargoylesoftware.css.parser.InputSource;
import com.gargoylesoftware.css.parser.media.MediaQueryList;
import com.gargoylesoftware.css.util.LangUtils;
import com.gargoylesoftware.css.util.ThrowCssExceptionErrorHandler;

/**
 * Implementation of {@link CSSStyleSheet}.
 *
 * @author Ronald Brill
 */
public class CSSStyleSheetImpl implements CSSStyleSheet, Serializable {

    private boolean disabled_;
    private Node ownerNode_;
    private StyleSheet parentStyleSheet_;
    private String href_;
    private String title_;
    private MediaList media_;
    private CSSRule ownerRule_;
    private boolean readOnly_;
    private CSSRuleList cssRules_;
    private String baseUri_;

    public void setMedia(final MediaList media) {
        media_ = media;
    }

    private String getBaseUri() {
        return baseUri_;
    }

    public void setBaseUri(final String baseUri) {
        baseUri_ = baseUri;
    }

    public CSSStyleSheetImpl() {
        super();
    }

    @Override
    public String getType() {
        return "text/css";
    }

    @Override
    public boolean getDisabled() {
        return disabled_;
    }

    /**
     * We will need to respond more fully if a stylesheet is disabled, probably
     * by generating an event for the main application.
     */
    @Override
    public void setDisabled(final boolean disabled) {
        disabled_ = disabled;
    }

    @Override
    public Node getOwnerNode() {
        return ownerNode_;
    }

    @Override
    public StyleSheet getParentStyleSheet() {
        return parentStyleSheet_;
    }

    @Override
    public String getHref() {
        return href_;
    }

    @Override
    public String getTitle() {
        return title_;
    }

    @Override
    public MediaList getMedia() {
        return media_;
    }

    @Override
    public CSSRule getOwnerRule() {
        return ownerRule_;
    }

    @Override
    public CSSRuleList getCssRules() {
        if (cssRules_ == null) {
            cssRules_ = new CSSRuleListImpl();
        }
        return cssRules_;
    }

    @Override
    public int insertRule(final String rule, final int index) throws DOMException {
        if (readOnly_) {
            throw new DOMExceptionImpl(
                DOMException.NO_MODIFICATION_ALLOWED_ERR,
                DOMExceptionImpl.READ_ONLY_STYLE_SHEET);
        }

        try {
            final InputSource is = new InputSource(new StringReader(rule));
            final CSSOMParser parser = new CSSOMParser();
            parser.setParentStyleSheet(this);
            parser.setErrorHandler(ThrowCssExceptionErrorHandler.INSTANCE);
            final CSSRule r = parser.parseRule(is);

            if (r == null) {
                // this should neven happen because of the ThrowCssExceptionErrorHandler
                throw new DOMExceptionImpl(
                        DOMException.SYNTAX_ERR,
                        DOMExceptionImpl.SYNTAX_ERROR,
                        "Parsing rule '" + rule + "' failed.");
            }

            if (getCssRules().getLength() > 0) {
                // We need to check that this type of rule can legally go into
                // the requested position.
                int msg = -1;
                if (r.getType() == CSSRule.CHARSET_RULE) {

                    // Index must be 0, and there can be only one charset rule
                    if (index != 0) {
                        msg = DOMExceptionImpl.CHARSET_NOT_FIRST;
                    }
                    else if (getCssRules().item(0).getType() == CSSRule.CHARSET_RULE) {
                        msg = DOMExceptionImpl.CHARSET_NOT_UNIQUE;
                    }
                }
                else if (r.getType() == CSSRule.IMPORT_RULE) {
                    // Import rules must preceed all other rules (except
                    // charset rules)
                    if (index <= getCssRules().getLength()) {
                        for (int i = 0; i < index; i++) {
                            final int rt = getCssRules().item(i).getType();
                            if ((rt != CSSRule.CHARSET_RULE) && (rt != CSSRule.IMPORT_RULE)) {
                                msg = DOMExceptionImpl.IMPORT_NOT_FIRST;
                                break;
                            }
                        }
                    }
                }
                else {
                    if (index <= getCssRules().getLength()) {
                        for (int i = index; i < getCssRules().getLength(); i++) {
                            final int rt = getCssRules().item(i).getType();
                            if ((rt == CSSRule.CHARSET_RULE) || (rt == CSSRule.IMPORT_RULE)) {
                                msg = DOMExceptionImpl.INSERT_BEFORE_IMPORT;
                                break;
                            }
                        }
                    }
                }
                if (msg > -1) {
                    throw new DOMExceptionImpl(DOMException.HIERARCHY_REQUEST_ERR, msg);
                }
            }

            // Insert the rule into the list of rules
            ((CSSRuleListImpl) getCssRules()).insert(r, index);

        }
        catch (final IndexOutOfBoundsException e) {
            throw new DOMExceptionImpl(
                DOMException.INDEX_SIZE_ERR,
                DOMExceptionImpl.INDEX_OUT_OF_BOUNDS,
                e.getMessage());
        }
        catch (final CSSException e) {
            throw new DOMExceptionImpl(
                DOMException.SYNTAX_ERR,
                DOMExceptionImpl.SYNTAX_ERROR,
                e.getMessage());
        }
        catch (final IOException e) {
            throw new DOMExceptionImpl(
                DOMException.SYNTAX_ERR,
                DOMExceptionImpl.SYNTAX_ERROR,
                e.getMessage());
        }
        return index;
    }

    @Override
    public void deleteRule(final int index) throws DOMException {
        if (readOnly_) {
            throw new DOMExceptionImpl(
                DOMException.NO_MODIFICATION_ALLOWED_ERR,
                DOMExceptionImpl.READ_ONLY_STYLE_SHEET);
        }

        try {
            ((CSSRuleListImpl) getCssRules()).delete(index);
        }
        catch (final IndexOutOfBoundsException e) {
            throw new DOMExceptionImpl(
                DOMException.INDEX_SIZE_ERR,
                DOMExceptionImpl.INDEX_OUT_OF_BOUNDS,
                e.getMessage());
        }
    }

    public boolean isReadOnly() {
        return readOnly_;
    }

    public void setReadOnly(final boolean b) {
        readOnly_ = b;
    }

    public void setOwnerNode(final Node ownerNode) {
        ownerNode_ = ownerNode;
    }

    public void setParentStyleSheet(final StyleSheet parentStyleSheet) {
        parentStyleSheet_ = parentStyleSheet;
    }

    public void setHref(final String href) {
        href_ = href;
    }

    public void setTitle(final String title) {
        title_ = title;
    }

    public void setMediaText(final String mediaText) {
        final InputSource source = new InputSource(new StringReader(mediaText));
        try {
            final CSSOMParser parser = new CSSOMParser();
            final MediaQueryList sml = parser.parseMedia(source);
            media_ = new MediaListImpl(sml);
        }
        catch (final IOException e) {
            // TODO handle exception
        }
    }

    public void setOwnerRule(final CSSRule ownerRule) {
        ownerRule_ = ownerRule;
    }

    public void setCssRules(final CSSRuleList rules) {
        cssRules_ = rules;
    }

    @Override
    public String toString() {
        return getCssRules().toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CSSStyleSheet)) {
            return false;
        }
        final CSSStyleSheet css = (CSSStyleSheet) obj;
        boolean eq = LangUtils.equals(getCssRules(), css.getCssRules());
        eq = eq && (getDisabled() == css.getDisabled());
        eq = eq && LangUtils.equals(getHref(), css.getHref());
        eq = eq && LangUtils.equals(getMedia(), css.getMedia());
        // TODO implement some reasonful equals method for ownerNode
//        eq = eq && Utils.equals(getOwnerNode(), css.getOwnerNode());
            // don't use ownerNode and parentStyleSheet in equals()
            // recursive loop -> stack overflow!
        eq = eq && LangUtils.equals(getTitle(), css.getTitle());
        return eq;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, baseUri_);
        hash = LangUtils.hashCode(hash, cssRules_);
        hash = LangUtils.hashCode(hash, disabled_);
        hash = LangUtils.hashCode(hash, href_);
        hash = LangUtils.hashCode(hash, media_);
        hash = LangUtils.hashCode(hash, ownerNode_);
        // don't use ownerNode and parentStyleSheet in hashCode()
        // recursive loop -> stack overflow!
        hash = LangUtils.hashCode(hash, readOnly_);
        hash = LangUtils.hashCode(hash, title_);
        return hash;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.writeObject(baseUri_);
        out.writeObject(cssRules_);
        out.writeBoolean(disabled_);
        out.writeObject(href_);
        out.writeObject(media_);
        // TODO ownerNode may not be serializable!
//        out.writeObject(ownerNode);
        out.writeBoolean(readOnly_);
        out.writeObject(title_);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        baseUri_ = (String) in.readObject();
        cssRules_ = (CSSRuleList) in.readObject();
        if (cssRules_ != null) {
            for (int i = 0; i < cssRules_.getLength(); i++) {
                final CSSRule cssRule = cssRules_.item(i);
                if (cssRule instanceof AbstractCSSRuleImpl) {
                    ((AbstractCSSRuleImpl) cssRule).setParentStyleSheet(this);
                }
            }
        }
        disabled_ = in.readBoolean();
        href_ = (String) in.readObject();
        media_ = (MediaList) in.readObject();
        // TODO ownerNode may not be serializable!
//        ownerNode = (Node) in.readObject();
        readOnly_ = in.readBoolean();
        title_ = (String) in.readObject();
    }

    /**
     * Imports referenced CSSStyleSheets.
     *
     * @param recursive <code>true</code> if the import should be done
     *   recursively, <code>false</code> otherwise
     */
    public void importImports(final boolean recursive) throws DOMException {
        for (int i = 0; i < getCssRules().getLength(); i++) {
            final CSSRule cssRule = getCssRules().item(i);
            if (cssRule.getType() == CSSRule.IMPORT_RULE) {
                final CSSImportRule cssImportRule = (CSSImportRule) cssRule;
                try {
                    final URI importURI = new URI(getBaseUri()).resolve(cssImportRule.getHref());
                    final CSSStyleSheetImpl importedCSS = (CSSStyleSheetImpl)
                        new CSSOMParser().parseStyleSheet(
                                new InputSource(importURI.toString()), null, importURI.toString());
                    if (recursive) {
                        importedCSS.importImports(recursive);
                    }
                    final MediaList mediaList = cssImportRule.getMedia();
                    if (mediaList.getLength() == 0) {
                        mediaList.appendMedium("all");
                    }
                    final CSSMediaRuleImpl cssMediaRule = new CSSMediaRuleImpl(this, null, mediaList);
                    cssMediaRule.setRuleList((CSSRuleListImpl) importedCSS.getCssRules());
                    deleteRule(i);
                    ((CSSRuleListImpl) getCssRules()).insert(cssMediaRule, i);
                }
                catch (final URISyntaxException e) {
                    throw new DOMException(DOMException.SYNTAX_ERR, e.getLocalizedMessage());
                }
                catch (final IOException e) {
                    // TODO handle exception
                }
            }
        }
    }
}
