/**
 * Copyright (c) 2016-2017 Zerocracy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permissions is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.zerocracy.tk;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSLDocument;
import com.zerocracy.jstk.Item;
import com.zerocracy.jstk.Project;
import java.io.IOException;
import java.net.URI;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.takes.rs.xe.XeAppend;
import org.takes.rs.xe.XeSource;
import org.xembly.Directive;

/**
 * XeSource through XSL.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.12
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class XeXsl implements XeSource {

    /**
     * Project.
     */
    private final Project project;

    /**
     * Item.
     */
    private final String name;

    /**
     * XSL stylesheet name.
     */
    private final String xsl;

    /**
     * Ctor.
     * @param pkt Project
     * @param itm Item
     * @param sheet XSL stylesheet
     */
    public XeXsl(final Project pkt, final String itm, final String sheet) {
        this.project = pkt;
        this.name = itm;
        this.xsl = sheet;
    }

    @Override
    public Iterable<Directive> toXembly() throws IOException {
        try (final Item item = this.project.acq(this.name)) {
            final String content;
            if (item.path().toFile().length() == 0L) {
                content = "<p>The document is empty yet.</p>";
            } else {
                final XML xml = new XMLDocument(item.path().toFile());
                content = new XSLDocument(
                    new TextOf(
                        new InputOf(
                            URI.create(
                                String.format(
                                    "http://datum.zerocracy.com/latest/xsl/%s",
                                    this.xsl
                                )
                            )
                        )
                    ).asString()
                ).transform(xml).nodes("/*/xhtml:body").get(0).toString();
            }
            return new XeAppend("xml", content).toXembly();
        }
    }
}
