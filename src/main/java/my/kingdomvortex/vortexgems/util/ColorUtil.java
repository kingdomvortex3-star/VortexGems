
package my.kingdomvortex.vortexgems.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {
    }

    public static Component text(String text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception exception) {
            return Component.text(text);
        }
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> result = new ArrayList<>();

        if (lines == null) {
            return result;
        }

        for (String line : lines) {
            result.add(text(line));
        }

        return result;
    }

    public static String plain(String text) {
        if (text == null) {
            return "";
        }

        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(MINI_MESSAGE.deserialize(text));
        } catch (Exception exception) {
            return text;
        }
    }

    public static String format(long amount) {
        return String.format("%,d", amount);
    }

    public static String formatGems(long amount) {
        return format(amount) + " Gems";
    }

    public static String replace(String text, String placeholder, String value) {
        if (text == null) {
            return "";
        }

        return text.replace(
                "{" + placeholder + "}",
                value == null ? "" : value
        );
    }
}
