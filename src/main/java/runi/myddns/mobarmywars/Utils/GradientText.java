package runi.myddns.mobarmywars.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class GradientText {

    private GradientText() {
    }

    public static Component gradient(String text,
                                     int startR, int startG, int startB,
                                     int endR, int endG, int endB) {

        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int len = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            float t = i / (float) len;

            int r = (int) (startR + (endR - startR) * t);
            int g = (int) (startG + (endG - startG) * t);
            int b = (int) (startB + (endB - startB) * t);

            result = result.append(
                    Component.text(String.valueOf(text.charAt(i)))
                            .color(TextColor.color(r, g, b))
            );
        }

        return result;
    }

    public static Component scrollingGradient(String text, float tick,
                                              int startR, int startG, int startB,
                                              int endR, int endG, int endB) {

        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int length = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            float offset = i / (float) length;
            float wave = offset * 1.4f + tick * 0.05f;
            float t = (float) (Math.sin(wave * Math.PI * 2) * 0.5f + 0.5f);

            int r = (int) (startR + (endR - startR) * t);
            int g = (int) (startG + (endG - startG) * t);
            int b = (int) (startB + (endB - startB) * t);

            result = result.append(
                    Component.text(String.valueOf(text.charAt(i)))
                            .color(TextColor.color(r, g, b))
            );
        }

        return result;
    }

    public static Component fadeToGray(String text, float fade) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        fade = Math.max(0.0f, Math.min(1.0f, fade));

        int startR = 255, startG = 215, startB = 64;
        int endR = 85, endG = 85, endB = 85;

        int r = (int) (startR + (endR - startR) * fade);
        int g = (int) (startG + (endG - startG) * fade);
        int b = (int) (startB + (endB - startB) * fade);

        return Component.text(text).color(TextColor.color(r, g, b));
    }
}
