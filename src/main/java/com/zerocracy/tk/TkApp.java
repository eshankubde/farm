/**
 * Copyright (c) 2016 Zerocracy
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jcabi.log.VerboseProcess;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import org.takes.Take;
import org.takes.facets.flash.TkFlash;
import org.takes.facets.fork.FkFixed;
import org.takes.facets.fork.FkHitRefresh;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.facets.forward.TkForward;
import org.takes.tk.TkClasspath;
import org.takes.tk.TkFiles;
import org.takes.tk.TkGzip;
import org.takes.tk.TkMeasured;
import org.takes.tk.TkVersioned;
import org.takes.tk.TkWithHeaders;
import org.takes.tk.TkWithType;
import org.takes.tk.TkWrap;

/**
 * Takes application.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class TkApp extends TkWrap {

    /**
     * Ctor, for tests mostly.
     * @throws IOException If fails
     */
    TkApp() throws IOException {
        this(new Properties());
    }

    /**
     * Ctor.
     * @param props Properties
     * @param forks Additional forks
     * @throws IOException If fails
     */
    public TkApp(final Properties props, final FkRegex... forks)
        throws IOException {
        super(
            new TkWithHeaders(
                new TkVersioned(
                    new TkMeasured(
                        new TkGzip(
                            new TkFlash(
                                new TkAppAuth(
                                    new TkForward(
                                        TkApp.regex(props, forks)
                                    ),
                                    props
                                )
                            )
                        )
                    )
                ),
                String.format(
                    "X-Zerocracy-Version: %s %s %s",
                    props.getProperty("build.version"),
                    props.getProperty("build.revision"),
                    props.getProperty("build.date")
                ),
                "Vary: Cookie"
            )
        );
    }

    /**
     * Make it.
     * @param props Properties
     * @param forks Additional forks
     * @return Takes
     * @throws IOException If fails
     */
    private static Take regex(final Properties props,
        final FkRegex... forks) throws IOException {
        return new TkFork(
            Lists.newArrayList(
                Iterables.concat(
                    Arrays.asList(forks),
                    Arrays.asList(
                        new FkRegex("/", new TkIndex(props)),
                        new FkRegex("/robots.txt", ""),
                        new FkRegex(
                            "/xsl/[a-z\\-]+\\.xsl",
                            new TkWithType(
                                TkApp.refresh("./src/main/xsl"),
                                "text/xsl"
                            )
                        ),
                        new FkRegex(
                            "/css/[a-z]+\\.css",
                            new TkWithType(
                                TkApp.refresh("./src/main/scss"),
                                "text/css"
                            )
                        )
                    )
                )
            )
        );
    }

    /**
     * Hit refresh fork.
     * @param path Path of files
     * @return Fork
     * @throws IOException If fails
     */
    private static Take refresh(final String path) throws IOException {
        return new TkFork(
            new FkHitRefresh(
                new File(path),
                () -> new VerboseProcess(
                    new ProcessBuilder(
                        "mvn",
                        "generate-resources"
                    )
                ).stdout(),
                new TkFiles("./target/classes")
            ),
            new FkFixed(new TkClasspath())
        );
    }

}
