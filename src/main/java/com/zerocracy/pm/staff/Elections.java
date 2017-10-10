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
package com.zerocracy.pm.staff;

import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSL;
import com.jcabi.xml.XSLDocument;
import com.zerocracy.Xocument;
import com.zerocracy.jstk.Item;
import com.zerocracy.jstk.Project;
import com.zerocracy.jstk.farm.fake.FkItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.cactoos.io.LengthOf;
import org.cactoos.io.TeeInput;
import org.xembly.Directives;

/**
 * Elections.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.12
 */
public final class Elections {

    /**
     * XSLT.
     */
    private static final XSL STYLESHEET = XSLDocument.make(
        Elections.class.getResourceAsStream("to-winner.xsl")
    );

    /**
     * Project.
     */
    private final Project project;

    /**
     * Ctor.
     * @param pkt Project
     */
    public Elections(final Project pkt) {
        this.project = pkt;
    }

    /**
     * Bootstrap it.
     * @return Itself
     * @throws IOException If fails
     */
    public Elections bootstrap() throws IOException {
        try (final Item team = this.item()) {
            new Xocument(team).bootstrap("pm/staff/elections");
        }
        return this;
    }

    /**
     * Elect a performer.
     * @param job The job to elect
     * @param logins Candidates
     * @param voters Voters and weights
     * @return TRUE if a new election made some changes to the situation
     * @throws IOException If fails
     * @checkstyle ExecutableStatementCountCheck (200 lines)
     */
    public boolean elect(final String job, final Iterable<String> logins,
        final Map<Voter, Integer> voters) throws IOException {
        final String state = this.state(job);
        final String date = ZonedDateTime.now().format(
            DateTimeFormatter.ISO_INSTANT
        );
        final Directives dirs = new Directives()
            .xpath(
                String.format(
                    "/elections[not(job[@id='%s'])]",
                    job
                )
            )
            .add("job")
            .attr("id", job)
            .xpath(
                String.format(
                    "/elections/job[@id='%s' ]",
                    job
                )
            )
            .strict(1)
            .add("election")
            .attr("date", date);
        final StringBuilder log = new StringBuilder(0);
        for (final Map.Entry<Voter, Integer> ent : voters.entrySet()) {
            dirs.add("vote")
                .attr("author", ent.getKey().getClass().getName())
                .attr("weight", ent.getValue());
            for (final String login : logins) {
                log.setLength(0);
                dirs.add("person")
                    .attr("login", login)
                    .attr("points", ent.getKey().vote(login, log))
                    .set(log.toString())
                    .up();
            }
            dirs.up();
        }
        try (final Item item = this.item()) {
            final Path path = item.path();
            final Path temp = Files.createTempFile("elections", ".xml");
            new LengthOf(new TeeInput(path, temp)).value();
            final Project pkt = file -> new FkItem(temp);
            new Xocument(temp).modify(dirs);
            boolean modified = true;
            if (new Elections(pkt).state(job).equals(state)) {
                new Xocument(temp).modify(
                    new Directives().xpath(
                        String.format(
                            "/elections/job[@id='%s']/election[@date='%s']",
                            job, date
                        )
                    ).remove()
                );
                modified = false;
            }
            if (modified) {
                new LengthOf(new TeeInput(temp, path)).value();
            }
            return modified;
        }
    }

    /**
     * Remove the entire election for the job.
     * @param job The job
     * @throws IOException If fails
     */
    public void remove(final String job) throws IOException {
        try (final Item roles = this.item()) {
            new Xocument(roles).modify(
                new Directives().xpath(
                    String.format(
                        "/elections/job[@id='%s']",
                        job
                    )
                ).remove()
            );
        }
    }

    /**
     * Iterate election jobs.
     * @return Job list
     * @throws IOException If failed
     */
    public List<String> jobs() throws IOException {
        try (final Item roles = this.item()) {
            return new Xocument(roles).xpath("/elections/job/@id");
        }
    }

    /**
     * Retrieve TRUE if at least one election happened already.
     * @param job The job
     * @return TRUE if an election exists
     * @throws IOException If fails
     */
    public boolean exists(final String job) throws IOException {
        try (final Item item = this.item()) {
            return !new XMLDocument(item.path().toFile()).nodes(
                String.format(
                    "/elections/job[ @id ='%s']/election[ last()]", job
                )
            ).isEmpty();
        }
    }

    /**
     * Retrieve TRUE if the performer is elected for the job.
     * @param job The job
     * @return TRUE if it has a winner
     * @throws IOException If fails
     */
    public boolean elected(final String job) throws IOException {
        boolean elected = this.exists(job);
        if (elected) {
            try (final Item item = this.item()) {
                elected = Elections.STYLESHEET.transform(
                    new XMLDocument(item.path().toFile()).nodes(
                        String.format(
                            "/elections/job[@id ='%s']/election[ last()]", job
                        )
                    ).get(0)
                ).nodes("/summary/winner").iterator().hasNext();
            }
        }
        return elected;
    }

    /**
     * Retrieve the reason for the given job.
     * @param job The job
     * @return The reason
     * @throws IOException If fails
     */
    public String reason(final String job) throws IOException {
        try (final Item item = this.item()) {
            return Elections.STYLESHEET.transform(
                new XMLDocument(item.path().toFile()).nodes(
                    String.format(
                        "/elections/job[@id ='%s']/election[last()]", job
                    )
                ).get(0)
            ).xpath("/summary/reason/text()").get(0);
        }
    }

    /**
     * Retrieve the reason for the given job.
     * @param job The job
     * @return The reason
     * @throws IOException If fails
     */
    public String winner(final String job) throws IOException {
        try (final Item item = this.item()) {
            return Elections.STYLESHEET.transform(
                new XMLDocument(item.path().toFile()).nodes(
                    String.format(
                        "/elections/job[@id='%s']/election[last()]", job
                    )
                ).get(0)
            ).xpath("/summary/winner/text()").get(0);
        }
    }

    /**
     * Current state with this job.
     * @param job The job
     * @return State, encrypted
     * @throws IOException If fails
     */
    private String state(final String job) throws IOException {
        final StringBuilder state = new StringBuilder(0);
        state.append(this.exists(job)).append(' ').append(this.elected(job));
        if (this.elected(job)) {
            state.append(' ').append(this.winner(job));
        }
        return state.toString();
    }

    /**
     * The item.
     * @return Item
     * @throws IOException If fails
     */
    private Item item() throws IOException {
        return this.project.acq("elections.xml");
    }

}
