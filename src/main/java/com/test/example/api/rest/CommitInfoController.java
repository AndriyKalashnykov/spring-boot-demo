package com.test.example.api.rest;

import com.test.example.domain.Commit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CommitInfoController {

    @Value("${git.commit.message.full}")
    private String commitMessage;

    @Value("${git.branch}")
    private String branch;

    @Value("${git.commit.id.full}")
    private String commitId;

    @RequestMapping(value = "/commitid",
            method = RequestMethod.GET,
            produces = {"application/json", "application/xml"})
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody ResponseEntity<?> getCommitId() {
        List<Commit> commits = new ArrayList<Commit>();
        commits.add(new Commit(commitId, commitMessage, branch));
        return new ResponseEntity(commits, HttpStatus.OK);
    }
}