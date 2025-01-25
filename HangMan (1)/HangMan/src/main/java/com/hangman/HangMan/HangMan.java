package com.hangman.HangMan; // Keep the package declaration

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
public class HangMan {  // Changed from HangmanApplication to HangMan
    public static void main(String[] args) {
        SpringApplication.run(HangMan.class, args);  // Adjusted to run the HangMan class
    }
}

@RestController
@RequestMapping("/api/game")
class HangmanController {

    private final Map<String, GameState> games = new HashMap<>();
    private final List<String> words = Arrays.asList("SPRING", "JAVA", "CODING", "PROGRAMMING", "COMPUTER");
    private final Random random = new Random();

    @PostMapping("/new")
    public ResponseEntity<GameState> newGame() {
        String gameId = UUID.randomUUID().toString();
        String word = words.get(random.nextInt(words.size()));
        GameState gameState = new GameState(gameId, word);
        games.put(gameId, gameState);
        return ResponseEntity.ok(gameState);
    }

    @PostMapping("/guess/{gameId}")
    public ResponseEntity<GameState> makeGuess(@PathVariable String gameId, @RequestBody GuessRequest guess) {
        GameState gameState = games.get(gameId);
        if (gameState == null) {
            return ResponseEntity.badRequest().body(null);
        }

        if (gameState.getGameStatus() != GameStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(gameState);
        }

        char letter = Character.toUpperCase(guess.getLetter());

        if (!gameState.getGuessedLetters().contains(letter)) {
            gameState.getGuessedLetters().add(letter);
            if (!gameState.getWord().contains(String.valueOf(letter))) {
                gameState.setRemainingAttempts(gameState.getRemainingAttempts() - 1);
            }
        }

        updateGameStatus(gameState);
        return ResponseEntity.ok(gameState);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<String> getGameState(@PathVariable String gameId) {
        GameState gameState = games.get(gameId);
        if (gameState == null) {
            return ResponseEntity.badRequest().body("Game not found");
        }

        StringBuilder response = new StringBuilder();
        response.append("\n").append(renderHangman(gameState.getRemainingAttempts()));
        response.append("\nWord: ").append(gameState.getMaskedWord());
        response.append("\nRemaining Attempts: ").append(gameState.getRemainingAttempts());
        response.append("\nGame Status: ").append(gameState.getGameStatus());

        if (gameState.getGameStatus() == GameStatus.LOST) {
            response.append("\nThe word was: ").append(gameState.getWord());
        }

        return ResponseEntity.ok(response.toString());
    }

    private void updateGameStatus(GameState gameState) {
        String word = gameState.getWord();
        Set<Character> guessedLetters = gameState.getGuessedLetters();

        boolean won = word.chars()
                .mapToObj(ch -> (char) ch)
                .allMatch(guessedLetters::contains);

        if (won) {
            gameState.setGameStatus(GameStatus.WON);
        } else if (gameState.getRemainingAttempts() == 0) {
            gameState.setGameStatus(GameStatus.LOST);
        }
    }

    private String renderHangman(int attemptsLeft) {
        String[] stages = {
                "  +---+\n      |   |\n          |\n          |\n          |\n          |\n    =========",
                "  +---+\n  O   |   |\n          |\n          |\n          |\n          |\n    =========",
                "  +---+\n  O   |   |\n  |   |\n          |\n          |\n          |\n    =========",
                "  +---+\n  O   |   |\n /|   |\n          |\n          |\n          |\n    =========",
                "  +---+\n  O   |   |\n /|\\  |\n          |\n          |\n          |\n    =========",  // Escape the backslash here
                "  +---+\n  O   |   |\n /|\\  |\n /    |\n          |\n          |\n    =========",  // Escape the backslash here
                "  +---+\n  O   |   |\n /|\\  |\n / \\  |\n          |\n          |\n    ========="  // Escape the backslash here
        };

        return stages[6 - attemptsLeft];
    }
}

class GameState {
    private final String gameId;
    private final String word;
    private final Set<Character> guessedLetters;
    private int remainingAttempts;
    private GameStatus gameStatus;

    public GameState(String gameId, String word) {
        this.gameId = gameId;
        this.word = word;
        this.guessedLetters = new HashSet<>();
        this.remainingAttempts = 6;
        this.gameStatus = GameStatus.IN_PROGRESS;
    }

    public String getGameId() {
        return gameId;
    }

    public String getWord() {
        return word;
    }

    public Set<Character> getGuessedLetters() {
        return guessedLetters;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(int remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public String getMaskedWord() {
        StringBuilder masked = new StringBuilder();
        for (char c : word.toCharArray()) {
            masked.append(guessedLetters.contains(c) ? c : "_");
        }
        return masked.toString();
    }
}

enum GameStatus {
    IN_PROGRESS,
    WON,
    LOST
}

class GuessRequest {
    private char letter;

    public char getLetter() {
        return letter;
    }

    public void setLetter(char letter) {
        this.letter = letter;
    }
}
