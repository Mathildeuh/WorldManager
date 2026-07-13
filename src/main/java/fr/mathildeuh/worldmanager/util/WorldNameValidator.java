package fr.mathildeuh.worldmanager.util;

import java.util.regex.Pattern;

/** Guards against path-traversal and unsafe filesystem characters in user-supplied world names. */
public final class WorldNameValidator {

    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private WorldNameValidator() { }

    public static boolean isValid(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.equals(".") || name.equals("..")) {
            return false;
        }
        return SAFE_NAME.matcher(name).matches();
    }
}
