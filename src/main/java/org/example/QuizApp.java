package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@AllArgsConstructor
@Getter
public class QuizApp {
    public List<Question> questions;
    public int current = 0;
    public int correct = 0;
    public int incorrect = 0;
    private long quizStartTime;

    private JFrame frame;
    private JTextArea questionArea;
    private JLabel answerLabel;
    private JTextField answerField;
    private JLabel resultLabel;
    private JProgressBar progressBar;
    private JButton replayButton;

    private Timer countdownTimer;
    private final int questionTimeMs = 30000;
    private final int timerIntervalMs = 100;
    public int timeLeftMs;
    private final ObjectMapper mapper;

    public QuizApp(ObjectMapper mapper) throws Exception {
        this.mapper = mapper;
        loadQuestions();
        Collections.shuffle(questions);
        setupUI();
        quizStartTime = System.currentTimeMillis();
        showQuestion();
    }

    private void loadQuestions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getClassLoader().getResourceAsStream("questions.json");
        JsonNode root = mapper.readTree(is);
        questions = Arrays.asList(mapper.treeToValue(root.get("questions"), Question[].class));
    }

    private void setupUI() {
        frame = new JFrame("Quiz App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        questionArea = new JTextArea(5, 40);
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Serif", Font.PLAIN, 16));
        answerField = new JTextField(5);
        resultLabel = new JLabel("Type the number of your answer and press Enter.");
        progressBar = new JProgressBar(0, questionTimeMs);
        progressBar.setForeground(Color.GREEN);
        progressBar.setStringPainted(false);

        answerField.addActionListener(this::handleAnswer);

        replayButton = new JButton("Replay Quiz");
        replayButton.setVisible(false);
        replayButton.addActionListener(e -> restartQuiz());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(questionArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        answerLabel = new JLabel("Answer:");
        inputPanel.add(answerLabel);
        inputPanel.add(answerField);
        inputPanel.add(replayButton);

        panel.add(inputPanel, BorderLayout.SOUTH);
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(resultLabel, BorderLayout.EAST);

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private void showQuestion() {
        if (current >= questions.size()) {
            long durationMillis = System.currentTimeMillis() - quizStartTime;
            String durationFormatted = getDurationFormatted(durationMillis);
            questionArea.setText("Quiz Finished!\nCorrect: " + correct + "\nWrong: " + incorrect + "\nDuration: " + durationFormatted);
            answerLabel.setVisible(false);
            answerField.setEnabled(false);
            answerField.setVisible(false);
            replayButton.setVisible(true);
            resultLabel.setText("Done.");
            progressBar.setValue(0);
            progressBar.setForeground(Color.GRAY);

            frame.getRootPane().setDefaultButton(replayButton);  // Enter triggers replay
            saveResult(durationFormatted, "quiz_results.json");
            return;
        }

        Question q = questions.get(current);
        StringBuilder sb = new StringBuilder();
        sb.append("Q").append(q.question_number).append(": ").append(q.question).append("\n\n");
        for (Map<String, Integer> opt : q.options) {
            for (Map.Entry<String, Integer> entry : opt.entrySet()) {
                sb.append(entry.getValue()).append(". ").append(entry.getKey()).append("\n\n");
            }
        }
        questionArea.setText(sb.toString());

        timeLeftMs = questionTimeMs;
        progressBar.setMaximum(questionTimeMs);
        progressBar.setValue(timeLeftMs);
        progressBar.setForeground(Color.GREEN);

        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timer(timerIntervalMs, e -> updateCountdown());
        countdownTimer.start();

        frame.getRootPane().setDefaultButton(null); // Clear default to allow text input
        SwingUtilities.invokeLater(() -> answerField.requestFocusInWindow());
    }

    private void updateCountdown() {
        timeLeftMs -= timerIntervalMs;
        if (timeLeftMs <= 0) {
            countdownTimer.stop();
            resultLabel.setText("Time's up! Correct was: " + questions.get(current).answer);
            incorrect++;
            current++;
            answerField.setText("");
            SwingUtilities.invokeLater(this::showQuestion);
            return;
        }

        progressBar.setValue(timeLeftMs);

        float fraction = (float) timeLeftMs / questionTimeMs;
        if (fraction < 0.3f) {
            progressBar.setForeground(Color.RED);
        } else if (fraction < 0.6f) {
            progressBar.setForeground(Color.ORANGE);
        } else {
            progressBar.setForeground(Color.GREEN);
        }
    }

    private void handleAnswer(ActionEvent e) {
        if (countdownTimer != null) countdownTimer.stop();

        String input = answerField.getText().trim();
        if (!input.matches("\\d+")) {
            resultLabel.setText("Please enter a number.");
            countdownTimer.start();
            return;
        }

        int ans = Integer.parseInt(input);
        Question q = questions.get(current);
        if (ans == q.answer) {
            correct++;
            resultLabel.setText("Correct!");
        } else {
            incorrect++;
            resultLabel.setText("Wrong. Correct was: " + q.answer);
        }

        current++;
        answerField.setText("");
        showQuestion();
    }

    public void saveResult(String durationFormatted, String fileName) {
        try {
            File file = new File(fileName);
            List<JsonNode> entries = new ArrayList<>();

            if (file.exists() && file.length() > 0) {
                JsonNode[] existing = mapper.readValue(file, JsonNode[].class);
                entries.addAll(Arrays.asList(existing));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("date_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("correct", correct);
            result.put("wrong", incorrect);
            result.put("duration_ms", durationFormatted);

            entries.add(mapper.valueToTree(result));
            ObjectWriter objectWriter = mapper.writerWithDefaultPrettyPrinter();
            objectWriter.writeValue(file, entries);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDurationFormatted(long durationMillis) {
        Duration duration = Duration.ofMillis(durationMillis);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void restartQuiz() {
        current = 0;
        correct = 0;
        incorrect = 0;
        Collections.shuffle(questions);
        quizStartTime = System.currentTimeMillis();
        answerLabel.setVisible(true);
        answerField.setText("");
        answerField.setEnabled(true);
        answerField.setVisible(true);
        replayButton.setVisible(false);
        resultLabel.setText("Type the number of your answer and press Enter.");
        frame.getRootPane().setDefaultButton(null);
        showQuestion();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                launch();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void launch() throws Exception {
        new QuizApp(new ObjectMapper());
    }
}