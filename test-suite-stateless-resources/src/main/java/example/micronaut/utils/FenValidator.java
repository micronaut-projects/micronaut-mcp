package example.micronaut.utils;

import java.util.regex.Pattern;

public class FenValidator {
    private static final String FEN_REGEX =
        "^([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}" + // Piece placement
            "\\s[wb]" +                                         // Active color
            "\\s[KQkq-]{1,4}" +                                 // Castling rights
            "\\s([a-h][36]|-)" +                                // En passant
            "\\s\\d+" +                                         // Halfmove clock
            "\\s\\d+$";                                         // Fullmove number

    private static final Pattern FEN_PATTERN = Pattern.compile(FEN_REGEX);

    public static boolean isValidFen(String fen) {
        return FEN_PATTERN.matcher(fen).matches();
    }
}
