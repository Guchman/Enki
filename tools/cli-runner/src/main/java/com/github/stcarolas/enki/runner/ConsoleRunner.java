package com.github.stcarolas.enki.runner;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.stcarolas.enki.bitbucket.provider.BitbucketRepoProvider;
import com.github.stcarolas.enki.core.EnkiRunner;
import com.github.stcarolas.enki.core.EnkiRunner.EnkiRunnerBuilder;
import com.github.stcarolas.enki.core.RepoHandler;
import com.github.stcarolas.enki.github.provider.GitHubRepoProvider;

import org.xeustechnologies.jcl.JarClassLoader;

import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "enki",
    mixinStandardHelpOptions = true,
    version = "0.2",
    description = "run enki handlers from cli"
)
@Log4j2
public class ConsoleRunner implements Callable<Integer> {

    @Option(names = { "--github" }, description = "use GitHub RepoProvider")
    private boolean useGithubProvider = false;

    @Option(names = { "--github-username" }, description = "GitHub Username")
    private String githubUser = "";

    @Option(names = { "--github-organization" }, description = "GitHub Organization")
    private String githubOrg = "";

    @Option(
        names = { "--github-password" },
        description = "GitHub Password or Personal Access Token"
    )
    private String githubPassword = "";

    @Option(
        names = { "--bitbucket" }, 
        description = "use Bitbucket RepoProvider"
    )
    private boolean useBitbucketProvider = false;

    @Option(
        names = { "--bitbucket-endpoint" },
        description = "Bitbucket REST API Endpoint"
    )
    private String bitbucketEndpoint = "";

    @Option(
        names = { "--bitbucket-token" }, 
        description = "Bitbucket Access Token"
    )
    private String bitbucketToken = "";

    @Option(names = { "--gitlab" }, description = "use GitLab RepoProvider")
    private boolean useGitlabProvider = false;

    @Option(names = { "--gitea" }, description = "use Gitea RepoProvider")
    private boolean useGiteaProvider = false;

    @Parameters(index = "0", description = "jar with handlers")
    private String jar;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ConsoleRunner()).execute(args);
        System.exit(exitCode);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Integer call() throws Exception {
        Set<String> classes = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                String fileName = ((ZipEntry) zipEntries.nextElement()).getName();
                log.debug("seek {}", fileName);
                if (fileName.endsWith("class")) {
                    String className = fileName.replace("/", ".").replace(".class", "");
                    log.debug("detect {} as {}", fileName, className);
                    classes.add(className);
                }
            }
        }
        JarClassLoader jcl = new JarClassLoader();
        jcl.add(jar);
        Set<Class> handlers = new HashSet<>();
        for (String className : classes) {
            Class loadedClass = jcl.loadClass(className);
            if (Arrays.asList(loadedClass.getInterfaces()).contains(RepoHandler.class)) {
                log.info("Use {}", loadedClass.getName());
                handlers.add(loadedClass);
            }
        }

        EnkiRunnerBuilder enki = EnkiRunner.builder();
        if (useGithubProvider) {
            enki.provider(
                GitHubRepoProvider.builder()
                    .username(githubUser)
                    .password(githubPassword)
                    .organization(githubOrg)
                    .build()
            );
        }
        if (useBitbucketProvider) {
            enki.provider(
                BitbucketRepoProvider.builder()
                    .token(bitbucketToken)
                    .endpoint(bitbucketEndpoint)
                    .build()
            );
        }
        for (Class handler : handlers) {
            enki.handler((RepoHandler) handler.newInstance());
        }
        enki.build().handle();

        return 0;
    }
}
