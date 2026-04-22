package com.test.example.api.rest;

import com.test.example.domain.Commit;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommitInfoController {

  private String commitMessage = "";
  private String branch = "";
  private String commitId = "";
  private String commitTime = "";

  // Load git.properties manually rather than binding via @Value("${git.commit.message.full}"):
  // @Value recursively resolves ${...} placeholders inside its own resolved result, so a commit
  // message containing literal ${VAR} tokens (e.g. build docs) fails context startup with
  // "Could not resolve placeholder 'VAR'".
  @PostConstruct
  void load() {
    Properties props = new Properties();
    try (InputStream in = new ClassPathResource("git.properties").getInputStream()) {
      props.load(in);
      commitMessage = props.getProperty("git.commit.message.full", "");
      branch = props.getProperty("git.branch", "");
      commitId = props.getProperty("git.commit.id.full", "");
      commitTime = props.getProperty("git.commit.time", "");
    } catch (IOException ignored) {
      // git.properties not on classpath (e.g. running outside a Maven-packaged jar).
    }
  }

  @RequestMapping(
      value = "/commitid",
      method = RequestMethod.GET,
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.OK)
  public @ResponseBody ResponseEntity<?> getCommitId() {
    List<Commit> commits = new ArrayList<>();
    commits.add(new Commit(commitId, commitMessage, branch, commitTime));
    return new ResponseEntity<>(commits, HttpStatus.OK);
  }
}
