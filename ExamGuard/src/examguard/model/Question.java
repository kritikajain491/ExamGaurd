package examguard.model;

/** A multiple-choice question with four options. */
public class Question {
    private final String id;
    private String subject, text, explanation, difficulty;
    private String[] options;
    private int correct;

    public Question(String id, String subject, String text, String[] options, int correct, String explanation, String difficulty) {
        this.id = id; this.subject = subject; this.text = text; this.options = options;
        this.correct = correct; this.explanation = explanation; this.difficulty = difficulty;
    }

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getText() { return text; }
    public String[] getOptions() { return options; }
    public int getCorrect() { return correct; }
    public String getExplanation() { return explanation; }
    public String getDifficulty() { return difficulty; }
    public String correctText() { return (char) ('A' + correct) + ". " + options[correct]; }

    public String[] toRecord() {
        return new String[]{id, subject, text, options[0], options[1], options[2], options[3],
                String.valueOf(correct), explanation, difficulty};
    }

    public static Question fromRecord(String[] f) {
        return new Question(f[0], f[1], f[2], new String[]{f[3], f[4], f[5], f[6]}, Integer.parseInt(f[7]), f[8], f[9]);
    }
}
