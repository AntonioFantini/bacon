package org.jboss.pnc.bacon.pnc;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

@Slf4j
@Command(
        name = "artifact",
        description = "Artifact",
        subcommands = {
                ArtifactCli.Get.class,
                ArtifactCli.GetGav.class,
                ArtifactCli.ListFromHash.class,
                ArtifactCli.Usage.class,
                ArtifactCli.UsageGav.class })
public class ArtifactCli {

    private static final ClientCreator<ArtifactClient> CREATOR = new ClientCreator<>(ArtifactClient::new);

    @Command(
            name = "get",
            description = "Get an artifact by its id",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact get 10")
    public static class Get extends AbstractGetSpecificCommand<Artifact> {

        @Override
        public Artifact getSpecific(String id) throws ClientException {
            try (ArtifactClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(
            name = "get-gav",
            description = "Get artifact by its identifier/GAV. Identifier is usually in format of 'groupId:artifactId:classifier:version'",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifacts get-gav args4j:args4j:jar:2.0.16")
    public static class GetGav extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Parameters(description = "Identifier of artifact")
        private String identifier;

        @Override
        public Integer call() throws Exception {
            if (identifier == null) {
                log.error("You need to specify artifact identifier/gav");
            }
            try (ArtifactClient client = CREATOR.newClient()) {
                ObjectHelper.print(
                        getJsonOutput(),
                        client.getAll(
                                null,
                                null,
                                null,
                                Optional.empty(),
                                Optional.ofNullable("identifier==" + identifier)).iterator().next());
                return 0;
            }
        }
    }

    @Command(
            name = "list-from-hash",
            description = "List artifacts based on hash",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact list-from-hash --md5 stiritup")
    public static class ListFromHash extends JSONCommandHandler implements Callable<Integer> {
        @Option(names = "--md5")
        private String md5;

        @Option(names = "--sha1")
        private String sha1;

        @Option(names = "--sha256")
        private String sha256;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            if (md5 == null && sha1 == null && sha256 == null) {
                log.error("You need to use at least one hash option!");
                return 1;
            } else {
                try (ArtifactClient client = CREATOR.newClient()) {
                    ObjectHelper.print(getJsonOutput(), client.getAll(sha256, md5, sha1));
                    return 0;
                }
            }
        }
    }

    @Command(
            name = "usage",
            description = "Get the list of builds using the artifact",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact usage 10")
    public static class Usage extends AbstractListCommand<Build> {

        @CommandLine.Parameters(description = "Id of artifact")
        private String id;

        @Override
        public Collection<Build> getAll(String sort, String query) throws RemoteResourceException {
            try (ArtifactClient client = CREATOR.newClient()) {
                return client.getDependantBuilds(id, Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

    @Command(
            name = "usage-gav",
            description = "Get the list of builds using the artifact by artifact identifier/gav",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact usage-gav args4j:args4j:jar:2.0.16")
    public static class UsageGav extends AbstractListCommand<Build> {

        @CommandLine.Parameters(description = "Identifier of artifact")
        private String identifier;

        @Override
        public Collection<Build> getAll(String sort, String query) throws RemoteResourceException {
            try (ArtifactClient client = CREATOR.newClient()) {
                Artifact a = client
                        .getAll(null, null, null, Optional.empty(), Optional.ofNullable("identifier==" + identifier))
                        .iterator()
                        .next();
                return client.getDependantBuilds(a.getId(), Optional.ofNullable(sort), Optional.ofNullable(query))
                        .getAll();
            }
        }
    }
}
