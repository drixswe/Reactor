package ink.reactor.world.block.state;

public final class Brushable {
    public static int of(final char id, final long dusted) {
        final String sentence = String.valueOf(dusted);
        return switch(sentence) {
            case "1" -> id+1;
            case "2" -> id+2;
            case "3" -> id+3;
            default -> id;
        };
    }
}