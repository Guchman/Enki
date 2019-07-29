package com.github.stcarolas.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import rocks.mango.gitea.OrganizationApi;
import rocks.mango.gitea.Repository;

@RequiredArgsConstructor
public class GiteaRepoProvider implements RepoProvider {

    private final String baseUrl;
    private final String organization;

    @Override
    public List<Repo> getRepos() {
        val organizations = Feign.builder()
            .decoder(
                new JacksonDecoder(Arrays.asList((Module) new JavaTimeModule()))
            ).target(OrganizationApi.class, baseUrl);
        return organizations
            .orgListRepos(organization)
            .stream()
            .map( repo -> convert(repo) )
            .collect(Collectors.toList());
    }

    public Repo convert(Repository repo){
        return Repo.builder()
            .name(repo.getName())
            .sshUrl(repo.getSshUrl())
            .build();
    }
    
}
