package org.example;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class Question {

    public int question_number;
    public String question;
    public List<Map<String, Integer>> options;
    public int answer;
}