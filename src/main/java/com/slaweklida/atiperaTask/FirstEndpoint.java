package com.slaweklida.atiperaTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InaccessibleObjectException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FirstEndpoint {
    @GetMapping("/{username}")
    public String getUser(@PathVariable("username") String username) throws IOException {
        String command = "powershell.exe gh api /users/" + username + "/repos --jq 'map(select(.fork == false))|.[]|{Repository_name: .name, Owner_login: .owner.login}'";
        Process powerShellProcess = Runtime.getRuntime().exec(command);
        powerShellProcess.getOutputStream().close();

        String text = "";
        String line;
        String branchLine;
        System.out.println("Standard Output:");
        BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
        while ((line = stdout.readLine()) != null) {
            text += line;

            String branchesCommand = "powershell.exe gh api /repos/" + username + "/" + catchRepoName(line) + "/branches --jq '.[]|{Branch_name: .name}'";
            Process powerShellProcessBranches = Runtime.getRuntime().exec(branchesCommand);
            powerShellProcessBranches.getOutputStream().close();
            BufferedReader branchesOut = new BufferedReader(new InputStreamReader(powerShellProcessBranches.getInputStream()));
            while ((branchLine = branchesOut.readLine()) != null)
                text += (branchLine);

            String commitsCommand = "powershell.exe gh api /repos/" + username + "/" + catchRepoName(line) + "/commits --jq '.[0]|{SHA: .sha}'";
            Process powerShellProcessCommits = Runtime.getRuntime().exec(commitsCommand);
            powerShellProcessCommits.getOutputStream().close();
            BufferedReader commitsOut = new BufferedReader(new InputStreamReader(powerShellProcessCommits.getInputStream()));
            while ((line = commitsOut.readLine()) != null)
                text += (line + "<br>");
        }
        stdout.close();

        System.out.println("Standard Error:");
        BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
        while ((line = stderr.readLine()) != null) text += (line + "<br>");
        stderr.close();
        System.out.println("Done");

//        if (text.equals("{\"message\":\"Not Found\",\"documentation_url\":\"https://docs.github.com/rest/reference/repos#list-repositories-for-a-user\"}gh: Not Found (HTTP 404)"))
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        else
            return text;
    }

    public String catchRepoName(String line) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(line);
            return jsonNode.get("Repository_name").asText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return line;
    }
}
